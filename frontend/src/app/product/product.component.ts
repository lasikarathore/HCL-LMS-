import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { PaginationComponent } from '../pagination/pagination.component';
import { ApiService } from '../service/api.service';
import { Router } from '@angular/router';
import { HasRoleDirective } from '../shared/directives/has-role.directive';

@Component({
  selector: 'app-product',
  standalone: true,
  imports: [CommonModule, PaginationComponent, HasRoleDirective],
  templateUrl: './product.component.html',
  styleUrl: './product.component.css',
})
export class ProductComponent implements OnInit {
  constructor(
    private apiService: ApiService,
    private router: Router
  ) {}
  products: any[] = [];
  allProducts: any[] = [];
  message: string = '';
  currentPage: number = 1;
  totalPages: number = 0;
  itemsPerPage: number = 12;

  ngOnInit(): void {
    this.fetchProducts();
  }

  fetchProducts(): void {
    this.apiService.getAllProducts().subscribe({
      next: (res: any) => {
        const raw = res.products || [];
        this.allProducts = raw.map((p: any) => this.normalizeProduct(p));
        this.totalPages = Math.max(
          1,
          Math.ceil(this.allProducts.length / this.itemsPerPage)
        );
        const start = (this.currentPage - 1) * this.itemsPerPage;
        this.products = this.allProducts.slice(
          start,
          start + this.itemsPerPage
        );
      },
      error: (error) => {
        this.showMessage(
          error?.error?.message ||
            error?.message ||
            'Unable to load products ' + error
        );
      },
    });
  }

  private normalizeProduct(p: any): any {
    const img = ApiService.resolvePublicAsset(p.imageUrl);
    if (img) {
      p.imageUrl = img;
    }
    return p;
  }

  stockClass(level: string | undefined): string {
    if (level === 'LOW') return 'ims-pill-low';
    if (level === 'MEDIUM') return 'ims-pill-med';
    return 'ims-pill-ok';
  }

  handleProductDelete(productId: string): void {
    if (window.confirm('Are you sure you want to delete this product?')) {
      this.apiService.deleteProduct(productId).subscribe({
        next: (res: any) => {
          if (res.status === 200) {
            this.showMessage('Product deleted successfully');
            this.fetchProducts();
          }
        },
        error: (error) => {
          this.showMessage(
            error?.error?.message ||
              error?.message ||
              'Unable to delete product' + error
          );
        },
      });
    }
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.fetchProducts();
  }

  navigateToAddProductPage(): void {
    this.router.navigate(['/add-product']);
  }

  navigateToEditProductPage(productId: string): void {
    this.router.navigate([`/edit-product/${productId}`]);
  }

  showMessage(message: string) {
    this.message = message;
    setTimeout(() => {
      this.message = '';
    }, 4000);
  }
}
