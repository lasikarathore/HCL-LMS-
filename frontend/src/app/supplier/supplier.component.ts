import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ApiService } from '../service/api.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-supplier',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './supplier.component.html',
  styleUrl: './supplier.component.css',
})
export class SupplierComponent implements OnInit {
  constructor(private apiService: ApiService, private router: Router) {}
  suppliers: any[] = [];
  summary: any = null;
  message: string = '';

  stars(r: number): string {
    const n = Math.round(Number(r || 0));
    const full = Math.max(0, Math.min(5, n));
    return '★★★★★'.slice(0, full) + '☆☆☆☆☆'.slice(0, 5 - full);
  }

  ngOnInit(): void {
    this.loadSummary();
    this.getSuppliers();
  }

  loadSummary(): void {
    this.apiService.getSupplierManagementSummary().subscribe({
      next: (res: any) => {
        const ok = res?.status === 200 || res?.status === '200';
        this.summary = ok ? res : null;
      },
      error: () => (this.summary = null),
    });
  }

  getSuppliers(): void {
    this.apiService.getSupplierManagementAll().subscribe({
      next: (res: any) => {
        const ok = res?.status === 200 || res?.status === '200';
        if (ok) {
          this.suppliers = res.supplierManagement || [];
        } else {
          this.showMessage(res.message);
        }
      },
      error: (error) => {
        this.showMessage(
          error?.error?.message ||
            error?.message ||
            'Unable to get suppliers' + error
        );
      },
    });
  }

  //Navigate to ass supplier Page
  navigateToAddSupplierPage(): void {
    this.router.navigate([`/add-supplier`]);
  }

  //Navigate to edit supplier Page
  navigateToEditSupplierPage(supplierId: string): void {
    this.router.navigate([`/edit-supplier/${supplierId}`]);
  }

  //Delete a caetgory
  handleDeleteSupplier(supplierId: string):void{
    if (window.confirm("Are you sure you want to delete this supplier?")) {
      this.apiService.deleteSupplier(supplierId).subscribe({
        next:(res:any) =>{
          if (res.status === 200) {
            this.showMessage("Supplier deleted successfully")
            this.loadSummary();
            this.getSuppliers(); //reload the suppliers
          }
        },
        error:(error) =>{
          this.showMessage(error?.error?.message || error?.message || "Unable to Delete Supplier" + error)
        }
      })
    }
  }

  showMessage(message: string) {
    this.message = message;
    setTimeout(() => {
      this.message = '';
    }, 4000);
  }
}
