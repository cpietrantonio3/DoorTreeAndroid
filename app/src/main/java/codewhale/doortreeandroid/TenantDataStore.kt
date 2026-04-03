package codewhale.doortreeandroid

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.NumberFormat
import java.text.ParsePosition
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID

data class MaintenancePhotoUpload(
    val bytes: ByteArray,
    val contentType: String = "image/jpeg",
    val fileExtension: String = "jpg"
)

data class TenantRecord(
    val uid: String,
    val bathrooms: Double?,
    val bedrooms: Double?,
    val city: String,
    val email: String,
    val firstName: String,
    val landlordUID: String,
    val lastName: String,
    val lateFeeAmount: Double?,
    val leaseEnd: String,
    val leaseEnded: Boolean,
    val leaseStart: String,
    val postalCode: String,
    val propertyId: String,
    val propertyManager: String,
    val propertyName: String,
    val province: String,
    val rentAmount: Double?,
    val securityDeposit: Double?,
    val squareFootage: Double?,
    val streetAddress: String,
    val unitNumber: String,
    val userType: String
) {
    val tenantProfile: TenantProfile
        get() = TenantProfile(
            name = displayName,
            email = email,
            phone = "",
            initials = initials(displayName)
        )

    val propertyInfo: PropertyInfo
        get() = PropertyInfo(
            name = propertyDisplayName,
            unit = unitDisplayName,
            address = streetAddressLine,
            city = cityLine
        )

    val leaseDetails: LeaseDetails
        get() = LeaseDetails(
            startDate = formattedDate(leaseStart),
            endDate = formattedDate(leaseEnd),
            monthlyRent = formattedCurrency(rentAmount),
            unitLabel = propertyInfo.subtitle,
            renewalNotice = renewalSummary
        )

    val propertyManagerDisplayName: String
        get() = propertyManager.trim().ifBlank { "Property Manager" }

    val propertyManagerInitials: String
        get() = initials(propertyManagerDisplayName)

    private val displayName: String
        get() = listOf(firstName.trim(), lastName.trim())
            .filter(String::isNotBlank)
            .joinToString(" ")
            .ifBlank { email.ifBlank { "Tenant" } }

    private val propertyDisplayName: String
        get() = propertyName.trim().ifBlank { "Property" }

    private val unitDisplayName: String
        get() = unitNumber.trim().let { if (it.isBlank()) "Unit -" else "Unit $it" }

    private val streetAddressLine: String
        get() = streetAddress.trim().ifBlank { unitDisplayName }

    private val cityLine: String
        get() {
            val location = listOf(city.trim(), province.trim()).filter(String::isNotBlank).joinToString(", ")
            val postal = postalCode.trim()
            return when {
                location.isBlank() -> postal
                postal.isBlank() -> location
                else -> "$location $postal"
            }
        }

    private val renewalSummary: String
        get() {
            val formattedEnd = formattedDate(leaseEnd)
            return when {
                leaseEnded && formattedEnd == "-" -> "Lease has ended."
                leaseEnded -> "Lease ended on $formattedEnd."
                formattedEnd == "-" -> "Lease is active."
                else -> "Lease active until $formattedEnd."
            }
        }

    companion object {
        private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        private val outputFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())

        fun fromSnapshot(uid: String, snapshot: JsonObject): TenantRecord {
            return TenantRecord(
                uid = uid,
                bathrooms = snapshot["bathrooms"].doubleValue(),
                bedrooms = snapshot["bedrooms"].doubleValue(),
                city = snapshot["city"].stringValue(),
                email = snapshot["email"].stringValue(),
                firstName = snapshot["firstName"].stringValue(),
                landlordUID = snapshot["landlordUID"].stringValue(),
                lastName = snapshot["lastName"].stringValue(),
                lateFeeAmount = snapshot["lateFeeAmount"].doubleValue(),
                leaseEnd = snapshot["leaseEnd"].stringValue(),
                leaseEnded = snapshot["leaseEnded"]?.jsonPrimitive?.booleanOrNull ?: false,
                leaseStart = snapshot["leaseStart"].stringValue(),
                postalCode = snapshot["postalCode"].stringValue(),
                propertyId = snapshot["propertyId"].stringValue(),
                propertyManager = snapshot["propertyManager"].stringValue(),
                propertyName = snapshot["propertyName"].stringValue(),
                province = snapshot["province"].stringValue(),
                rentAmount = snapshot["rentAmount"].doubleValue(),
                securityDeposit = snapshot["securityDeposit"].doubleValue(),
                squareFootage = snapshot["squarefootage"].doubleValue(),
                streetAddress = snapshot["streetAddress"].stringValue(),
                unitNumber = snapshot["unitNumber"].stringValue(),
                userType = snapshot["userType"].stringValue()
            )
        }

        private fun formattedDate(raw: String): String {
            return runCatching {
                LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE).format(outputFormatter)
            }.getOrElse {
                raw.ifBlank { "-" }
            }
        }

        private fun formattedCurrency(value: Double?): String {
            return value?.let { currencyFormatter.format(it) } ?: "-"
        }

        private fun initials(value: String): String {
            return value.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercaseChar() }
                .joinToString("")
                .ifBlank { "?" }
        }

        private fun JsonElement?.stringValue(): String {
            return this?.jsonPrimitive?.content?.trim().orEmpty()
        }

        private fun JsonElement?.doubleValue(): Double? {
            return this?.jsonPrimitive?.doubleOrNull
                ?: this?.jsonPrimitive?.content?.toDoubleOrNull()
        }
    }
}

