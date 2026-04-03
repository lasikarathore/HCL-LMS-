import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { LegendPosition, ScaleType } from '@swimlane/ngx-charts';
import { ApiService } from '../service/api.service';
import { IMS_CHART_SCHEME } from '../theme/ims-chart-scheme';

type RangeOpt = 'THIS_MONTH' | 'LAST_MONTH' | 'THIS_QUARTER' | 'THIS_YEAR';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule, NgxChartsModule],
  templateUrl: './analytics.component.html',
  styleUrl: './analytics.component.css',
})
export class AnalyticsComponent implements OnInit {
  message = '';
  range: RangeOpt = 'THIS_MONTH';

  loading = false;
  summary: any = null;

  chartScheme = IMS_CHART_SCHEME;
  schemeType = ScaleType.Ordinal;
  barView: [number, number] = [920, 340];
  legendPosition = LegendPosition.Below;

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loading = true;
    this.api.getAnalyticsSummary(this.range).subscribe({
      next: (res: any) => {
        this.loading = false;
        const ok = res?.status === 200 || res?.status === '200';
        this.summary = ok ? res : null;
      },
      error: () => {
        this.loading = false;
        this.summary = null;
      },
    });
  }

  exportCsv(): void {
    // Simple client-side export for turnover table
    const rows = this.summary?.rows || [];
    const header = [
      'Product',
      'UnitsSold',
      'UnitsPurchased',
      'ClosingStock',
      'TurnoverRate',
    ];
    const lines = [header.join(',')].concat(
      rows.map((r: any) =>
        [
          JSON.stringify(r.product || ''),
          r.unitsSold ?? 0,
          r.unitsPurchased ?? 0,
          r.closingStock ?? 0,
          r.turnoverRate ?? 0,
        ].join(',')
      )
    );
    const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'analytics.csv';
    a.click();
    URL.revokeObjectURL(a.href);
  }

  exportPdf(): void {
    // Placeholder: export can be added later (server-side)
    this.message = 'PDF export coming next (CSV works now).';
    setTimeout(() => (this.message = ''), 4000);
  }

  points(rows: any[]): { name: string; value: number }[] {
    return (rows || []).map((r: any) => ({ name: r.name, value: Number(r.value) }));
  }

  groupedTrend(): { name: string; series: { name: string; value: number }[] }[] {
    return (this.summary?.trend || []).map((m: any) => ({
      name: m.name,
      series: [
        { name: 'Sales', value: Number(m.sales || 0) },
        { name: 'Purchases', value: Number(m.purchases || 0) },
      ],
    }));
  }
}

