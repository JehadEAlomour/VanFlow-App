package com.jehadalomour.flowvan.feature.print

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
import com.jehadalomour.flowvan.core.data.repository.AppSettingsRepository
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.UserRepository
import com.jehadalomour.flowvan.core.model.InvoiceLine
import com.jehadalomour.flowvan.core.domain.printer.PaperWidth
import com.jehadalomour.flowvan.core.domain.printer.PrintResult
import com.jehadalomour.flowvan.core.domain.printer.PrinterState
import com.jehadalomour.flowvan.core.domain.printer.PrinterTarget
import com.jehadalomour.flowvan.core.domain.printer.PrinterType
import com.jehadalomour.flowvan.core.domain.printer.ReceiptPrinter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class VoucherPrintViewModel(
    private val invoiceId: String,
    private val invoiceDao: InvoiceDao,
    private val customers: CustomerRepository,
    private val users: UserRepository,
    private val appSettings: AppSettingsRepository,
    private val json: Json,
    private val printer: ReceiptPrinter,
) : ViewModel() {

    private val _state = MutableStateFlow(
        VoucherPrintState(
            connectType = printer.lastTarget?.type ?: PrinterType.BLUETOOTH,
            connectAddress = printer.lastTarget?.address.orEmpty(),
        ),
    )
    val state: StateFlow<VoucherPrintState> = _state.asStateFlow()

    init {
        invoiceDao.observeById(invoiceId)
            .onEach { entity ->
                if (entity == null) {
                    _state.update { it.copy(isLoading = false) }
                    return@onEach
                }
                val lines = runCatching {
                    json.decodeFromString<List<InvoiceLine>>(entity.linesJson)
                }.getOrDefault(emptyList())

                val customer = customers.findById(entity.customerId)
                val salesman = users.findById(entity.salesmanId)
                val settings = appSettings.get()

                _state.update {
                    it.copy(
                        isLoading = false,
                        invoiceId = entity.id,
                        number = entity.number,
                        type = entity.type,
                        paymentMethod = entity.paymentMethod,
                        createdAt = entity.createdAt,
                        customerNameAr = customer?.nameAr.orEmpty(),
                        customerCode = customer?.code.orEmpty(),
                        customerTaxNumber = customer?.taxNumber,
                        salesmanNameAr = salesman?.nameAr.orEmpty(),
                        lines = lines,
                        subtotal = entity.subtotal,
                        discountAmount = entity.discountAmount,
                        taxAmount = entity.taxAmount,
                        total = entity.total,
                        notes = entity.notes,
                        branch = settings.branch,
                    )
                }
            }
            .launchIn(viewModelScope)

        // Mirror the live connection state into our UI state.
        printer.state
            .onEach { s -> _state.update { it.copy(printerState = s) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: VoucherPrintEvent) {
        when (event) {
            VoucherPrintEvent.RequestConnectThenPrint -> {
                _state.update {
                    it.copy(showConnectDialog = true, pendingPrint = true, printMessageAr = null)
                }
                refreshDevices()
            }

            VoucherPrintEvent.DismissConnectDialog -> _state.update {
                it.copy(showConnectDialog = false, pendingPrint = false)
            }

            is VoucherPrintEvent.ConnectTypeSelected -> {
                _state.update { it.copy(connectType = event.type) }
                refreshDevices()
            }

            is VoucherPrintEvent.ConnectAddressChanged -> _state.update {
                it.copy(connectAddress = event.address)
            }

            is VoucherPrintEvent.DeviceSelected -> _state.update {
                it.copy(connectType = event.target.type, connectAddress = event.target.address)
            }

            VoucherPrintEvent.RefreshDevices -> refreshDevices()

            VoucherPrintEvent.Connect -> connect()

            VoucherPrintEvent.Disconnect -> printer.disconnect()

            is VoucherPrintEvent.Print -> print(event.receiptPng)

            VoucherPrintEvent.DismissMessage -> _state.update { it.copy(printMessageAr = null) }
        }
    }

    private fun refreshDevices() {
        val type = _state.value.connectType
        val devices = when (type) {
            PrinterType.BLUETOOTH -> printer.discoverBluetooth()
            PrinterType.USB -> printer.discoverUsb()
            PrinterType.SERIAL -> printer.discoverSerialPorts()
            PrinterType.NETWORK -> emptyList()
        }
        _state.update { it.copy(discoveredDevices = devices) }
    }

    private fun connect() {
        val s = _state.value
        if (s.connectAddress.isBlank()) return
        val target = PrinterTarget(
            type = s.connectType,
            address = s.connectAddress,
            name = s.connectAddress,
            baudRate = printer.lastTarget?.baudRate ?: 115200,
        )
        viewModelScope.launch {
            when (val result = printer.connect(target)) {
                is PrintResult.Success -> _state.update { it.copy(showConnectDialog = false) }
                is PrintResult.Failure -> _state.update { it.copy(printMessageAr = result.message) }
            }
        }
    }

    private fun print(png: ByteArray) {
        if (_state.value.printerState !is PrinterState.Connected) return
        _state.update { it.copy(isPrinting = true, pendingPrint = false, printMessageAr = null) }
        viewModelScope.launch {
            val result = printer.printImage(png, PaperWidth.MM80)
            _state.update {
                it.copy(
                    isPrinting = false,
                    printMessageAr = when (result) {
                        is PrintResult.Success -> SUCCESS_MESSAGE
                        is PrintResult.Failure -> result.message
                    },
                )
            }
        }
    }

    private companion object {
        const val SUCCESS_MESSAGE = "تمت الطباعة بنجاح"
    }
}
