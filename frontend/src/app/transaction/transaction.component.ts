import { Component, OnInit } from '@angular/core';
import { PaginationComponent } from '../pagination/pagination.component';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { LegendPosition, ScaleType } from '@swimlane/ngx-charts';
import { ApiService } from '../service/api.service';
import { Router } from '@angular/router';
import { InsightCardRowComponent } from '../shared/insight-card-row/insight-card-row.component';
import { IMS_CHART_SCHEME } from '../theme/ims-chart-scheme';

@Component({
  selector: 'app-transaction',
  standalone: true,
  imports: [
    PaginationComponent,
    FormsModule,
    CommonModule,
    NgxChartsModule,
    InsightCardRowComponent,
  ],
  templateUrl: './transaction.component.html',
  styleUrl: './transaction.component.css',
})
export class TransactionComponent implements OnInit {
  constructor(
    private apiService: ApiService,
    private router: Router
  ) {}

  user: any = null;
  transactions: any[] = [];
  analytics: any = null;
  message = '';

  searchInput = '';
  valueToSearch = '';
  currentPage = 1;
  totalPages = 0;
  itemsPerPage = 10;

  chartScheme = IMS_CHART_SCHEME;
  schemeType = ScaleType.Ordinal;
  lineView: [number, number] = [720, 360];
  donutView: [number, number] = [250, 300];
  donutLegendPosition = LegendPosition.Below;
  dailyView: [number, number] = [700, 220];
  monthlyView: [number, number] = [920, 400];
  monthlyTrendView: [number, number] = [920, 300];
  showLegend = true;
  showLabels = false;
  animations = true;

  months = [
    { name: 'January', value: '1' },
    { name: 'February', value: '2' },
    { name: 'March', value: '3' },
    { name: 'April', value: '4' },
    { name: 'May', value: '5' },
    { name: 'June', value: '6' },
    { name: 'July', value: '7' },
    { name: 'August', value: '8' },
    { name: 'September', value: '9' },
    { name: 'October', value: '10' },
    { name: 'November', value: '11' },
    { name: 'December', value: '12' },
  ];
  years = Array.from({ length: 10 }, (_, i) => new Date().getFullYear() - i);
  selectedMonth = String(new Date().getMonth() + 1);
  selectedYear = String(new Date().getFullYear());

  ngOnInit(): void {
    this.apiService.getLoggedInUserInfo().subscribe({
      next: (u) => (this.user = u),
      error: () => {},
    });
    this.loadAnalytics();
    this.loadTransactions();
  }

  loadAnalytics(): void {
    const m = Number.parseInt(this.selectedMonth, 10);
    const y = Number.parseInt(this.selectedYear, 10);
    this.apiService.getTransactionAnalytics(m, y).subscribe({
      next: (res: any) => {
        const ok = res?.status === 200 || res?.status === '200';
        this.analytics = ok ? res?.transactionAnalytics ?? null : null;
      },
      error: (error) => {
        this.showMessage(
          error?.error?.message ||
            error?.message ||
            'Unable to load analytics ' + error
        );
      },
    });
  }

  loadTransactions(): void {
    this.apiService
      .getAllTransactions(this.valueToSearch, this.currentPage - 1, this.itemsPerPage)
      .subscribe({
        next: (res: any) => {
          this.transactions = res.transactions || [];
          const tp = res.totalPages ?? 0;
          this.totalPages = Math.max(1, tp);
        },
        error: (error) => {
          this.showMessage(
            error?.error?.message ||
              error?.message ||
              'Unable to get transactions ' + error
          );
        },
      });
  }

  points(rows: any[] | undefined | null): { name: string; value: number }[] {
    return (rows || []).map((r: any) => ({
      name: r.name,
      value: Number(r.value),
    }));
  }

  /** ngx-charts line chart expects one group with a `series` array, not flat points. */
  lineChartResults(rows: any[] | undefined | null): { name: string; series: { name: string; value: number }[] }[] {
    const pts = this.points(rows);
    if (!pts.length) return [];
    return [{ name: 'Volume', series: pts }];
  }

  /** ngx-charts bar-vertical-2d: one entry per month, inner series = transaction types. */
  monthlyGroupedResults(
    groups: any[] | null | undefined
  ): { name: string; series: { name: string; value: number }[] }[] {
    if (!groups?.length) return [];
    return groups.map((g: any) => ({
      name: g.name ?? '',
      series: (g.series || []).map((s: any) => ({
        name: s.name ?? '',
        value: Number(s.value),
      })),
    }));
  }

  hbarClass(i: number): string {
    return 'hx' + (i % 6);
  }

  handleSearch(): void {
    this.currentPage = 1;
    this.valueToSearch = this.searchInput;
    this.loadTransactions();
  }

  applyChartMonth(): void {
    this.loadAnalytics();
  }

  navigateTOTransactionsDetailsPage(transactionId: string): void {
    this.router.navigate([`/transaction/${transactionId}`]);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadTransactions();
  }

  showMessage(message: string) {
    this.message = message;
    setTimeout(() => {
      this.message = '';
    }, 4000);
  }
}
