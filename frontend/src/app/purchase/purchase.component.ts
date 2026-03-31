import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../service/api.service';

@Component({
  selector: 'app-purchase',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './purchase.component.html',
  styleUrl: './purchase.component.css',
})
export class PurchaseComponent implements OnInit {
  constructor(
    private apiService: ApiService,
    private route: ActivatedRoute
  ) {}

  products: any[] = [];
  suppliers: any[] = [];
  productId = '';
  supplierId = '';
  poId = '';
  description = '';
  quantity = '';
  message = '';
  recentPurchases: any[] = [];

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      const pid = params['product_id'];
      if (pid != null && pid !== '') {
        this.productId = String(pid);
      }
      const sid = params['supplier_id'];
      if (sid != null && sid !== '') {
        this.supplierId = String(sid);
      }
      const poid = params['po_id'];
      if (poid != null && poid !== '') {
        this.poId = String(poid);
      }
      const qty = params['quantity'];
      if (qty != null && qty !== '') {
        this.quantity = String(qty);
      }
      const desc = params['description'];
      if (desc != null && desc !== '') {
        this.description = String(desc);
      }
    });
    this.fetchProductsAndSuppliers();
    this.loadRecentPurchases();
  }

  loadRecentPurchases(): void {
    this.apiService.getAllTransactions('', 0, 10, 'PURCHASE').subscribe({
      next: (res: any) => {
        const ok = res?.status === 200 || res?.status === '200';
        this.recentPurchases = ok ? res.transactions || [] : [];
        if (!ok && res?.message) {
          this.showMessage(res.message);
        }
      },
      error: (err) => {
        this.showMessage(
          err?.error?.message ||
            err?.message ||
            'Could not load recent purchases. Check that the API is running and restart the backend after updating (fixes transaction search SQL).'
        );
      },
    });
  }

  fetchProductsAndSuppliers(): void {
    this.apiService.getAllProducts().subscribe({
      next: (res: any) => {
        if (res.status === 200) {
          this.products = res.products || [];
        }
      },
      error: (error) => {
        this.showMessage(
          error?.error?.message ||
            error?.message ||
            'Unable to get products ' + error
        );
      },
    });

    this.apiService.getAllSuppliers().subscribe({
      next: (res: any) => {
        if (res.status === 200) {
          this.suppliers = res.suppliers || [];
        }
      },
      error: (error) => {
        this.showMessage(
          error?.error?.message ||
            error?.message ||
            'Unable to get suppliers ' + error
        );
      },
    });
  }

  handleSubmit(): void {
    if (!this.productId || !this.supplierId || !this.quantity) {
      this.showMessage('Please fill all required fields');
      return;
    }
    const body = {
      productId: this.productId,
      supplierId: this.supplierId,
      quantity: parseInt(this.quantity, 10),
      description: this.description,
    };

    this.apiService.purchaseProduct(body).subscribe({
      next: (res: any) => {
        if (res.status === 200) {
          this.showMessage(res.message);
          // If this purchase is completing a PO receive flow, mark the PO as RECEIVED.
          const maybePo = this.poId;
          this.resetForm();
          this.loadRecentPurchases();

          if (maybePo) {
            this.apiService.receivePurchaseOrder(maybePo).subscribe({
              next: () => {},
              error: () => {},
            });
          }
        }
      },
      error: (error) => {
        this.showMessage(
          error?.error?.message ||
            error?.message ||
            'Unable to complete purchase ' + error
        );
      },
    });
  }

  resetForm(): void {
    this.productId = '';
    this.supplierId = '';
    this.poId = '';
    this.description = '';
    this.quantity = '';
  }

  showMessage(message: string) {
    this.message = message;
    setTimeout(() => {
      this.message = '';
    }, 4000);
  }
}
