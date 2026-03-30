import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../service/api.service';

@Component({
  selector: 'app-stock-alerts',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './stock-alerts.component.html',
  styleUrl: './stock-alerts.component.css',
})
export class StockAlertsComponent implements OnInit {
  message = '';
  alerts: any[] = [];

  // Alert thresholds:
  // Critical (<5), Low (<10), Watch (<20)
  criticalThreshold = 5;
  lowThreshold = 10;
  watchThreshold = 20;

  // Filters / UI state
  filterStatus: 'All' | 'Critical' | 'Low' | 'Watch' = 'All';
  autoNotifyEnabled = true;

  // Summary counts
  totalProducts = 0;
  healthyStockCount = 0;
  allProducts: any[] = [];

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadAlerts();
    this.loadAllProductsForSummary();
  }

  private loadAlerts(): void {
    this.apiService.getActiveAlerts().subscribe({
      next: (res: any) => {
        const ok = res?.status === 200 || res?.status === '200';
        const raw = ok ? res?.alerts || [] : [];
        this.alerts = this.dedupeAlerts(raw);
        if (!ok && res?.message) this.showMessage(res.message);
      },
      error: (err) => {
        this.showMessage(
          err?.error?.message ||
            err?.message ||
            'Unable to load stock alerts'
        );
      },
    });
  }

  private severityRank(status: string): number {
    switch (status) {
      case 'Critical':
        return 0;
      case 'Low':
        return 1;
      case 'Watch':
        return 2;
      default:
        return 3;
    }
  }

  /**
   * Ensure only ONE active alert per product is shown on UI.
   * If duplicates exist, keep the most severe one (Critical > Low > Watch).
   */
  private dedupeAlerts(rawAlerts: any[]): any[] {
    const bestByProductId = new Map<number, any>();
    for (const a of rawAlerts || []) {
      const pid = Number(a?.productId);
      if (!pid || Number.isNaN(pid)) continue;

      const existing = bestByProductId.get(pid);
      if (!existing) {
        bestByProductId.set(pid, a);
        continue;
      }

      const aRank = this.severityRank(a?.status);
      const eRank = this.severityRank(existing?.status);

      if (aRank < eRank) {
        bestByProductId.set(pid, a);
        continue;
      }

      // Same severity: keep the one with the lower current stock (more urgent).
      if (aRank === eRank) {
        const aStock = Number(a?.currentStock);
        const eStock = Number(existing?.currentStock);
        if (!Number.isNaN(aStock) && !Number.isNaN(eStock) && aStock < eStock) {
          bestByProductId.set(pid, a);
        }
      }
    }

    // Sort: Critical -> Low -> Watch, then lower stock first.
    return Array.from(bestByProductId.values()).sort((x, y) => {
      const xr = this.severityRank(x?.status);
      const yr = this.severityRank(y?.status);
      if (xr !== yr) return xr - yr;
      const xs = Number(x?.currentStock);
      const ys = Number(y?.currentStock);
      if (Number.isNaN(xs) || Number.isNaN(ys)) return 0;
      return xs - ys;
    });
  }

  private loadAllProductsForSummary(): void {
    this.apiService.getAllProducts().subscribe({
      next: (res: any) => {
        const ok = res?.status === 200 || res?.status === '200';
        this.allProducts = ok ? res?.products || [] : [];
        this.totalProducts = this.allProducts.length;
        this.healthyStockCount = this.allProducts.filter((p: any) => {
          const q = Number(p?.stockQuantity);
          return !Number.isNaN(q) && q >= this.watchThreshold;
        }).length;
      },
      error: () => {
        // Summary is optional; keep page functional if this fails.
      },
    });
  }

  private showMessage(message: string) {
    this.message = message;
    setTimeout(() => {
      this.message = '';
    }, 4000);
  }

  stockPercent(stock: number, threshold: number): number {
    const s = Number(stock);
    const t = Number(threshold);
    if (Number.isNaN(s) || Number.isNaN(t) || t <= 0) return 0;
    const pct = (s / t) * 100;
    return Math.max(0, Math.min(100, pct));
  }

  statusPillClass(status: string): string {
    switch (status) {
      case 'Critical':
        return 'pill-critical';
      case 'Low':
        return 'pill-low';
      default:
        return 'pill-watch';
    }
  }

  statusBarClass(status: string): string {
    switch (status) {
      case 'Critical':
        return 'bar-critical';
      case 'Low':
        return 'bar-low';
      default:
        return 'bar-watch';
    }
  }

  filteredAlerts(): any[] {
    if (this.filterStatus === 'All') return this.alerts;
    return this.alerts.filter((a) => a?.status === this.filterStatus);
  }

  countBy(status: 'Critical' | 'Low' | 'Watch'): number {
    return this.alerts.filter((a) => a?.status === status).length;
  }

  getActiveBelowWatchCountLabel(): string {
    const active = this.alerts.length;
    const total = this.totalProducts || 0;
    return `${active}/${total}`;
  }
}