class TenantDataStore(
    private val authSession: AuthSessionStore
) {
    private val restClient = FirebaseRestClient()
    private val storage = FirebaseStorage.getInstance("gs://${FirebaseConfig.storageBucket}")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var activeUid: String? = null

    var tenantRecord by mutableStateOf<TenantRecord?>(null)
        private set
    var notificationPreferences by mutableStateOf(DoorTreeSampleData.notificationPreferences)
        private set
    var pendingInvoices by mutableStateOf<List<PendingInvoiceItem>>(emptyList())
        private set
    var maintenanceRequests by mutableStateOf<List<MaintenanceRequestItem>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set

    val tenantProfile: TenantProfile
        get() = tenantRecord?.tenantProfile ?: TenantProfile("Tenant", "", "", "?")

    val propertyInfo: PropertyInfo
        get() = tenantRecord?.propertyInfo ?: PropertyInfo("Property", "Unit -", "", "")

    val leaseDetails: LeaseDetails
        get() = tenantRecord?.leaseDetails ?: LeaseDetails("-", "-", "-", "-", "Lease information is unavailable.")

    val propertyManagerName: String
        get() = tenantRecord?.propertyManagerDisplayName ?: "Property Manager"

    val propertyManagerInitials: String
        get() = tenantRecord?.propertyManagerInitials ?: "PM"

    val quickActions: List<QuickActionItem>
        get() = DoorTreeSampleData.quickActions

    val paymentMethods: List<PaymentMethodItem>
        get() = DoorTreeSampleData.paymentMethods

    val paymentHistory: List<PaymentItem>
        get() = emptyList()

    val completedPayments: List<PaymentItem>
        get() = paymentHistory.filter { it.status == StatusBadgeStyle.Paid }

    val chatSections: List<ChatSection>
        get() = emptyList()

    val documents: List<DocumentItem>
        get() = emptyList()

    val notificationCenterItems: List<NotificationCenterItem>
        get() {
            val record = tenantRecord ?: return emptyList()
            val monthlyRent = leaseDetails.monthlyRent.takeIf { it != "-" } ?: "Your rent"
            val propertyName = propertyInfo.name.takeIf { it != "Property" } ?: "your property"
            val unitLabel = propertyInfo.unit.takeIf { it != "Unit -" } ?: "your unit"
            val leaseEnd = leaseDetails.endDate.takeIf { it != "-" } ?: "your current lease term"
            val managerName = propertyManagerName.takeIf { it != "Property Manager" } ?: "your property manager"

            return listOf(
                NotificationCenterItem(
                    id = "rent-reminder",
                    title = "Rent reminder",
                    message = "$monthlyRent is due for ${currentMonthLabel()}. You can submit payment anytime from the Payments tab.",
                    timestamp = "Today",
                    category = NotificationCenterCategory.Payment,
                    isUnread = true
                ),
                NotificationCenterItem(
                    id = "lease-status",
                    title = "Lease status",
                    message = "Your lease for $propertyName stays active through $leaseEnd.",
                    timestamp = "This week",
                    category = NotificationCenterCategory.Lease,
                    isUnread = true
                ),
                NotificationCenterItem(
                    id = "manager-contact",
                    title = "Manager contact",
                    message = "$managerName is assigned to $unitLabel. Use Chat if you need help with the apartment.",
                    timestamp = "Anytime",
                    category = NotificationCenterCategory.Message,
                    isUnread = false
                ),
                NotificationCenterItem(
                    id = "profile-sync",
                    title = "Profile synced",
                    message = "Your tenant account for ${record.email} is connected and ready.",
                    timestamp = "Now",
                    category = NotificationCenterCategory.Reminder,
                    isUnread = false
                )
            )
        }

    val unreadNotificationCount: Int
        get() = notificationCenterItems.count { it.isUnread }

    fun handleAuthState(uid: String?) {
        if (uid.isNullOrBlank()) {
            debugMaintenanceRequestLog("handleAuthState received empty uid, resetting store")
            reset()
            return
        }
        if (activeUid == uid && (tenantRecord != null || isLoading)) {
            debugMaintenanceRequestLog("handleAuthState ignoring duplicate uid $uid")
            return
        }
        activeUid = uid
        debugMaintenanceRequestLog("handleAuthState loading uid $uid")
        scope.launch {
            loadTenantRecord(uid)
        }
    }

    fun reload() {
        val uid = activeUid ?: return
        debugMaintenanceRequestLog("reload requested for uid $uid")
        scope.launch {
            loadTenantRecord(uid)
        }
    }

    suspend fun refresh() {
        val uid = activeUid ?: return
        debugMaintenanceRequestLog("refresh requested for uid $uid")
        loadTenantRecord(uid)
    }

    suspend fun submitMaintenanceRequest(
        category: MaintenanceCategory,
        customCategoryName: String,
        description: String,
        priority: MaintenancePriority,
        refundAmount: String? = null,
        isRefundRequest: Boolean = false,
        photos: List<MaintenancePhotoUpload> = emptyList()
    ) {
        val uid = activeUid ?: throw IllegalStateException(L("maintenance.submit_error.sign_in"))
        val record = tenantRecord ?: throw IllegalStateException(L("maintenance.submit_error.tenant_unavailable"))
        val trimmedDescription = description.trim()
        if (trimmedDescription.isBlank()) {
            throw IllegalStateException(L("maintenance.submit_error.describe_issue"))
        }
        val validatedRefundAmount = validatedRefundAmount(refundAmount, isRefundRequest)

        val idToken = authSession.ensureValidIdToken()
            ?: throw IllegalStateException(L("maintenance.submit_error.sign_in"))
        val landlordUid = resolveLandlordUid(record)
        val now = Instant.now()
        val today = LocalDate.now()
        val requestId = UUID.randomUUID().toString()
        val requestDate = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val timestamp = now.toString()
        val requestPath = maintenanceRequestPath(uid = uid, date = today, requestId = requestId)
        val landlordPath = maintenanceRequestPath(uid = landlordUid, date = today, requestId = requestId)
        val photoUrls = uploadMaintenancePhotos(
            photos = photos,
            tenantUid = uid,
            landlordUid = landlordUid,
            requestDate = requestDate,
            requestId = requestId
        )
        val payload = buildJsonObject {
            put("category", JsonPrimitive(if (isRefundRequest) "Refund Request" else resolvedMaintenanceCategoryValue(category, customCategoryName)))
            put("createdAt", JsonPrimitive(timestamp))
            put("date", JsonPrimitive(requestDate))
            put("description", JsonPrimitive(trimmedDescription))
            put("internalNotes", JsonPrimitive(""))
            put("issue", JsonPrimitive(maintenanceIssueSummary(trimmedDescription)))
            put("photos", buildJsonArray {
                photoUrls.forEach { url ->
                    add(JsonPrimitive(url))
                }
            })
            put("preferredDate", JsonPrimitive(requestDate))
            put("priority", JsonPrimitive(priority.defaultTitle))
            put("property", JsonPrimitive(record.propertyName))
            put("status", JsonPrimitive("submitted"))
            put("tenant", JsonPrimitive(record.tenantProfile.name))
            put("unit", JsonPrimitive(record.unitNumber))
            put("updatedAt", JsonPrimitive(timestamp))
            validatedRefundAmount?.let { put("amount", JsonPrimitive(it)) }
        }

        val updated = restClient.patchDatabaseRoot(
            idToken = idToken,
            body = buildJsonObject {
                put(requestPath, payload)
                put(landlordPath, payload)
            }
        )
        if (!updated) {
            throw IllegalStateException("Unable to submit maintenance request.")
        }

        refresh()
    }

    fun updateNotificationSetting(key: NotificationSettingKey, isEnabled: Boolean) {
        val uid = activeUid ?: return
        val previous = notificationPreferences
        notificationPreferences = notificationPreferences.updating(key, isEnabled)

        scope.launch {
            val idToken = authSession.ensureValidIdToken()
            val updated = idToken != null && restClient.updateNotificationSetting(uid, idToken, key.firebaseKey, isEnabled)
            if (!updated) {
                notificationPreferences = previous
            }
        }
    }

    private suspend fun loadTenantRecord(uid: String) {
        isLoading = true
        loadError = null
        debugMaintenanceRequestLog("loadTenantRecord start uid=$uid")

        val idToken = authSession.ensureValidIdToken()
        if (idToken.isNullOrBlank()) {
            tenantRecord = null
            notificationPreferences = DoorTreeSampleData.notificationPreferences
            pendingInvoices = emptyList()
            maintenanceRequests = emptyList()
            loadError = L("auth.error.sign_in_again")
            isLoading = false
            debugMaintenanceRequestLog("loadTenantRecord missing auth token for uid $uid")
            return
        }

        val snapshot = runCatching { restClient.fetchUser(uid, idToken) }.getOrNull()
        val objectValue = snapshot?.jsonObject
        debugMaintenanceRequestLog("loadTenantRecord user snapshot keys=${objectValue?.keys?.sorted()?.joinToString(",").orEmpty()} raw=${snapshot?.toString() ?: "null"}")
        if (objectValue == null || objectValue.isEmpty()) {
            tenantRecord = null
            notificationPreferences = DoorTreeSampleData.notificationPreferences
            pendingInvoices = emptyList()
            maintenanceRequests = emptyList()
            loadError = "We couldn't find tenant data for this account."
            isLoading = false
            debugMaintenanceRequestLog("loadTenantRecord failed to decode tenant record for uid $uid")
            return
        }

        tenantRecord = TenantRecord.fromSnapshot(uid, objectValue)
        pendingInvoices = parsePendingInvoices(objectValue)
        debugMaintenanceRequestLog("loadTenantRecord pendingInvoices count=${pendingInvoices.size}")
        val maintenanceRequestsSnapshot = runCatching {
            restClient.fetchMaintenanceRequests(uid, idToken)
        }.getOrNull()
        debugMaintenanceRequestLog("loadMaintenanceRequests raw=${maintenanceRequestsSnapshot?.toString() ?: "null"}")
        maintenanceRequests = MaintenanceRequestParser.parseRequestsRoot(maintenanceRequestsSnapshot?.jsonObject) { message ->
            debugMaintenanceRequestLog(message)
        }
        debugMaintenanceRequestLog("loadMaintenanceRequests parsed count=${maintenanceRequests.size} ids=${maintenanceRequests.joinToString(",") { it.id }}")
        val notificationSettings = objectValue["notificationSettings"]?.jsonObject
        notificationPreferences = NotificationPreferences(
            paymentReminders = notificationSettings?.get("paymentReminders")?.jsonPrimitive?.booleanOrNull
                ?: DoorTreeSampleData.notificationPreferences.paymentReminders,
            maintenanceUpdates = notificationSettings?.get("maintenanceUpdates")?.jsonPrimitive?.booleanOrNull
                ?: DoorTreeSampleData.notificationPreferences.maintenanceUpdates,
            messages = notificationSettings?.get("messages")?.jsonPrimitive?.booleanOrNull
                ?: DoorTreeSampleData.notificationPreferences.messages,
            faceID = DoorTreeSampleData.notificationPreferences.faceID
        )
        isLoading = false
    }

    private fun reset() {
        activeUid = null
        tenantRecord = null
        notificationPreferences = DoorTreeSampleData.notificationPreferences
        pendingInvoices = emptyList()
        maintenanceRequests = emptyList()
        isLoading = false
        loadError = null
    }

    private fun debugMaintenanceRequestLog(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d("TenantDataStore", message)
        }
    }

    private fun currentMonthLabel(): String {
        return java.time.LocalDate.now()
            .format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))
    }

    private fun parsePendingInvoices(snapshot: JsonObject): List<PendingInvoiceItem> {
        val invoicesRoot = snapshot["invoices"] as? JsonObject ?: return emptyList()

        return invoiceSnapshots(invoicesRoot)
            .filter { (_, invoiceObject) -> isPendingInvoice(invoiceObject) }
            .mapNotNull { (fallbackId, invoiceObject) ->
                runCatching { invoiceFromSnapshot(fallbackId, invoiceObject) }.getOrNull()
            }
            .sortedWith(
                compareBy<PendingInvoiceItem> { it.sortDate ?: LocalDate.MAX }
                    .thenBy { it.invoiceNumber }
            )
    }

    private fun parseMaintenanceRequests(snapshot: JsonObject): List<MaintenanceRequestItem> {
        return MaintenanceRequestParser.parse(snapshot) { message ->
            if (BuildConfig.DEBUG) {
                Log.d("TenantDataStore", message)
            }
        }
    }

    private fun invoiceSnapshots(root: JsonObject): List<Pair<String, JsonObject>> {
        val invoices = mutableListOf<Pair<String, JsonObject>>()

        fun walk(path: List<String>, node: JsonObject) {
            if (looksLikeInvoice(node)) {
                invoices += (path.lastOrNull().orEmpty() to node)
                return
            }

            node.forEach { (key, value) ->
                val objectValue = value as? JsonObject ?: return@forEach
                walk(path + key, objectValue)
            }
        }

        walk(emptyList(), root)
        return invoices
    }

    private fun maintenanceRequestSnapshots(root: JsonObject): List<Pair<String, JsonObject>> {
        val requests = mutableListOf<Pair<String, JsonObject>>()

        fun walk(path: List<String>, node: JsonObject) {
            if (looksLikeMaintenanceRequest(node)) {
                requests += (path.lastOrNull().orEmpty() to node)
                return
            }

            node.forEach { (key, value) ->
                val objectValue = value as? JsonObject ?: return@forEach
                walk(path + key, objectValue)
            }
        }

        walk(emptyList(), root)
        return requests
    }

    private fun looksLikeInvoice(node: JsonObject): Boolean {
        return node.containsKey("invoiceNumber")
            || node.containsKey("dueDate")
            || node.containsKey("lineItems")
            || (node.containsKey("balance") && node.containsKey("total"))
    }

    private fun looksLikeMaintenanceRequest(node: JsonObject): Boolean {
        return node.containsKey("issue")
            || node.containsKey("description")
            || node.containsKey("assign")
            || node.containsKey("priority")
    }

    private fun invoiceFromSnapshot(fallbackId: String, snapshot: JsonObject): PendingInvoiceItem {
        val invoiceNumber = snapshot["invoiceNumber"].stringValue().ifBlank { fallbackId.ifBlank { L("maintenance.invoice.title") } }
        val propertyName = snapshot["propertyName"].stringValue()
        val recipientName = snapshot["recipientName"].stringValue()
        val recipientAddress = snapshot["recipientAddress"].stringValue()
        val recipientEmail = snapshot["recipientEmail"].stringValue()
        val recipientNumber = snapshot["recipientNumber"].stringValue()
        val issueDate = formatDate(snapshot["issueDate"].stringValue())
        val dueDate = formatDate(snapshot["dueDate"].stringValue())
        val createdAt = formatDateTime(snapshot["createdAt"].stringValue())
        val updatedAt = formatDateTime(snapshot["updatedAt"].stringValue())
        val statusLabel = localizedInvoiceStatus(snapshot["status"].stringValue())

        return PendingInvoiceItem(
            id = invoiceNumber,
            invoiceNumber = invoiceNumber,
            propertyName = propertyName,
            recipientName = recipientName,
            recipientAddress = recipientAddress,
            recipientEmail = recipientEmail,
            recipientNumber = recipientNumber,
            issueDate = issueDate,
            dueDate = dueDate,
            createdAt = createdAt,
            updatedAt = updatedAt,
            statusLabel = statusLabel,
            notes = snapshot["notes"].stringValue(),
            terms = snapshot["terms"].stringValue(),
            subtotal = formatCurrency(snapshot["subtotal"].doubleValue()),
            tpsAmount = formatCurrency(snapshot["tpsAmount"].doubleValue()),
            tvqAmount = formatCurrency(snapshot["tvqAmount"].doubleValue()),
            total = formatCurrency(snapshot["total"].doubleValue()),
            balance = formatCurrency(snapshot["balance"].doubleValue()),
            lineItems = parseInvoiceLineItems(snapshot["lineItems"]),
            sortDate = parseLocalDate(snapshot["dueDate"].stringValue()) ?: parseLocalDate(snapshot["issueDate"].stringValue())
        )
    }

    private fun maintenanceRequestFromSnapshot(
        fallbackId: String,
        snapshot: JsonObject
    ): MaintenanceRequestItem {
        val category = localizedMaintenanceCategory(snapshot["category"].stringValue())
        val issue = firstNonBlank(
            snapshot["issue"].stringValue(),
            snapshot["description"].stringValue(),
            category,
            L("maintenance.title")
        )
        val details = snapshot["description"].stringValue().ifBlank { issue }
        val requestDate = firstNonBlank(
            snapshot["date"].stringValue(),
            snapshot["preferredDate"].stringValue(),
            snapshot["createdAt"].stringValue(),
            snapshot["updatedAt"].stringValue()
        )
        val preferredDate = firstNonBlank(
            snapshot["preferredDate"].stringValue(),
            snapshot["date"].stringValue()
        )

        return MaintenanceRequestItem(
            id = fallbackId,
            title = issue,
            category = category,
            submittedDate = formatMaintenanceDate(requestDate),
            submittedDateShort = formatMaintenanceDate(requestDate, short = true),
            status = maintenanceRequestStatus(snapshot),
            issue = issue,
            details = details,
            priority = localizedMaintenancePriority(snapshot["priority"].stringValue()),
            preferredDate = formatMaintenanceDate(preferredDate),
            assignedTo = snapshot["assign"].stringValue(),
            propertyName = snapshot["property"].stringValue(),
            unit = snapshot["unit"].stringValue(),
            tenantName = snapshot["tenant"].stringValue(),
            internalNotes = snapshot["internalNotes"].stringValue(),
            costEstimate = formatCurrency(snapshot["costEstimate"].doubleValue()),
            createdAt = formatDateTime(snapshot["createdAt"].stringValue()),
            updatedAt = formatDateTime(snapshot["updatedAt"].stringValue()),
            photos = parsePhotoUrls(snapshot["photos"]),
            sortDate = parseMaintenanceDate(requestDate)
        )
    }

    private fun parseInvoiceLineItems(snapshot: JsonElement?): List<InvoiceLineItem> {
        return when (snapshot) {
            is JsonObject -> snapshot.entries
                .sortedBy { it.key.toIntOrNull() ?: Int.MAX_VALUE }
                .mapNotNull { (key, value) ->
                    val item = value as? JsonObject ?: return@mapNotNull null
                    invoiceLineItemFromSnapshot(key, item)
                }
            is JsonArray -> snapshot.mapIndexedNotNull { index, value ->
                val item = value as? JsonObject ?: return@mapIndexedNotNull null
                invoiceLineItemFromSnapshot(index.toString(), item)
            }
            else -> emptyList()
        }
    }

    private fun invoiceLineItemFromSnapshot(
        fallbackId: String,
        snapshot: JsonObject
    ): InvoiceLineItem {
        return InvoiceLineItem(
            id = fallbackId,
            description = snapshot["description"].stringValue().ifBlank {
                snapshot["category"].stringValue().ifBlank { L("maintenance.invoice.line_items") }
            },
            category = snapshot["category"].stringValue(),
            quantity = formatQuantity(snapshot["qty"].doubleValue()),
            unitPrice = formatCurrency(snapshot["price"].doubleValue()),
            amount = formatCurrency(snapshot["amount"].doubleValue()),
            taxable = snapshot["taxable"]?.jsonPrimitive?.booleanOrNull ?: false
        )
    }

    private fun isPendingInvoice(snapshot: JsonObject): Boolean {
        val normalizedStatus = snapshot["status"].stringValue().trim().lowercase(Locale.ROOT)
        val resolvedStatuses = setOf("paid", "completed", "cancelled", "canceled", "void")
        val balance = snapshot["balance"].doubleValue()
        return normalizedStatus !in resolvedStatuses && (balance == null || balance > 0.0)
    }

    private fun localizedInvoiceStatus(rawStatus: String): String {
        return when (rawStatus.trim().lowercase(Locale.ROOT)) {
            "paid" -> L("status.paid")
            "completed", "complete" -> L("status.completed")
            "in_progress", "in-progress", "in progress" -> L("status.in_progress")
            "overdue", "due" -> L("status.due")
            "pending", "sent", "open" -> L("status.pending")
            else -> rawStatus.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                .ifBlank { L("status.pending") }
        }
    }

    private fun localizedMaintenanceCategory(rawCategory: String): String {
        return when (rawCategory.trim().lowercase(Locale.ROOT)) {
            "refund request", "refund_request" -> "Refund Request"
            "plumbing" -> MaintenanceCategory.Plumbing.localizedTitle
            "electrical" -> MaintenanceCategory.Electrical.localizedTitle
            "hvac" -> MaintenanceCategory.Hvac.localizedTitle
            "heating" -> MaintenanceCategory.Heating.localizedTitle
            "cooling" -> MaintenanceCategory.Cooling.localizedTitle
            "appliance repair", "appliance_repair", "appliances" -> MaintenanceCategory.ApplianceRepair.localizedTitle
            "general repair", "general_repair" -> MaintenanceCategory.GeneralRepair.localizedTitle
            "carpentry" -> MaintenanceCategory.Carpentry.localizedTitle
            "painting" -> MaintenanceCategory.Painting.localizedTitle
            "flooring" -> MaintenanceCategory.Flooring.localizedTitle
            "locksmith" -> MaintenanceCategory.Locksmith.localizedTitle
            "pest control", "pest_control" -> MaintenanceCategory.PestControl.localizedTitle
            "cleaning" -> MaintenanceCategory.Cleaning.localizedTitle
            "roofing" -> MaintenanceCategory.Roofing.localizedTitle
            "exterior" -> MaintenanceCategory.Exterior.localizedTitle
            "landscaping" -> MaintenanceCategory.Landscaping.localizedTitle
            "snow removal", "snow_removal" -> MaintenanceCategory.SnowRemoval.localizedTitle
            "drywall" -> MaintenanceCategory.Drywall.localizedTitle
            "water damage", "water_damage" -> MaintenanceCategory.WaterDamage.localizedTitle
            "mold" -> MaintenanceCategory.Mold.localizedTitle
            "inspection" -> MaintenanceCategory.Inspection.localizedTitle
            "preventive maintenance", "preventive_maintenance" -> MaintenanceCategory.PreventiveMaintenance.localizedTitle
            "emergency" -> MaintenanceCategory.Emergency.localizedTitle
            "other" -> MaintenanceCategory.Other.localizedTitle
            else -> rawCategory.trim().ifBlank { MaintenanceCategory.Other.localizedTitle }
        }
    }

    private fun localizedMaintenancePriority(rawPriority: String): String {
        return when (rawPriority.trim().lowercase(Locale.ROOT)) {
            "low" -> MaintenancePriority.Low.localizedTitle
            "medium" -> MaintenancePriority.Medium.localizedTitle
            "high" -> MaintenancePriority.High.localizedTitle
            "urgent" -> MaintenancePriority.Urgent.localizedTitle
            "emergency" -> MaintenancePriority.Emergency.localizedTitle
            else -> rawPriority.trim().replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }.ifBlank { MaintenancePriority.Low.localizedTitle }
        }
    }

    private fun maintenanceRequestStatus(snapshot: JsonObject): StatusBadgeStyle {
        return when (snapshot["status"].stringValue().trim().lowercase(Locale.ROOT)) {
            "completed", "complete", "resolved", "closed" -> StatusBadgeStyle.Completed
            "in progress", "in_progress", "in-progress", "assigned" -> StatusBadgeStyle.InProgress
            "due", "overdue" -> StatusBadgeStyle.Due
            "paid" -> StatusBadgeStyle.Paid
            "pending", "submitted", "sent", "open", "" -> {
                if (snapshot["assign"].stringValue().isNotBlank()) StatusBadgeStyle.InProgress else StatusBadgeStyle.Pending
            }
            else -> {
                if (snapshot["assign"].stringValue().isNotBlank()) StatusBadgeStyle.InProgress else StatusBadgeStyle.Pending
            }
        }
    }

    private fun formatCurrency(value: Double?): String {
        return value?.let {
            NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }.format(it)
        } ?: "-"
    }

    private fun formatQuantity(value: Double?): String {
        val quantity = value ?: return "-"
        return if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString()
    }

    private fun formatDate(raw: String): String {
        return parseLocalDate(raw)
            ?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
            ?: raw.ifBlank { "-" }
    }

    private fun formatMaintenanceDate(raw: String, short: Boolean = false): String {
        if (raw.isBlank()) return "-"

        parseLocalDate(raw)?.let { date ->
            val formatter = if (short) {
                DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
            } else {
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
            }
            return date.format(formatter)
        }

        return runCatching {
            Instant.parse(raw)
                .atZone(ZoneId.systemDefault())
                .format(
                    if (short) {
                        DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
                    } else {
                        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
                    }
                )
        }.getOrElse {
            raw
        }
    }

    private fun formatDateTime(raw: String): String {
        if (raw.isBlank()) return "-"
        return runCatching {
            Instant.parse(raw)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
        }.getOrElse {
            raw
        }
    }

    private fun parseLocalDate(raw: String): LocalDate? {
        if (raw.isBlank()) return null
        return runCatching {
            LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE)
        }.getOrNull()
    }

    private fun parseMaintenanceDate(raw: String): LocalDate? {
        return parseLocalDate(raw)
            ?: runCatching {
                Instant.parse(raw).atZone(ZoneId.systemDefault()).toLocalDate()
            }.getOrNull()
    }

    private fun parsePhotoUrls(snapshot: JsonElement?): List<String> {
        return when (snapshot) {
            is JsonObject -> snapshot.entries
                .sortedBy { it.key.toIntOrNull() ?: Int.MAX_VALUE }
                .mapNotNull { (_, value) -> value.stringValue().ifBlank { null } }
            is JsonArray -> snapshot.mapNotNull { value -> value.stringValue().ifBlank { null } }
            else -> emptyList()
        }
    }

    private fun resolveLandlordUid(tenantRecord: TenantRecord): String {
        return tenantRecord.landlordUID.trim()
            .ifBlank { throw IllegalStateException(L("maintenance.submit_error.landlord_missing")) }
    }

    private suspend fun uploadMaintenancePhotos(
        photos: List<MaintenancePhotoUpload>,
        tenantUid: String,
        landlordUid: String,
        requestDate: String,
        requestId: String
    ): List<String> {
        if (photos.isEmpty()) {
            return emptyList()
        }

        val candidateOwnerUids = listOf(landlordUid, tenantUid).distinct().filter { it.isNotBlank() }
        return photos.mapIndexed { index, photo ->
            val extension = photo.fileExtension.trim().ifBlank { "jpg" }
            val filename = "$requestId-${index + 1}.$extension"
            val metadata = StorageMetadata.Builder()
                .setContentType(photo.contentType.ifBlank { "image/jpeg" })
                .build()
            var lastError: Exception? = null

            for (ownerUid in candidateOwnerUids) {
                val path = "Users/$ownerUid/requests/$requestDate/photos/$filename"
                val reference = storage.reference.child(path)

                try {
                    reference.putBytes(photo.bytes, metadata).await()
                    return@mapIndexed reference.downloadUrl.await().toString()
                } catch (error: Exception) {
                    lastError = error
                    val storageError = error as? StorageException
                    val isPermissionIssue = storageError?.errorCode == StorageException.ERROR_NOT_AUTHORIZED ||
                        storageError?.errorCode == StorageException.ERROR_NOT_AUTHENTICATED
                    if (!isPermissionIssue || ownerUid == candidateOwnerUids.last()) {
                        break
                    }
                }
            }

            throw lastError ?: IllegalStateException("Unable to upload maintenance photo.")
        }
    }

    private fun maintenanceRequestPath(uid: String, date: LocalDate, requestId: String): String {
        val month = date.format(DateTimeFormatter.ofPattern("LLLL", Locale.ENGLISH))
        return "users/$uid/maintenance/requests/${date.year}/$month/${date.dayOfMonth}/$requestId"
    }

    private fun maintenanceIssueSummary(description: String): String {
        val trimmed = description.trim()
        return if (trimmed.length <= 80) {
            trimmed
        } else {
            trimmed.take(80).trim()
        }
    }

    private fun resolvedMaintenanceCategoryValue(category: MaintenanceCategory, customCategoryName: String): String {
        if (category == MaintenanceCategory.Other) {
            val trimmed = customCategoryName.trim()
            return if (trimmed.isBlank()) category.defaultTitle else trimmed
        }

        return category.defaultTitle
    }

    private fun validatedRefundAmount(rawValue: String?, isRefundRequest: Boolean): Double? {
        if (!isRefundRequest) {
            return null
        }

        val trimmed = rawValue?.trim().orEmpty()
        if (trimmed.isBlank()) {
            throw IllegalStateException("Enter a refund amount.")
        }

        val sanitized = trimmed.replace("$", "").replace(" ", "")
        val parsed = NumberFormat.getNumberInstance(Locale.getDefault()).parse(sanitized, ParsePosition(0))?.toDouble()
        if (parsed != null && parsed > 0) {
            return parsed
        }

        val normalized = sanitized.replace(",", ".").replace(Regex("[^0-9.]"), "")
        val fallback = normalized.toDoubleOrNull()
        if (fallback != null && fallback > 0) {
            return fallback
        }

        throw IllegalStateException("Enter a valid refund amount.")
    }

    private fun firstNonBlank(vararg values: String): String {
        return values.firstOrNull { it.isNotBlank() }.orEmpty()
    }

    private fun JsonElement?.stringValue(): String {
        return this?.jsonPrimitive?.content?.trim().orEmpty()
    }

    private fun JsonElement?.doubleValue(): Double? {
        return this?.jsonPrimitive?.doubleOrNull
            ?: this?.jsonPrimitive?.content?.toDoubleOrNull()
    }
}

