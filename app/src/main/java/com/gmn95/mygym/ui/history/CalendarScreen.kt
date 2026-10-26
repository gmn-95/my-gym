package com.gmn95.mygym.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gmn95.mygym.R
import com.gmn95.mygym.data.repository.CompletedWorkoutSessionSummary
import com.gmn95.mygym.feature.history.HistoryUiState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields

@Composable
fun CalendarScreen(
    uiState: HistoryUiState,
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    var monthOffset by rememberSaveable { mutableLongStateOf(0L) }
    var selectedEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val locale = LocalLocale.current.platformLocale
    val month = YearMonth.now().plusMonths(monthOffset)
    val sessionsByDay = remember(uiState.sessions) {
        uiState.sessions.groupBy { session ->
            java.time.Instant.ofEpochMilli(session.endedAtEpochMillis)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        }
    }
    val selectedDate = selectedEpochDay?.let(LocalDate::ofEpochDay)
    val selectedSessions = selectedDate?.let(sessionsByDay::get).orEmpty()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.back))
            }
            Text(
                text = stringResource(R.string.calendar),
                style = MaterialTheme.typography.headlineMedium,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { monthOffset -= 1 }) {
                    Text(stringResource(R.string.previous_month))
                }
                Text(
                    text = month.month.getDisplayName(TextStyle.FULL, locale) +
                        " ${month.year}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = { monthOffset += 1 }) {
                    Text(stringResource(R.string.next_month))
                }
            }
            CalendarMonth(
                month = month,
                sessionsByDay = sessionsByDay,
                selectedDate = selectedDate,
                locale = locale,
                onSelectDate = { selectedEpochDay = it.toEpochDay() },
            )
        }
        item {
            Text(
                text = selectedDate?.format(
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                        .withLocale(locale),
                ) ?: stringResource(R.string.select_training_day),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        items(selectedSessions, key = CompletedWorkoutSessionSummary::id) { session ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSession(session.id) },
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(session.workoutName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(
                            R.string.history_duration,
                            formatDuration(session.activeDurationMillis),
                        ),
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarMonth(
    month: YearMonth,
    sessionsByDay: Map<LocalDate, List<CompletedWorkoutSessionSummary>>,
    selectedDate: LocalDate?,
    locale: java.util.Locale,
    onSelectDate: (LocalDate) -> Unit,
) {
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val weekdays = DayOfWeek.entries.let { days ->
        days.drop(firstDayOfWeek.ordinal) + days.take(firstDayOfWeek.ordinal)
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        weekdays.forEach { day ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    val blankDays = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val gridDays = List<LocalDate?>(blankDays) { null } +
        (1..month.lengthOfMonth()).map(month::atDay)
    gridDays.chunked(7).forEach { week ->
        Row(modifier = Modifier.fillMaxWidth()) {
            week.forEach { date ->
                Box(modifier = Modifier.weight(1f)) {
                    CalendarDay(
                        date = date,
                        hasSessions = date != null && sessionsByDay.containsKey(date),
                        isSelected = date == selectedDate,
                        onSelect = { date?.let(onSelectDate) },
                    )
                }
            }
            repeat(7 - week.size) {
                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
            }
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate?,
    hasSessions: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val color = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        hasSessions -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        hasSessions -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(MaterialTheme.shapes.small)
            .background(color)
            .then(
                if (hasSessions) Modifier.clickable(onClick = onSelect) else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        date?.let {
            Text(
                text = it.dayOfMonth.toString(),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
