package com.jehadalomour.flowvan.feature.print

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.core.data.repository.CompanyInfoRepository
import com.jehadalomour.flowvan.core.data.repository.ProductRepository
import com.jehadalomour.flowvan.core.data.repository.UserRepository
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.domain.printer.PrinterLanguage
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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The van's own stock, on paper.
 *
 * READ ENTIRELY FROM THE DEVICE. `ProductRepository.observeAll()` is the same
 * local stream the van-stock screen itself renders, so the sheet says what the
 * rep is looking at and prints with no signal at all — which is the point. A
 * salesman counting his van is standing in it, often in a yard, and a stock
 * sheet that needs a network is a stock sheet he cannot have when he needs it.
 *
 * Nothing new is fetched and no endpoint was added for this.
 */
@OptIn(ExperimentalTime::class)
class VanStockPrintViewModel(
    private val products: ProductRepository,
    private val users: UserRepository,
    private val companyInfo: CompanyInfoRepository,
    private val printer: ReceiptPrinter,
    private val session: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(
        VanStockPrintState(
            printedAt = Clock.System.now().toEpochMilliseconds(),
            connectType = printer.lastTarget?.type ?: PrinterType.BLUETOOTH,
            connectLanguage = printer.language,
            connectAddress = printer.lastTarget?.address.orEmpty(),
        ),
    )
    val state: StateFlow<VanStockPrintState> = _state.asStateFlow()

    init {
        products.observeAll()
            .onEach { list ->
                // Which rows, in what order, and what each is worth: see
                // vanStockRows, which is pure so it can be tested.
                _state.update { it.copy(rows = vanStockRows(list), isLoading = false) }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            // Whoever is holding the phone is who the office will ask about this
            // count, so the sheet is signed with their name.
            val salesman = session.currentUserId?.let { users.findById(it) }
            _state.update { it.copy(salesmanNameAr = salesman?.nameAr.orEmpty()) }
        }

        viewModelScope.launch {
            val info = companyInfo.getForPrint()
            _state.update {
                it.copy(
                    companyNameAr = info.nameAr,
                    companyNameEn = info.nameEn,
                    companyTaxNumber = info.taxNumber,
                    companyLogo = info.logo,
                )
            }
        }

        printer.state
            .onEach { s -> _state.update { it.copy(printerState = s) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: VanStockPrintEvent) {
        when (event) {
            VanStockPrintEvent.RequestConnectThenPrint -> {
                _state.update {
                    it.copy(showConnectDialog = true, pendingPrint = true, printMessageAr = null)
                }
                refreshDevices()
            }

            VanStockPrintEvent.DismissConnectDialog -> _state.update {
                it.copy(showConnectDialog = false, pendingPrint = false)
            }

            is VanStockPrintEvent.PrinterLanguageSelected -> {
                // Device-wide and persisted: written here it routes every print
                // screen to the right SDK from now on, not just this one.
                printer.language = event.language
                _state.update { it.copy(connectLanguage = event.language) }
            }

            is VanStockPrintEvent.ConnectTypeSelected -> {
                _state.update { it.copy(connectType = event.type) }
                refreshDevices()
            }

            is VanStockPrintEvent.ConnectAddressChanged -> _state.update {
                it.copy(connectAddress = event.address)
            }

            is VanStockPrintEvent.DeviceSelected -> _state.update {
                it.copy(connectType = event.target.type, connectAddress = event.target.address)
            }

            VanStockPrintEvent.RefreshDevices -> refreshDevices()
            VanStockPrintEvent.Connect -> connect()
            VanStockPrintEvent.Disconnect -> printer.disconnect()
            is VanStockPrintEvent.Print -> print(event.png)
            VanStockPrintEvent.DismissMessage -> _state.update { it.copy(printMessageAr = null) }
        }
    }

    private fun refreshDevices() {
        val devices = when (_state.value.connectType) {
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

    /**
     * The sheet goes to the printer as an IMAGE, like every other receipt here:
     * printer firmware cannot shape Arabic, so text nodes come out as
     * disconnected letters in the wrong order.
     */
    private fun print(png: ByteArray) {
        if (_state.value.printerState !is PrinterState.Connected) return
        _state.update { it.copy(isPrinting = true, pendingPrint = false, printMessageAr = null) }
        viewModelScope.launch {
            val result = printer.printImage(png)
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