internal object MaintenanceRequestParser {
    fun parse(
        snapshot: JsonObject,
        logger: ((String) -> Unit)? = null
    ): List<MaintenanceRequestItem> {
        val requestsRoot = (snapshot["maintenance"] as? JsonObject)?.get("requests") as? JsonObject
        return parseRequestsRoot(requestsRoot, logger)
    }

    fun parseRequestsRoot(
        requestsRoot: JsonObject?,
        logger: ((String) -> Unit)? = null
    ): List<MaintenanceRequestItem> {
        if (requestsRoot == null) {
            logger?.invoke("maintenance requests root missing")
            return emptyList()
        }

        val snapshots = requestLeafSnapshots(requestsRoot, logger)
        logger?.invoke("maintenance requests discovered: ${snapshots.size}")

        val parsed = snapshots.mapNotNull { snapshot ->
            runCatching { maintenanceRequestFromSnapshot(snapshot) }.getOrNull()
        }
        logger?.invoke("maintenance requests mapped: ${parsed.size}")

        return parsed
            .sortedWith(
                compareByDescending<ParsedMaintenanceRequest> { it.primarySortDate ?: LocalDate.MIN }
                    .thenByDescending { it.secondarySortInstant ?: Instant.EPOCH }
                    .thenBy { it.item.id }
            )
            .map { it.item }
    }

