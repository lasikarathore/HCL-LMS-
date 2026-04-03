import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../service/api.service';

@Component({
  selector: 'app-sell',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './sell.component.html',
  styleUrl: './sell.component.css',
})
export class SellComponent implements OnInit {
  constructor(private apiService: ApiService) {}

  products: any[] = [];
  productId = '';
  description = '';
  quantity = '';
  message = '';
  recentSales: any[] = [];
  isStockInsufficient = false;
  isOutOfStock = false;
  isLowStock = false;
  availableStock = 0;

  ngOnInit(): void {
    this.fetchProducts();
    this.loadRecentSales();
  }

  get lineTotal(): number {
    const p = this.products.find(
      (x) => String(x.id) === String(this.productId)
    );
    const q = parseInt(this.quantity, 10);
    
    if (p) {
      this.availableStock = p.stockQuantity;
      this.isStockInsufficient = q > p.stockQuantity;
      this.isOutOfStock = p.stockQuantity === 0;
      this.isLowStock = p.stockQuantity > 0 && p.stockQuantity < 5;
    } else {
      this.isStockInsufficient = false;
      this.isOutOfStock = false;
      this.isLowStock = false;
    }

    if (!p || Number.isNaN(q) || q < 1) {
      return 0;
    }
    return Number(p.price) * q;
  }

  onProductChange(): void {
    const p = this.products.find(
      (x) => String(x.id) === String(this.productId)
    );
    if (p) {
      this.availableStock = p.stockQuantity;
      this.isOutOfStock = p.stockQuantity === 0;
      this.isLowStock = p.stockQuantity > 0 && p.stockQuantity < 5;
      if (this.isOutOfStock) {
        this.quantity = '';
      }
    } else {
      this.isOutOfStock = false;
      this.isLowStock = false;
    }
  }

  loadRecentSales(): void {
    this.apiService.getAllTransactions('', 0, 10, 'SALE').subscribe({
      next: (res: any) => {
        const ok = res?.status === 200 || res?.status === '200';
        this.recentSales = ok ? res.transactions || [] : [];
        if (!ok && res?.message) {
          this.showMessage(res.message);
        }
      },
      error: (err) => {
        this.showMessage(
          err?.error?.message ||
            err?.message ||
            'Could not load recent sales. Check that the API is running and restart the backend if you still see database errors.'
        );
      },
    });
  }

  fetchProducts(): void {
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
  }

  handleSubmit(): void {
    if (!this.productId || !this.quantity) {
      this.showMessage('Please fill all required fields');
      return;
    }
    const body = {
      productId: this.productId,
      quantity: parseInt(this.quantity, 10),
      description: this.description,
    };

    this.apiService.sellProduct(body).subscribe({
      next: (res: any) => {
        if (res.status === 200) {
          this.showMessage(res.message);
          this.resetForm();
          this.fetchProducts();
          this.loadRecentSales();
        }
      },
      error: (error) => {
        this.showMessage(
          error?.error?.message ||
            error?.message ||
            'Unable to complete sale ' + error
        );
      },
    });
  }

  resetForm(): void {
    this.productId = '';
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
