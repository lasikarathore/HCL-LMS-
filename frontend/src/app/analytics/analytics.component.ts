import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { LegendPosition, ScaleType } from '@swimlane/ngx-charts';
import { ApiService } from '../service/api.service';
import { IMS_CHART_SCHEME } from '../theme/ims-chart-scheme';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

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
    if (!this.summary) {
      this.message = 'No data to export';
      return;
    }

    const doc = new jsPDF();
    const pageWidth = doc.internal.pageSize.getWidth();

    // 1. Header
    doc.setFontSize(22);
    doc.setTextColor(40, 44, 52);
    doc.text('IMS Analytics Report', 14, 22);

    doc.setFontSize(10);
    doc.setTextColor(100);
    const dateStr = new Date().toLocaleString();
    doc.text(`Generated on: ${dateStr}`, 14, 28);
    doc.text(`Range: ${this.range.replace('_', ' ')}`, 14, 33);

    // 2. KPI Summary
    doc.setFontSize(14);
    doc.setTextColor(40, 44, 52);
    doc.text('Performance Summary', 14, 45);

    autoTable(doc, {
      startY: 50,
      head: [['Metric', 'Value']],
      body: [
        ['Total Revenue', this.formatCurrency(this.summary.revenue)],
        ['Gross Profit', this.formatCurrency(this.summary.grossProfit)],
        ['Profit Margin', `${this.summary.grossMargin}%`],
        ['Inventory Value', this.formatCurrency(this.summary.inventoryValue)],
        ['Turnover Rate', `${this.summary.turnoverRate}x (Target: ${this.summary.industryAvg}x)`],
      ],
      theme: 'striped',
      headStyles: { fillColor: [99, 102, 241] },
    });

    // 3. Category Breakdown
    const finalY = (doc as any).lastAutoTable.finalY + 10;
    doc.text('Category Performance', 14, finalY);

    autoTable(doc, {
      startY: finalY + 5,
      head: [['Category', 'Revenue', 'Share %']],
      body: this.summary.analyticsCategories.map((c: any) => [
        c.category,
        this.formatCurrency(c.revenue),
        `${c.percent}%`,
      ]),
      theme: 'grid',
      headStyles: { fillColor: [79, 70, 229] },
    });

    // 4. Inventory Turnover Report
    doc.addPage();
    doc.text('Detailed Inventory Turnover Report', 14, 22);

    autoTable(doc, {
      startY: 28,
      head: [['Product', 'Units Sold', 'Units Purchased', 'Closing Stock', 'Turnover']],
      body: this.summary.rows.map((r: any) => [
        r.product,
        r.unitsSold,
        r.unitsPurchased,
        r.closingStock,
        `${r.turnoverRate}x`,
      ]),
      headStyles: { fillColor: [99, 102, 241] },
    });

    // 5. Insights
    if (this.summary.insight) {
      const insightY = (doc as any).lastAutoTable.finalY + 15;
      doc.setFontSize(12);
      doc.text('System Insights', 14, insightY);
      doc.setFontSize(10);
      doc.setTextColor(60);
      const splitInsight = doc.splitTextToSize(this.summary.insight, pageWidth - 28);
      doc.text(splitInsight, 14, insightY + 7);
    }

    doc.save(`IMS_Analytics_${this.range}_${new Date().toISOString().slice(0, 10)}.pdf`);
    this.message = 'PDF report generated successfully!';
    setTimeout(() => (this.message = ''), 4000);
  }

  private formatCurrency(val: number): string {
    return 'INR ' + new Intl.NumberFormat('en-IN').format(val || 0);
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

