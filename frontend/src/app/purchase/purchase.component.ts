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
  approvedPOs: any[] = []; // NEW: for PO selection
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

  onPoSelect(id: string) {
    if (!id) {
      this.resetForm();
      return;
    }
    const po = this.approvedPOs.find(p => String(p.id) === id);
    if (po) {
      this.productId = String(po.supplierId == null ? '' : po.items[0]?.productId || '');
      this.supplierId = String(po.supplierId || '');
      this.quantity = String(po.items[0]?.quantity || '');
      this.description = `PO Receipt: ${po.poNumber}`;
    }
  }

  loadRecentPurchases(): void {
    this.apiService.getAllTransactions('', 0, 10, 'PURCHASE', 'COMPLETED').subscribe({
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
          const all = res.suppliers || [];
          this.suppliers = all.filter((s: any) => s.active !== false);
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

    this.apiService.getPurchaseOrders('APPROVED').subscribe({
      next: (res: any) => {
        if (res.status === 200) {
          this.approvedPOs = res.purchaseOrders || [];
        }
      },
      error: () => {}
    });
  }

  handleSubmit(): void {
    // 1. If we have a PO ID, we follow the "Receive PO" workflow
    if (this.poId) {
      this.apiService.receivePurchaseOrder(this.poId).subscribe({
        next: (res: any) => {
          if (res?.status === 200) {
            this.showMessage("PO Received successfully, stock updated");
            this.resetForm();
            this.loadRecentPurchases();
          } else {
            this.showMessage(res?.message || "Unable to receive PO");
          }
        },
        error: (err) => this.showMessage(err?.error?.message || err?.message || "Error receiving PO")
      });
      return;
    }

    // 2. Otherwise, follow the "Manual Stock Inward" workflow
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
          this.resetForm();
          this.loadRecentPurchases();
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
