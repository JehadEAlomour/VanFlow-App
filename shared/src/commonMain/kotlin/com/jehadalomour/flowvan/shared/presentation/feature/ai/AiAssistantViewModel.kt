package com.jehadalomour.flowvan.shared.presentation.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jehadalomour.flowvan.shared.data.local.dao.AiMessageDao
import com.jehadalomour.flowvan.shared.data.local.entity.AiMessageEntity
import com.jehadalomour.flowvan.shared.data.remote.ClaudeApiClient
import com.jehadalomour.flowvan.shared.data.repository.CustomerRepository
import com.jehadalomour.flowvan.shared.data.repository.InvoiceRepository
import com.jehadalomour.flowvan.shared.data.repository.ProductRepository
import com.jehadalomour.flowvan.shared.data.settings.AiSettings
import com.jehadalomour.flowvan.shared.domain.model.AiMessage
import com.jehadalomour.flowvan.shared.domain.usecase.GetDailyKpiUseCase
import com.jehadalomour.flowvan.shared.presentation.format.formatJod
import com.jehadalomour.flowvan.shared.presentation.i18n.AppLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val WELCOME = "أهلاً! أنا مساعدك الذكي لتطبيق FlowVan. يمكنني مساعدتك في: ملخص اليوم، تحليل أداء العملاء، توصيات المنتجات، استفسارات المخزون..."
private const val FALLBACK = "🤔 سؤال رائع! في الوقت الحالي أعمل بالبيانات التجريبية. جرب: ملخص، مبيعات، مخزون، مسار، عميل"
private const val OFFLINE_NOTE = "\n\n_(لتفعيل الذكاء الاصطناعي الحقيقي، أضف مفتاح API من الإعدادات)_"

