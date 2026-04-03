import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../service/api.service';

type PoStatusTab = 'ALL' | 'PENDING' | 'APPROVED' | 'RECEIVED';

@Component({
  selector: 'app-purchase-orders',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './purchase-orders.component.html',
  styleUrl: './purchase-orders.component.css',
})
export class PurchaseOrdersComponent implements OnInit {
  message = '';

  tab: PoStatusTab = 'ALL';
  summary: any = null;
  rows: any[] = [];

  suppliers: any[] = [];
  products: any[] = [];

  creating = false;
  form: any = {
    supplierId: '',
    requiredByDate: '',
    expectedDeliveryDate: '',
    productId: '',
    quantity: 1,
    unitPrice: 0,
    priority: 'Normal',
    notes: '',
  };

  constructor(
    public apiService: ApiService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  get isAdmin(): boolean {
    return this.apiService.isAdmin();
  }

  get isWarehouseManager(): boolean {
    return this.apiService.isWarehouseManager();
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe((p) => {
      const pid = p['product_id'];
      if (pid != null && pid !== '') this.form.productId = String(pid);
    });
    this.loadSummary();
    this.loadSuppliersProducts();
    this.loadList();
  }

  loadSummary(): void {
    this.apiService.getPurchaseOrderSummary().subscribe({
      next: (res: any) => (this.summary = res?.status === 200 ? res : null),
      error: () => {},
    });
  }

  loadSuppliersProducts(): void {
    this.apiService.getAllSuppliers().subscribe({
      next: (res: any) => {
        const all = res?.suppliers || [];
        this.suppliers = all.filter((s: any) => s.status === 'ACTIVE');
      },
      error: () => {},
    });
    this.apiService.getAllProducts().subscribe({
      next: (res: any) => (this.products = res?.products || []),
      error: () => {},
    });
  }

  loadList(): void {
    this.apiService.getPurchaseOrders(this.tab).subscribe({
      next: (res: any) => {
        const ok = res?.status === 200 || res?.status === '200';
        this.rows = ok ? res?.purchaseOrders || [] : [];
      },
      error: (err) =>
        this.showMessage(
          err?.error?.message || err?.message || 'Unable to load purchase orders'
        ),
    });
  }

  setTab(t: PoStatusTab): void {
    this.tab = t;
    this.loadList();
  }

  get totalValue(): number {
    const q = Number(this.form.quantity);
    const u = Number(this.form.unitPrice);
    if (Number.isNaN(q) || Number.isNaN(u)) return 0;
    return q * u;
  }

  submitPo(): void {
    if (!this.form.supplierId || !this.form.productId || !this.form.quantity) {
      this.showMessage('Please fill supplier, product, and quantity');
      return;
    }
    const body = {
      supplierId: Number(this.form.supplierId),
      requiredByDate: this.form.requiredByDate || null,
      expectedDeliveryDate: this.form.expectedDeliveryDate || null,
      priority: this.form.priority || 'Normal',
      notes: this.form.notes || null,
      productId: Number(this.form.productId),
      quantity: Number(this.form.quantity),
      unitPrice: Number(this.form.unitPrice),
    };
    this.apiService.createPurchaseOrder(body).subscribe({
      next: (res: any) => {
        if (res?.status === 200) {
          this.showMessage('Submitted for approval');
          this.creating = false;
          this.loadSummary();
          this.loadList();
        } else {
          this.showMessage(res?.message || 'Unable to create PO');
        }
      },
      error: (err) =>
        this.showMessage(
          err?.error?.message || err?.message || 'Unable to create PO'
        ),
    });
  }

  approve(id: any): void {
    this.apiService.approvePurchaseOrder(String(id)).subscribe({
      next: () => {
        this.loadSummary();
        this.loadList();
      },
      error: () => this.showMessage('Unable to approve PO'),
    });
  }

  receive(po: any): void {
    const id = po?.id;
    if (!id) return;
    // Navigate to Receive Stock page with PO id pre-selected.
    this.router.navigate(['/purchase'], { queryParams: { po_id: id } });
  }

  view(po: any): void {
    alert(JSON.stringify(po, null, 2));
  }

  showMessage(m: string): void {
    this.message = m;
    setTimeout(() => (this.message = ''), 4000);
  }
}
