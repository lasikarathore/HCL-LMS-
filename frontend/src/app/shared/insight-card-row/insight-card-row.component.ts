import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { Color, ScaleType } from '@swimlane/ngx-charts';
import { INSIGHT_SPARK_SCHEMES } from '../../theme/insight-spark-schemes';

export interface InsightCardVm {
  title?: string;
  badgeText?: string;
  primaryValue?: unknown;
  currency?: boolean;
  trendPercentVsPriorWeek?: unknown;
  trendPositive?: boolean;
  sparkline?: unknown[];
}

@Component({
  selector: 'app-insight-card-row',
  standalone: true,
  imports: [CommonModule, NgxChartsModule],
  templateUrl: './insight-card-row.component.html',
  styleUrl: './insight-card-row.component.css',
})
export class InsightCardRowComponent {
  @Input() cards: InsightCardVm[] | null = null;

  sparkView: [number, number] = [148, 44];
  schemeType = ScaleType.Ordinal;
  animations = false;

  schemeAt(i: number): Color {
    return INSIGHT_SPARK_SCHEMES[i % INSIGHT_SPARK_SCHEMES.length];
  }

  cardClass(i: number): string {
    return 'tone-' + (i % 3);
  }

  sparkSeries(card: InsightCardVm): { name: string; value: number }[] {
    const pts = (card.sparkline || []) as unknown[];
    return pts.map((v, idx) => ({
      name: String(idx),
      value: Number(v),
    }));
  }

  sparkLineChartResults(card: InsightCardVm): { name: string; series: { name: string; value: number }[] }[] {
    const series = this.sparkSeries(card);
    if (!series.length) return [];
    return [{ name: 'spark', series }];
  }

  formatPrimary(card: InsightCardVm): string {
    const v = Number(card.primaryValue);
    if (Number.isNaN(v)) return '—';
    if (card.currency) {
      return v.toLocaleString('en-IN', {
        style: 'currency',
        currency: 'INR',
        maximumFractionDigits: 2,
      });
    }
    return String(Math.round(v));
  }

  trendText(card: InsightCardVm): string {
    const p = Number(card.trendPercentVsPriorWeek);
    if (Number.isNaN(p)) return '';
    const sign = p >= 0 ? '▲' : '▼';
    return `${sign} ${Math.abs(p).toFixed(1)}% vs prior week`;
  }
}