    private fun requestLeafSnapshots(
        root: JsonObject,
        logger: ((String) -> Unit)? = null
    ): List<MaintenanceRequestLeaf> {
        val requests = mutableListOf<MaintenanceRequestLeaf>()

        root.forEach { (yearKey, yearValue) ->
            val months = yearValue as? JsonObject ?: return@forEach

            months.forEach { (monthKey, monthValue) ->
                dayEntries(monthValue).forEach { (dayKey, dayValue) ->
                    val requestNodes = dayValue as? JsonObject ?: return@forEach
                    val fallbackDate = pathDate(yearKey, monthKey, dayKey)

                    requestNodes.forEach { (requestId, requestValue) ->
                        val requestObject = requestValue as? JsonObject ?: return@forEach
                        requests += MaintenanceRequestLeaf(
                            id = requestId,
                            snapshot = requestObject,
                            fallbackDate = fallbackDate
                        )
                        logger?.invoke(
                            "discovered request leaf id=$requestId fallbackDate=${fallbackDate ?: "null"} keys=${requestObject.keys.sorted().joinToString(",")}"
                        )
                    }
                }
            }
        }

        return requests
    }

    private fun dayEntries(monthValue: JsonElement): List<Pair<String, JsonElement>> {
        val daysObject = monthValue as? JsonObject
        if (daysObject != null) {
            return daysObject.entries.map { it.key to it.value }
        }

        val daysArray = monthValue as? JsonArray ?: return emptyList()
        return daysArray.mapIndexedNotNull { index, dayValue ->
            if (index == 0 || dayValue is JsonNull) {
                null
            } else {
                index.toString() to dayValue
            }
        }
    }

