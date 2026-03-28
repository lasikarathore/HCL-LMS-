package com.phegondev.InventoryManagementSystem.service;

import com.phegondev.InventoryManagementSystem.dto.ChartPointDTO;
import com.phegondev.InventoryManagementSystem.dto.DashboardSparklineCardDTO;
import com.phegondev.InventoryManagementSystem.dto.HorizontalMetricDTO;
import com.phegondev.InventoryManagementSystem.dto.TopProductRowDTO;
import com.phegondev.InventoryManagementSystem.enums.TransactionType;
import com.phegondev.InventoryManagementSystem.repository.ProductRepository;
import com.phegondev.InventoryManagementSystem.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TransactionTimeSeriesHelper {

    private final TransactionRepository transactionRepository;
    private final ProductRepository productRepository;

    public record SevenDayWindow(
            List<BigDecimal> dailySaleSums,
            List<BigDecimal> dailyTotalSums,
            List<Long> dailyTxCounts,
            List<String> dayLabels
    ) {}

    public SevenDayWindow buildSevenDayWindow(LocalDate inclusiveEnd) {
        LocalDate start = inclusiveEnd.minusDays(6);
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endExclusive = inclusiveEnd.plusDays(1).atStartOfDay();

        List<Object[]> rows = transactionRepository.dailyStatsBetween(startDt, endExclusive);
        Map<LocalDate, Object[]> byDate = new HashMap<>();
        for (Object[] row : rows) {
            LocalDate d = ((Date) row[0]).toLocalDate();
            byDate.put(d, row);
        }

        List<BigDecimal> sales = new ArrayList<>();
        List<BigDecimal> totals = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate d = start.plusDays(i);
            Object[] row = byDate.get(d);
            if (row == null) {
                sales.add(BigDecimal.ZERO);
                totals.add(BigDecimal.ZERO);
                counts.add(0L);
            } else {
                sales.add((BigDecimal) row[1]);
                totals.add((BigDecimal) row[2]);
                counts.add(((Number) row[3]).longValue());
            }
            labels.add(d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.US));
        }

        return new SevenDayWindow(sales, totals, counts, labels);
    }

    public List<ChartPointDTO> toBarPoints(SevenDayWindow w) {
        List<ChartPointDTO> out = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            out.add(ChartPointDTO.builder()
                    .name(w.dayLabels().get(i))
                    .value(w.dailyTotalSums().get(i))
                    .build());
        }
        return out;
    }

    public List<ChartPointDTO> toLinePoints(SevenDayWindow w) {
        List<ChartPointDTO> out = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            out.add(ChartPointDTO.builder()
                    .name(w.dayLabels().get(i))
                    .value(w.dailyTotalSums().get(i))
                    .build());
        }
        return out;
    }

    public BigDecimal trendPercent(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0
                    ? BigDecimal.valueOf(100)
                    : BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }

    public List<DashboardSparklineCardDTO> buildDashboardInsightCards(BigDecimal inventoryValue) {
        LocalDate today = LocalDate.now();
        SevenDayWindow cur = buildSevenDayWindow(today);
        SevenDayWindow prev = buildSevenDayWindow(today.minusDays(7));

        BigDecimal curVol = cur.dailyTotalSums().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal prevVol = prev.dailyTotalSums().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal volTrend = trendPercent(curVol, prevVol);

        BigDecimal curSales = cur.dailySaleSums().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal prevSales = prev.dailySaleSums().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saleTrend = trendPercent(curSales, prevSales);

        long curCnt = cur.dailyTxCounts().stream().mapToLong(Long::longValue).sum();
        long prevCnt = prev.dailyTxCounts().stream().mapToLong(Long::longValue).sum();
        BigDecimal cntTrend = trendPercent(
                BigDecimal.valueOf(curCnt),
                BigDecimal.valueOf(prevCnt)
        );

        List<DashboardSparklineCardDTO> list = new ArrayList<>();
        list.add(DashboardSparklineCardDTO.builder()
                .title("Inventory value")
                .badgeText("Stock valuation")
                .primaryValue(inventoryValue == null ? BigDecimal.ZERO : inventoryValue)
                .currency(true)
                .sparkline(cur.dailyTotalSums())
                .trendPercentVsPriorWeek(volTrend)
                .trendPositive(volTrend.compareTo(BigDecimal.ZERO) >= 0)
                .build());

        list.add(DashboardSparklineCardDTO.builder()
                .title("Weekly sales")
                .badgeText("7-day window")
                .primaryValue(curSales)
                .currency(true)
                .sparkline(cur.dailySaleSums())
                .trendPercentVsPriorWeek(saleTrend)
                .trendPositive(saleTrend.compareTo(BigDecimal.ZERO) >= 0)
                .build());

        list.add(DashboardSparklineCardDTO.builder()
                .title("Transactions")
                .badgeText("Activity count")
                .primaryValue(BigDecimal.valueOf(curCnt))
                .currency(false)
                .sparkline(cur.dailyTxCounts().stream().map(BigDecimal::valueOf).toList())
                .trendPercentVsPriorWeek(cntTrend)
                .trendPositive(cntTrend.compareTo(BigDecimal.ZERO) >= 0)
                .build());

        return list;
    }

    public List<DashboardSparklineCardDTO> buildTransactionHubInsightCards() {
        LocalDate today = LocalDate.now();
        SevenDayWindow cur = buildSevenDayWindow(today);
        SevenDayWindow prev = buildSevenDayWindow(today.minusDays(7));

        BigDecimal curVol = cur.dailyTotalSums().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal prevVol = prev.dailyTotalSums().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal volTrend = trendPercent(curVol, prevVol);

        BigDecimal curSales = cur.dailySaleSums().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal prevSales = prev.dailySaleSums().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saleTrend = trendPercent(curSales, prevSales);

        long curCnt = cur.dailyTxCounts().stream().mapToLong(Long::longValue).sum();
        long prevCnt = prev.dailyTxCounts().stream().mapToLong(Long::longValue).sum();
        BigDecimal cntTrend = trendPercent(
                BigDecimal.valueOf(curCnt),
                BigDecimal.valueOf(prevCnt)
        );

        List<DashboardSparklineCardDTO> list = new ArrayList<>();
        list.add(DashboardSparklineCardDTO.builder()
                .title("Flow volume")
                .badgeText("All types · 7d")
                .primaryValue(curVol)
                .currency(true)
                .sparkline(cur.dailyTotalSums())
                .trendPercentVsPriorWeek(volTrend)
                .trendPositive(volTrend.compareTo(BigDecimal.ZERO) >= 0)
                .build());
        list.add(DashboardSparklineCardDTO.builder()
                .title("Sales pulse")
                .badgeText("Checkout")
                .primaryValue(curSales)
                .currency(true)
                .sparkline(cur.dailySaleSums())
                .trendPercentVsPriorWeek(saleTrend)
                .trendPositive(saleTrend.compareTo(BigDecimal.ZERO) >= 0)
                .build());
        list.add(DashboardSparklineCardDTO.builder()
                .title("Events logged")
                .badgeText("Ledger")
                .primaryValue(BigDecimal.valueOf(curCnt))
                .currency(false)
                .sparkline(cur.dailyTxCounts().stream().map(BigDecimal::valueOf).toList())
                .trendPercentVsPriorWeek(cntTrend)
                .trendPositive(cntTrend.compareTo(BigDecimal.ZERO) >= 0)
                .build());
        return list;
    }

    public List<HorizontalMetricDTO> categoryInventoryShareBars(int maxRows) {
        List<Object[]> rows = productRepository.sumInventoryValueByCategory();
        if (rows.isEmpty()) {
            return List.of();
        }
        BigDecimal total = rows.stream()
                .map(r -> (BigDecimal) r[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }
        List<HorizontalMetricDTO> out = new ArrayList<>();
        int idx = 0;
        for (Object[] r : rows) {
            if (out.size() >= maxRows) {
                break;
            }
            String name = (String) r[0];
            BigDecimal amt = (BigDecimal) r[1];
            BigDecimal pct = amt.multiply(BigDecimal.valueOf(100))
                    .divide(total, 1, RoundingMode.HALF_UP);
            out.add(HorizontalMetricDTO.builder()
                    .label(name)
                    .percent(pct)
                    .colorIndex(idx++ % 6)
                    .build());
        }
        return out;
    }

    public List<ChartPointDTO> donutByTypeLast30Days() {
        LocalDateTime since = LocalDate.now().minusDays(30).atStartOfDay();
        List<ChartPointDTO> pts = new ArrayList<>();
        for (Object[] row : transactionRepository.sumByTypeSince(since)) {
            pts.add(ChartPointDTO.builder()
                    .name(((TransactionType) row[0]).name())
                    .value((BigDecimal) row[1])
                    .build());
        }
        return pts;
    }

    public List<TopProductRowDTO> topProductRows(int maxRows) {
        LocalDateTime since = LocalDate.now().minusDays(30).atStartOfDay();
        List<Object[]> rows = transactionRepository.topProductVolumeSince(since);
        List<TopProductRowDTO> out = new ArrayList<>();
        for (Object[] r : rows) {
            if (out.size() >= maxRows) {
                break;
            }
            out.add(TopProductRowDTO.builder()
                    .name((String) r[0])
                    .volumeLast30Days((BigDecimal) r[1])
                    .build());
        }
        return out;
    }

    public List<HorizontalMetricDTO> topProductHorizontalBars(int maxRows) {
        LocalDateTime since = LocalDate.now().minusDays(30).atStartOfDay();
        List<Object[]> rows = transactionRepository.topProductVolumeSince(since);
        if (rows.isEmpty()) {
            return List.of();
        }
        BigDecimal top = (BigDecimal) rows.get(0)[1];
        if (top.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }
        List<HorizontalMetricDTO> out = new ArrayList<>();
        int idx = 0;
        for (Object[] r : rows) {
            if (out.size() >= maxRows) {
                break;
            }
            String name = (String) r[0];
            BigDecimal amt = (BigDecimal) r[1];
            BigDecimal pct = amt.multiply(BigDecimal.valueOf(100))
                    .divide(top, 1, RoundingMode.HALF_UP)
                    .min(BigDecimal.valueOf(100));
            out.add(HorizontalMetricDTO.builder()
                    .label(name.length() > 28 ? name.substring(0, 25) + "…" : name)
                    .percent(pct)
                    .colorIndex(idx++ % 6)
                    .build());
        }
        return out;
    }
}
