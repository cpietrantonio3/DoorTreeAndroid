package codewhale.doortreeandroid

import codewhale.doortreeandroid.ui.theme.DoorTreeTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object RentScheduleBuilder {
    private const val maxEntries = 240
    private val parser = DateTimeFormatter.ISO_LOCAL_DATE

    fun entries(
        rentEntries: List<RentLedgerEntry> = emptyList(),
        tenantRecord: TenantRecord?,
        leaseDetails: LeaseDetails
    ): List<RentScheduleEntry> {
        if (rentEntries.isNotEmpty()) {
            return rentEntries
                .sortedBy { it.sortDate ?: LocalDate.MIN }
                .mapNotNull { entry ->
                    val dueDate = entry.sortDate ?: runCatching {
                        LocalDate.parse(entry.dueDate, parser)
                    }.getOrNull() ?: return@mapNotNull null
                    val status = liveStatusFor(entry, dueDate)
                    RentScheduleEntry(
                        dueDate = dueDate,
                        amount = entry.amount,
                        statusLabel = status.label,
                        accentColor = status.accentColor,
                        accentBackground = status.accentBackground
                    )
                }
        }

        val record = tenantRecord ?: return emptyList()
        val startDate = runCatching { LocalDate.parse(record.leaseStart, parser) }.getOrNull() ?: return emptyList()
        val endDate = runCatching { LocalDate.parse(record.leaseEnd, parser) }.getOrNull()

        val dueDates = mutableListOf<LocalDate>()
        var current = startDate

        repeat(maxEntries) {
            if (endDate != null && current.isAfter(endDate)) return@repeat
            dueDates += current
            current = current.plusMonths(1)
        }

        if (
            endDate != null &&
            dueDates.size > 1 &&
            dueDates.last() == endDate &&
            startDate.dayOfMonth == endDate.dayOfMonth
        ) {
            dueDates.removeLast()
        }

        return dueDates.map { dueDate ->
            val status = statusFor(dueDate)
            RentScheduleEntry(
                dueDate = dueDate,
                amount = leaseDetails.monthlyRent,
                statusLabel = status.label,
                accentColor = status.accentColor,
                accentBackground = status.accentBackground
            )
        }
    }

    private fun liveStatusFor(entry: RentLedgerEntry, dueDate: LocalDate): ScheduleStatus {
        val now = LocalDate.now()
        return when {
            entry.isPaid -> ScheduleStatus(
                label = L("status.paid"),
                accentColor = DoorTreeTheme.paidText,
                accentBackground = DoorTreeTheme.paidBackground
            )
            dueDate.isAfter(now) -> ScheduleStatus(
                label = L("payments.schedule.status.upcoming"),
                accentColor = DoorTreeTheme.dueText,
                accentBackground = DoorTreeTheme.dueBackground
            )
            dueDate.isEqual(now) -> ScheduleStatus(
                label = L("status.due"),
                accentColor = DoorTreeTheme.pendingText,
                accentBackground = DoorTreeTheme.pendingBackground
            )
            else -> ScheduleStatus(
                label = L("status.overdue"),
                accentColor = DoorTreeTheme.destructive,
                accentBackground = DoorTreeTheme.destructive.copy(alpha = 0.14f)
            )
        }
    }

    private fun statusFor(dueDate: LocalDate): ScheduleStatus {
        val now = LocalDate.now()
        return when {
            dueDate.year == now.year && dueDate.month == now.month -> ScheduleStatus(
                label = L("status.due"),
                accentColor = DoorTreeTheme.pendingText,
                accentBackground = DoorTreeTheme.pendingBackground
            )
            dueDate.isAfter(now) -> ScheduleStatus(
                label = L("payments.schedule.status.upcoming"),
                accentColor = DoorTreeTheme.dueText,
                accentBackground = DoorTreeTheme.dueBackground
            )
            else -> ScheduleStatus(
                label = L("status.overdue"),
                accentColor = DoorTreeTheme.destructive,
                accentBackground = DoorTreeTheme.destructive.copy(alpha = 0.14f)
            )
        }
    }

    private data class ScheduleStatus(
        val label: String,
        val accentColor: androidx.compose.ui.graphics.Color,
        val accentBackground: androidx.compose.ui.graphics.Color
    )
}