    private fun maintenanceRequestFromSnapshot(snapshot: MaintenanceRequestLeaf): ParsedMaintenanceRequest {
        val requestObject = snapshot.snapshot
        val category = localizedMaintenanceCategory(requestObject["category"].stringValue())
        val issue = firstNonBlank(
            requestObject["issue"].stringValue(),
            requestObject["description"].stringValue(),
            category,
            L("maintenance.title")
        )
        val details = requestObject["description"].stringValue().ifBlank { issue }
        val requestDate = firstNonBlank(
            requestObject["date"].stringValue(),
            requestObject["preferredDate"].stringValue(),
            snapshot.fallbackDate.orEmpty(),
            requestObject["createdAt"].stringValue(),
            requestObject["updatedAt"].stringValue()
        )
        val preferredDate = firstNonBlank(
            requestObject["preferredDate"].stringValue(),
            requestObject["date"].stringValue(),
            snapshot.fallbackDate.orEmpty()
        )
        val primarySortDate = parseMaintenanceDate(requestObject["date"].stringValue())
            ?: parseMaintenanceDate(requestObject["preferredDate"].stringValue())
            ?: parseMaintenanceDate(snapshot.fallbackDate.orEmpty())
            ?: parseMaintenanceDate(requestObject["createdAt"].stringValue())
            ?: parseMaintenanceDate(requestObject["updatedAt"].stringValue())
        val secondarySortInstant = parseInstant(requestObject["updatedAt"].stringValue())
            ?: parseInstant(requestObject["createdAt"].stringValue())

        val item = MaintenanceRequestItem(
            id = snapshot.id,
            title = issue,
            category = category,
            submittedDate = formatMaintenanceDate(requestDate),
            submittedDateShort = formatMaintenanceDate(requestDate, short = true),
            status = maintenanceRequestStatus(requestObject),
            issue = issue,
            details = details,
            priority = localizedMaintenancePriority(requestObject["priority"].stringValue()),
            preferredDate = formatMaintenanceDate(preferredDate),
            assignedTo = requestObject["assign"].stringValue(),
            propertyName = requestObject["property"].stringValue(),
            unit = requestObject["unit"].stringValue(),
            tenantName = requestObject["tenant"].stringValue(),
            internalNotes = requestObject["internalNotes"].stringValue(),
            costEstimate = formatCurrency(requestObject["costEstimate"].doubleValue()),
            createdAt = formatDateTime(requestObject["createdAt"].stringValue()),
            updatedAt = formatDateTime(requestObject["updatedAt"].stringValue()),
            photos = parsePhotoUrls(requestObject["photos"]),
            sortDate = primarySortDate
        )

        return ParsedMaintenanceRequest(
            item = item,
            primarySortDate = primarySortDate,
            secondarySortInstant = secondarySortInstant
        )
    }

