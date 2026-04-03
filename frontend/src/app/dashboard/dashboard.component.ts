import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { ApiService } from '../service/api.service';
import { InsightCardRowComponent } from '../shared/insight-card-row/insight-card-row.component';
import { IMS_CHART_SCHEME } from '../theme/ims-chart-scheme';
import { LegendPosition, ScaleType } from '@swimlane/ngx-charts';
import { HasRoleDirective } from '../shared/directives/has-role.directive';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, NgxChartsModule, InsightCardRowComponent, HasRoleDirective],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent implements OnInit {
  user: any = null;
  summary: any = null;
  message = '';

  get role(): string | null {
    return this.apiService.getRole();
  }

  mainTab: 'bar' | 'line' = 'bar';

  chartScheme = IMS_CHART_SCHEME;
  schemeType = ScaleType.Ordinal;
  mainChartView: [number, number] = [720, 360];
  donutView: [number, number] = [250, 300];
  donutLegendPosition = LegendPosition.Below;

  showLegend = true;
  showLabels = false;
  animations = true;

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadUser();
    this.loadSummary();
  }

  loadUser(): void {
    this.apiService.getLoggedInUserInfo().subscribe({
      next: (u) => (this.user = u),
      error: () => {},
    });
  }

  loadSummary(): void {
    this.apiService.getDashboardSummary().subscribe({
      next: (res: any) => {
        const ok = res?.status === 200 || res?.status === '200';
        this.summary = ok ? res?.dashboardSummary ?? null : null;
      },
      error: (err) =>
        this.showMsg(
          err?.error?.message ||
            err?.message ||
            'Unable to load dashboard summary'
        ),
    });
  }

  points(rows: any[] | undefined | null): { name: string; value: number }[] {
    return (rows || []).map((r: any) => ({
      name: r.name,
      value: Number(r.value),
    }));
  }

  lineChartResults(rows: any[] | undefined | null): { name: string; series: { name: string; value: number }[] }[] {
    const pts = this.points(rows);
    if (!pts.length) return [];
    return [{ name: 'Volume', series: pts }];
  }

  hbarClass(i: number): string {
    return 'hx' + (i % 6);
  }

  formatMoney(v: number | string | undefined): string {
    if (v === undefined || v === null) return '—';
    const n = typeof v === 'string' ? Number(v) : v;
    if (Number.isNaN(n)) return '—';
    return n.toLocaleString('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 2,
    });
  }

  showMsg(m: string): void {
    this.message = m;
    setTimeout(() => (this.message = ''), 5000);
  }
}
