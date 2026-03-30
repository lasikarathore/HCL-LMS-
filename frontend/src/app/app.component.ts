import { CommonModule } from '@angular/common';
import {
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
} from '@angular/core';
import {
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from '@angular/router';
import { filter } from 'rxjs/operators';
import { Subscription } from 'rxjs';
import { ApiService } from './service/api.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'ims';
  shellVisible = true;
  lowStockCount: number = 0;
  activeAlertCount: number = 0;
  private sub?: Subscription;
  private alertPollHandle?: ReturnType<typeof setInterval>;

  constructor(
    private apiService: ApiService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.sub = this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd))
      .subscribe(() => this.refreshShell());
    this.refreshShell();
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.stopAlertPolling();
  }

  private startAlertPolling(): void {
    this.stopAlertPolling();
    const tick = () => {
      this.apiService.getActiveAlertsCount().subscribe({
        next: (res: any) => {
          this.activeAlertCount = Number(res?.alertCount ?? 0) || 0;
          this.cdr.detectChanges();
        },
        error: () => {
          // Keep silent during polling.
        },
      });
    };

    tick();
    this.alertPollHandle = setInterval(tick, 10000);
  }

  private stopAlertPolling(): void {
    if (this.alertPollHandle) {
      clearInterval(this.alertPollHandle);
      this.alertPollHandle = undefined;
    }
  }

  private refreshShell(): void {
    const path = this.router.url.split('?')[0];
    this.shellVisible = !(
      path === '/login' ||
      path === '/register' ||
      !this.apiService.isAuthenticated()
    );

    if (this.shellVisible) {
      this.apiService.getDashboardSummary().subscribe({
        next: (res) => {
          this.lowStockCount = res.dashboardSummary?.lowStockProductCount || 0;
          this.cdr.detectChanges();
        },
        error: (err) => console.error('Error fetching dashboard summary:', err),
      });
      this.startAlertPolling();
    } else {
      this.activeAlertCount = 0;
      this.stopAlertPolling();
    }

    this.cdr.detectChanges();
  }

  isAuth(): boolean {
    return this.apiService.isAuthenticated();
  }

  isAdmin(): boolean {
    return this.apiService.isAdmin();
  }

  logOut(): void {
    this.apiService.logout();
    this.router.navigate(['/login']);
    this.refreshShell();
    this.cdr.detectChanges();
  }
}