    private fun pathDate(year: String, month: String, day: String): String? {
        val yearValue = year.trim().toIntOrNull() ?: return null
        val dayValue = day.trim().toIntOrNull() ?: return null
        val monthValue = monthNames.indexOfFirst { it.equals(month.trim(), ignoreCase = true) }
            .takeIf { it >= 0 }
            ?.plus(1)
            ?: return null

        return "%04d-%02d-%02d".format(Locale.ENGLISH, yearValue, monthValue, dayValue)
    }

    private fun parsePhotoUrls(snapshot: JsonElement?): List<String> {
        return when (snapshot) {
            is JsonObject -> snapshot.entries
                .sortedBy { it.key.toIntOrNull() ?: Int.MAX_VALUE }
                .mapNotNull { (_, value) -> value.stringValue().ifBlank { null } }
            is JsonArray -> snapshot.mapNotNull { value -> value.stringValue().ifBlank { null } }
            else -> emptyList()
        }
    }

    private fun formatMaintenanceDate(raw: String, short: Boolean = false): String {
        if (raw.isBlank()) return "-"

        parseMaintenanceDate(raw)?.let { date ->
            val formatter = if (short) {
                DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
            } else {
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
            }
            return date.format(formatter)
        }

        return runCatching {
            Instant.parse(raw)
                .atZone(ZoneId.systemDefault())
                .format(
                    if (short) {
                        DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
                    } else {
                        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
                    }
                )
        }.getOrElse {
            raw
        }
    }

