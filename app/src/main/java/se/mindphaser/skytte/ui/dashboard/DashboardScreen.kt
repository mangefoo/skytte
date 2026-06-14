package se.mindphaser.skytte.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import se.mindphaser.skytte.R
import se.mindphaser.skytte.ui.SkytteTopBar
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private val swedish = Locale.forLanguageTag("sv-SE")
private val monthLabelFormatter = DateTimeFormatter.ofPattern("MMM", swedish)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenSettings: () -> Unit,
    vm: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
) {
    val stats by vm.stats.collectAsState(initial = DashboardStats.EMPTY)

    Scaffold(
        topBar = {
            SkytteTopBar(
                title = stringResource(R.string.tab_dashboard),
                onOpenSettings = onOpenSettings
            )
        }
    ) { padding ->
        if (stats.totalSessions == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.empty_dashboard),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummarySection(stats)
            CostSection(stats)
            MonthlyChartCard(
                title = stringResource(R.string.sessions_per_month),
                buckets = stats.monthlySessions
            )
            MonthlyChartCard(
                title = stringResource(R.string.shots_per_month),
                buckets = stats.monthlyShots
            )
            MonthlyChartCard(
                title = stringResource(R.string.cost_per_month),
                buckets = stats.monthlyCost
            )
            BreakdownCard(
                title = stringResource(R.string.shots_per_weapon),
                items = stats.shotsPerWeapon,
                barColor = MaterialTheme.colorScheme.primary
            )
            BreakdownCard(
                title = stringResource(R.string.shots_per_ammo),
                items = stats.shotsPerAmmo,
                barColor = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun SummarySection(stats: DashboardStats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.stat_shots_title),
            style = MaterialTheme.typography.titleMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                value = stats.totalShots,
                label = stringResource(R.string.stat_total),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = stats.shotsThisYear,
                label = stringResource(R.string.stat_this_year),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = stats.shotsLast30Days,
                label = stringResource(R.string.stat_last_30_days),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CostSection(stats: DashboardStats) {
    fun kr(amount: Double) = String.format(Locale.forLanguageTag("sv-SE"), "%.0f kr", amount)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.stat_cost_title),
            style = MaterialTheme.typography.titleMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                valueText = kr(stats.totalCost),
                label = stringResource(R.string.stat_total),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                valueText = kr(stats.costThisYear),
                label = stringResource(R.string.stat_this_year),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                valueText = kr(stats.costLast30Days),
                label = stringResource(R.string.stat_last_30_days),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatTile(value: Int, label: String, modifier: Modifier = Modifier) =
    StatTile(valueText = value.toString(), label = label, modifier = modifier)

@Composable
private fun StatTile(valueText: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Smallest "nice" gridline step (1·10ⁿ, 2·10ⁿ or 5·10ⁿ) so the axis has ~[targetLines] divisions. */
private fun niceStep(rawMax: Int, targetLines: Int = 4): Int {
    if (rawMax <= 0) return 1
    val rough = rawMax.toDouble() / targetLines
    val magnitude = 10.0.pow(floor(log10(rough)))
    val normalized = rough / magnitude
    val niceNormalized = when {
        normalized <= 1.0 -> 1.0
        normalized <= 2.0 -> 2.0
        normalized <= 5.0 -> 5.0
        else -> 10.0
    }
    return (niceNormalized * magnitude).toInt().coerceAtLeast(1)
}

@Composable
private fun MonthlyChartCard(title: String, buckets: List<MonthBucket>) {
    val plotHeight = 140.dp
    val maxValue = buckets.maxOfOrNull { it.value } ?: 0
    val step = niceStep(maxValue)
    val axisMax = if (maxValue <= 0) step else ceil(maxValue.toFloat() / step).toInt() * step
    // Gridline values from top (axisMax) down to 0.
    val gridValues = (axisMax downTo 0 step step).toList()

    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                // Y-axis labels, evenly spaced to line up with the gridlines.
                Column(
                    modifier = Modifier
                        .height(plotHeight)
                        .width(28.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    gridValues.forEach { value ->
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = axisLabelColor,
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                // Plot area (gridlines + bars) and the month labels beneath, aligned by shared weights.
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(plotHeight)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            gridValues.forEach { _ ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(gridColor)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            buckets.forEach { bucket ->
                                val fraction =
                                    if (axisMax > 0) bucket.value.toFloat() / axisMax else 0f
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    if (bucket.value > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                .background(barColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        buckets.forEach { bucket ->
                            Text(
                                text = bucket.month.format(monthLabelFormatter),
                                style = MaterialTheme.typography.labelSmall,
                                color = axisLabelColor,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakdownCard(title: String, items: List<LabeledCount>, barColor: Color) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            val topCount = items.firstOrNull()?.shots ?: 0
            val unknownLabel = stringResource(R.string.unknown_label)
            items.forEach { item ->
                BreakdownRow(
                    label = item.label ?: unknownLabel,
                    shots = item.shots,
                    fraction = if (topCount > 0) item.shots.toFloat() / topCount else 0f,
                    barColor = barColor
                )
            }
        }
    }
}

@Composable
private fun BreakdownRow(label: String, shots: Int, fraction: Float, barColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = shots.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}