class AiAssistantViewModel(
    private val customerId: String?,
    private val aiMessageDao: AiMessageDao,
    private val getKpi: GetDailyKpiUseCase,
    private val invoices: InvoiceRepository,
    private val products: ProductRepository,
    private val customers: CustomerRepository,
    private val aiSettings: AiSettings,
    private val claudeApiClient: ClaudeApiClient,
) : ViewModel() {

    private val conversationId = if (customerId != null) "customer-$customerId" else "home"

    private val _state = MutableStateFlow(AiAssistantState(apiKeySet = aiSettings.isConfigured))
    val state: StateFlow<AiAssistantState> = _state.asStateFlow()

    init {
        loadHistory()
    }

    fun onEvent(event: AiAssistantEvent) {
        when (event) {
            is AiAssistantEvent.InputChanged -> _state.update { it.copy(inputText = event.text) }
            AiAssistantEvent.Send -> sendMessage(_state.value.inputText)
            is AiAssistantEvent.ChipTapped -> sendMessage(event.text)
            AiAssistantEvent.OpenApiKeyDialog -> _state.update { it.copy(showApiKeyDialog = true, apiKeyInput = aiSettings.apiKey) }
            AiAssistantEvent.DismissApiKeyDialog -> _state.update { it.copy(showApiKeyDialog = false, apiKeyInput = "") }
            is AiAssistantEvent.ApiKeyInputChanged -> _state.update { it.copy(apiKeyInput = event.text) }
            AiAssistantEvent.SaveApiKey -> saveApiKey()
        }
    }

    private fun saveApiKey() {
        val key = _state.value.apiKeyInput.trim()
        aiSettings.apiKey = key
        _state.update { it.copy(apiKeySet = key.isNotBlank(), showApiKeyDialog = false, apiKeyInput = "") }
    }

    @OptIn(ExperimentalTime::class)
    private fun loadHistory() {
        viewModelScope.launch {
            val history = aiMessageDao.listConversation(conversationId).map { it.toDomain() }
            if (history.isEmpty()) {
                val welcome = buildMessage("assistant", WELCOME)
                aiMessageDao.upsert(welcome.toEntity())
                _state.update { it.copy(messages = listOf(welcome.toDomain())) }
            } else {
                _state.update { it.copy(messages = history) }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun sendMessage(text: String) {
        if (text.isBlank() || _state.value.isThinking || _state.value.isStreaming) return
        viewModelScope.launch {
            val userMsg = buildMessage("user", text)
            aiMessageDao.upsert(userMsg.toEntity())
            _state.update { s -> s.copy(messages = s.messages + userMsg.toDomain(), inputText = "", isThinking = true) }

            if (aiSettings.isConfigured) {
                streamClaudeResponse(text.trim())
            } else {
                delay(1200)
                val response = generateDemoResponse(text.trim()) + OFFLINE_NOTE
                val aiMsg = buildMessage("assistant", response)
                aiMessageDao.upsert(aiMsg.toEntity())
                _state.update { s -> s.copy(messages = s.messages + aiMsg.toDomain(), isThinking = false) }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun streamClaudeResponse(query: String) {
        val systemPrompt = buildSystemPrompt()
        _state.update { it.copy(isThinking = false, isStreaming = true, streamingContent = "") }

        try {
            claudeApiClient.streamResponse(aiSettings.apiKey, systemPrompt, query).collect { token ->
                _state.update { it.copy(streamingContent = it.streamingContent + token) }
            }
            val finalContent = _state.value.streamingContent
            if (finalContent.isNotBlank()) {
                val aiMsg = buildMessage("assistant", finalContent)
                aiMessageDao.upsert(aiMsg.toEntity())
                _state.update { s ->
                    s.copy(messages = s.messages + aiMsg.toDomain(), isStreaming = false, streamingContent = "")
                }
            } else {
                _state.update { it.copy(isStreaming = false, streamingContent = "") }
            }
        } catch (e: Exception) {
            val errorMsg = buildMessage("assistant", "⚠️ خطأ في الاتصال: ${e.message ?: "حاول مجدداً"}")
            aiMessageDao.upsert(errorMsg.toEntity())
            _state.update { s ->
                s.copy(messages = s.messages + errorMsg.toDomain(), isStreaming = false, streamingContent = "")
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun buildSystemPrompt(): String {
        val tz = TimeZone.currentSystemDefault()
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val today = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz).date
        val startOfTodayMs = today.atStartOfDayIn(tz).toEpochMilliseconds()
        val kpi = getKpi()

        return buildString {
            appendLine("أنت مساعد ذكي لتطبيق FlowVan لمندوبي المبيعات في شركة المدينة للتجارة في عمّان، الأردن.")
            appendLine("اللغة: العربية فقط. أجب بإيجاز وعملية.")
            appendLine()
            appendLine("بيانات اليوم:")
            appendLine("• المبيعات: ${kpi.salesTotal.formatJod(AppLanguage.AR)}")
            appendLine("• المرتجعات: ${kpi.returnsTotal.formatJod(AppLanguage.AR)}")
            appendLine("• التحصيلات: ${kpi.collectionsTotal.formatJod(AppLanguage.AR)}")
            appendLine("• الزيارات: ${kpi.customersVisited} / ${kpi.customersPlanned}")
            if (customerId != null) {
                val customer = customers.findById(customerId)
                if (customer != null) {
                    appendLine()
                    appendLine("العميل الحالي: ${customer.nameAr}")
                    appendLine("• الرصيد: ${customer.balance.formatJod(AppLanguage.AR)}")
                    appendLine("• المتأخرات: ${customer.overdueAmount.formatJod(AppLanguage.AR)}")
                    appendLine("• التصنيف: ${customer.tier}")
                }
            }
            val lowStock = products.observeLowStock().first().take(5)
            if (lowStock.isNotEmpty()) {
                appendLine()
                appendLine("منتجات منخفضة المخزون: ${lowStock.joinToString(", ") { it.nameAr }}")
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun generateDemoResponse(query: String): String {
        val tz = TimeZone.currentSystemDefault()
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val today = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz).date
        val startOfTodayMs = today.atStartOfDayIn(tz).toEpochMilliseconds()

        return when {
            query.containsAny("ملخص", "summary") -> buildSummaryResponse()
            query.containsAny("مبيعات", "sales") -> buildSalesResponse(startOfTodayMs)
            query.containsAny("مخزون", "stock") -> buildStockResponse()
            query.containsAny("عميل", "customer") -> buildCustomerResponse()
            query.containsAny("مسار", "route") -> buildRouteResponse()
            else -> FALLBACK
        }
    }

    private suspend fun buildSummaryResponse(): String {
        val kpi = getKpi()
        return buildString {
            appendLine("📊 **ملخص اليوم**")
            appendLine()
            appendLine("💰 المبيعات: ${kpi.salesTotal.formatJod(AppLanguage.AR)}")
            appendLine("↩️ المرتجعات: ${kpi.returnsTotal.formatJod(AppLanguage.AR)}")
            appendLine("💵 صافي المبيعات: ${(kpi.salesTotal - kpi.returnsTotal).formatJod(AppLanguage.AR)}")
            appendLine("🏦 التحصيلات: ${kpi.collectionsTotal.formatJod(AppLanguage.AR)}")
            append("👥 العملاء: ${kpi.customersVisited} / ${kpi.customersPlanned} تمت زيارتهم")
        }
    }

    private suspend fun buildSalesResponse(startOfTodayMs: Long): String {
        val todayInvoices = invoices.listSince(startOfTodayMs).filter { it.type == "SALE" }
        if (todayInvoices.isEmpty()) return "📋 لا توجد فواتير بيع اليوم حتى الآن."
        return buildString {
            appendLine("🧾 **فواتير البيع اليوم (${todayInvoices.size})**")
            appendLine()
            todayInvoices.forEach { inv ->
                appendLine("• ${inv.number} — ${inv.total.formatJod(AppLanguage.AR)}")
            }
            append("\n💰 الإجمالي: ${todayInvoices.sumOf { it.total }.formatJod(AppLanguage.AR)}")
        }
    }

    private suspend fun buildStockResponse(): String {
        val lowStock = products.observeLowStock().first()
        if (lowStock.isEmpty()) return "✅ جميع المنتجات متوفرة بكميات كافية."
        return buildString {
            appendLine("⚠️ **المنتجات ذات المخزون المنخفض (${lowStock.size})**")
            appendLine()
            lowStock.take(10).forEach { p ->
                val status = if (p.vanStock == 0) "نفد ❌" else "منخفض ⚠️"
                appendLine("• ${p.nameAr} (${p.sku}) — الكمية: ${p.vanStock} / الحد: ${p.minStock} — $status")
            }
        }
    }

    private suspend fun buildCustomerResponse(): String {
        if (customerId != null) {
            val customer = customers.findById(customerId) ?: return FALLBACK
            return buildString {
                appendLine("👤 **${customer.nameAr}**")
                appendLine()
                appendLine("• الرصيد: ${customer.balance.formatJod(AppLanguage.AR)}")
                appendLine("• المتأخرات: ${customer.overdueAmount.formatJod(AppLanguage.AR)}")
                appendLine("• التصنيف: ${customer.tier}")
                appendLine("• مستوى خطر التوقف: ${(customer.churnRisk * 100).toInt()}%")
                append("• حد الائتمان: ${customer.creditLimit.formatJod(AppLanguage.AR)}")
            }
        }
        val topCustomers = customers.observeAll().first().take(5)
        return buildString {
            appendLine("👥 **أعلى العملاء**")
            appendLine()
            topCustomers.forEach { c ->
                appendLine("• ${c.nameAr} — رصيد: ${c.balance.formatJod(AppLanguage.AR)} — ${c.tier}")
            }
        }
    }

    private suspend fun buildRouteResponse(): String {
        val routeCustomers = customers.observeRoute().first()
        val remaining = routeCustomers.filter { it.visitOrder > 0 }
        if (remaining.isEmpty()) return "✅ تم زيارة جميع عملاء المسار اليوم!"
        return buildString {
            appendLine("🗺️ **عملاء المسار (${remaining.size})**")
            appendLine()
            remaining.forEach { c ->
                val warning = if (c.overdueAmount > 0) " ⚠️ ذمم متأخرة" else ""
                appendLine("• ${c.nameAr} — ${c.area}$warning")
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun buildMessage(role: String, content: String): MsgBundle {
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val id = "ai-$nowMs-${(1000..9999).random()}"
        return MsgBundle(id, conversationId, role, content, nowMs)
    }

    private data class MsgBundle(
        val id: String,
        val conversationId: String,
        val role: String,
        val content: String,
        val createdAt: Long,
    ) {
        fun toDomain() = AiMessage(id, conversationId, role, content, createdAt)
        fun toEntity() = AiMessageEntity(id, conversationId, role, content, createdAt)
    }

    private fun AiMessageEntity.toDomain() = AiMessage(id, conversationId, role, content, createdAt)

    private fun String.containsAny(vararg keywords: String) =
        keywords.any { this.contains(it, ignoreCase = true) }
}
