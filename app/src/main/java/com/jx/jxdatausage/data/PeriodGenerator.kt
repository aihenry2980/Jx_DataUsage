package com.jx.jxdatausage.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

object PeriodGenerator {
    private val dayFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    fun startOfTodayMs(zoneId: ZoneId = ZoneId.systemDefault(), now: Instant = Instant.now()): Long {
        return now.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun generatePeriods(
        tab: PeriodTab,
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<PeriodItem> {
        return when (tab) {
            PeriodTab.DAILY -> generateDaily(now, zoneId)
            PeriodTab.WEEKLY -> generateWeekly(now, zoneId)
            PeriodTab.MONTHLY -> generateMonthly(now, zoneId)
            PeriodTab.YEARLY -> generateYearly(now, zoneId)
        }
    }

    private fun generateDaily(now: Instant, zoneId: ZoneId): List<PeriodItem> {
        val today = now.atZone(zoneId).toLocalDate()
        return (0 until 30).map { offset ->
            val day = today.minusDays(offset.toLong())
            val start = day.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val nextStart = day.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val end = if (offset == 0) now.toEpochMilli() else nextStart
            PeriodItem(
                id = "daily-${day}",
                label = if (offset == 0) "Today" else day.format(dayFormatter),
                startMs = start,
                endMs = end,
                tab = PeriodTab.DAILY
            )
        }
    }

    private fun generateWeekly(now: Instant, zoneId: ZoneId): List<PeriodItem> {
        val weekFields = WeekFields.of(Locale.getDefault())
        val firstDayOfWeek = weekFields.firstDayOfWeek
        val today = now.atZone(zoneId).toLocalDate()
        val thisWeekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        return (0 until 12).map { offset ->
            val startDay = thisWeekStart.minusWeeks(offset.toLong())
            val endDay = startDay.plusWeeks(1)
            val start = startDay.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val end = if (offset == 0) now.toEpochMilli() else endDay.atStartOfDay(zoneId).toInstant().toEpochMilli()
            PeriodItem(
                id = "weekly-${startDay}",
                label = if (offset == 0) "This Week" else "Week of ${startDay.format(dayFormatter)}",
                startMs = start,
                endMs = end,
                tab = PeriodTab.WEEKLY
            )
        }
    }

    private fun generateMonthly(now: Instant, zoneId: ZoneId): List<PeriodItem> {
        val nowZdt = now.atZone(zoneId)
        val thisMonthStart = nowZdt.withDayOfMonth(1).toLocalDate()
        return (0 until 12).map { offset ->
            val monthStart = thisMonthStart.minusMonths(offset.toLong())
            val nextMonthStart = monthStart.plusMonths(1)
            val start = monthStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val end = if (offset == 0) now.toEpochMilli() else nextMonthStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
            PeriodItem(
                id = "monthly-${monthStart.year}-${monthStart.monthValue}",
                label = if (offset == 0) "This Month" else monthStart.format(monthFormatter),
                startMs = start,
                endMs = end,
                tab = PeriodTab.MONTHLY
            )
        }
    }

    private fun generateYearly(now: Instant, zoneId: ZoneId): List<PeriodItem> {
        val currentYear = now.atZone(zoneId).year
        return (0 until 5).map { offset ->
            val year = currentYear - offset
            val start = ZonedDateTime.of(year, 1, 1, 0, 0, 0, 0, zoneId).toInstant().toEpochMilli()
            val end = if (offset == 0) {
                now.toEpochMilli()
            } else {
                ZonedDateTime.of(year + 1, 1, 1, 0, 0, 0, 0, zoneId).toInstant().toEpochMilli()
            }
            PeriodItem(
                id = "yearly-$year",
                label = if (offset == 0) "This Year" else year.toString(),
                startMs = start,
                endMs = end,
                tab = PeriodTab.YEARLY
            )
        }
    }
}