    private fun formatDateTime(raw: String): String {
        if (raw.isBlank()) return "-"
        return runCatching {
            Instant.parse(raw)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
        }.getOrElse {
            raw
        }
    }

    private fun parseMaintenanceDate(raw: String): LocalDate? {
        if (raw.isBlank()) return null
        return runCatching {
            LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE)
        }.getOrNull() ?: runCatching {
            Instant.parse(raw).atZone(ZoneId.systemDefault()).toLocalDate()
        }.getOrNull()
    }

    private fun parseInstant(raw: String): Instant? {
        if (raw.isBlank()) return null
        return runCatching { Instant.parse(raw) }.getOrNull()
    }

    private fun maintenanceRequestStatus(snapshot: JsonObject): StatusBadgeStyle {
        return when (snapshot["status"].stringValue().trim().lowercase(Locale.ROOT)) {
            "completed", "complete", "resolved", "closed" -> StatusBadgeStyle.Completed
            "in progress", "in_progress", "in-progress", "assigned" -> StatusBadgeStyle.InProgress
            "due", "overdue" -> StatusBadgeStyle.Due
            "paid" -> StatusBadgeStyle.Paid
            "pending", "submitted", "sent", "open", "" -> {
                if (snapshot["assign"].stringValue().isNotBlank()) StatusBadgeStyle.InProgress else StatusBadgeStyle.Pending
            }
            else -> {
                if (snapshot["assign"].stringValue().isNotBlank()) StatusBadgeStyle.InProgress else StatusBadgeStyle.Pending
            }
        }
    }

    private fun localizedMaintenanceCategory(rawCategory: String): String {
        return when (rawCategory.trim().lowercase(Locale.ROOT)) {
            "refund request", "refund_request" -> "Refund Request"
            "plumbing" -> MaintenanceCategory.Plumbing.localizedTitle
            "electrical" -> MaintenanceCategory.Electrical.localizedTitle
            "hvac" -> MaintenanceCategory.Hvac.localizedTitle
            "heating" -> MaintenanceCategory.Heating.localizedTitle
            "cooling" -> MaintenanceCategory.Cooling.localizedTitle
            "appliance repair", "appliance_repair", "appliances" -> MaintenanceCategory.ApplianceRepair.localizedTitle
            "general repair", "general_repair" -> MaintenanceCategory.GeneralRepair.localizedTitle
            "carpentry" -> MaintenanceCategory.Carpentry.localizedTitle
            "painting" -> MaintenanceCategory.Painting.localizedTitle
            "flooring" -> MaintenanceCategory.Flooring.localizedTitle
            "locksmith" -> MaintenanceCategory.Locksmith.localizedTitle
            "pest control", "pest_control" -> MaintenanceCategory.PestControl.localizedTitle
            "cleaning" -> MaintenanceCategory.Cleaning.localizedTitle
            "roofing" -> MaintenanceCategory.Roofing.localizedTitle
            "exterior" -> MaintenanceCategory.Exterior.localizedTitle
            "landscaping" -> MaintenanceCategory.Landscaping.localizedTitle
            "snow removal", "snow_removal" -> MaintenanceCategory.SnowRemoval.localizedTitle
            "drywall" -> MaintenanceCategory.Drywall.localizedTitle
            "water damage", "water_damage" -> MaintenanceCategory.WaterDamage.localizedTitle
            "mold" -> MaintenanceCategory.Mold.localizedTitle
            "inspection" -> MaintenanceCategory.Inspection.localizedTitle
            "preventive maintenance", "preventive_maintenance" -> MaintenanceCategory.PreventiveMaintenance.localizedTitle
            "emergency" -> MaintenanceCategory.Emergency.localizedTitle
            "other" -> MaintenanceCategory.Other.localizedTitle
            else -> rawCategory.trim().ifBlank { MaintenanceCategory.Other.localizedTitle }
        }
    }

    private fun localizedMaintenancePriority(rawPriority: String): String {
        return when (rawPriority.trim().lowercase(Locale.ROOT)) {
            "low" -> MaintenancePriority.Low.localizedTitle
            "medium" -> MaintenancePriority.Medium.localizedTitle
            "high" -> MaintenancePriority.High.localizedTitle
            "urgent" -> MaintenancePriority.Urgent.localizedTitle
            "emergency" -> MaintenancePriority.Emergency.localizedTitle
            else -> rawPriority.trim().replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }.ifBlank { MaintenancePriority.Low.localizedTitle }
        }
    }

    private fun formatCurrency(value: Double?): String {
        return value?.let {
            NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }.format(it)
        } ?: "-"
    }

    private fun firstNonBlank(vararg values: String): String {
        return values.firstOrNull { it.isNotBlank() }.orEmpty()
    }

    private fun JsonElement?.stringValue(): String {
        return this?.jsonPrimitive?.content?.trim().orEmpty()
    }

    private fun JsonElement?.doubleValue(): Double? {
        return this?.jsonPrimitive?.doubleOrNull
            ?: this?.jsonPrimitive?.content?.toDoubleOrNull()
    }

    private data class MaintenanceRequestLeaf(
        val id: String,
        val snapshot: JsonObject,
        val fallbackDate: String?
    )

    private data class ParsedMaintenanceRequest(
        val item: MaintenanceRequestItem,
        val primarySortDate: LocalDate?,
        val secondarySortInstant: Instant?
    )

    private val monthNames = listOf(
        "January",
        "February",
        "March",
        "April",
        "May",
        "June",
        "July",
        "August",
        "September",
        "October",
        "November",
        "December"
    )
}
