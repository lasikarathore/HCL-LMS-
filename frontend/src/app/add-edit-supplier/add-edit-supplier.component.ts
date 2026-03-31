import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../service/api.service';

@Component({
  selector: 'app-add-edit-supplier',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './add-edit-supplier.component.html',
  styleUrl: './add-edit-supplier.component.css',
})
export class AddEditSupplierComponent implements OnInit {
  constructor(
    private apiService: ApiService,
    private router: Router,
    private route: ActivatedRoute
  ) {}
  message: string = '';
  isEditing: boolean = false;
  supplierId: string | null = null;

  formData: any = {
    name: '',
    address: '',
    email: '',
    categorySpecialisation: '',
    paymentTerms: 'Net 30',
    starRating: 4.2,
    onTimeDeliveryPercent: 90,
    active: true,
  };

  categories: any[] = [];

  ngOnInit(): void {
    this.loadCategories();
    this.route.paramMap.subscribe((pm) => {
      const id = pm.get('supplierId');
      if (id) {
        this.supplierId = id;
        this.isEditing = true;
        this.fetchSupplier();
      }
    });
  }

  loadCategories(): void {
    this.apiService.getAllCategory().subscribe({
      next: (res: any) => {
        if (res?.status === 200) {
          this.categories = res?.categories || [];
        }
      },
      error: () => {},
    });
  }

  fetchSupplier(): void {
    this.apiService.getSupplierManagementById(this.supplierId!).subscribe({
      next: (res: any) => {
        if (res.status === 200) {
          const s = res?.supplierTopPerformer || {};
          this.formData = {
            name: s.name || '',
            address: s.address || '',
            email: s.email || '',
            categorySpecialisation: s.categorySpecialisation || '',
            paymentTerms: s.paymentTerms || 'Net 30',
            starRating: s.starRating ?? 4.2,
            onTimeDeliveryPercent: s.onTimeDeliveryPercent ?? 90,
            active: s.active ?? true,
          };
        }
      },
      error: (error) => {
        this.showMessage(
          error?.error?.message ||
            error?.message ||
            'Unable to get supplier by id' + error
        );
      },
    });
  }

  // HANDLE FORM SUBMISSION
  handleSubmit() {
    if (!this.formData.name?.trim()) {
      this.showMessage('Supplier name is required');
      return;
    }

    //prepare data for submission
    const supplierData = {
      id: this.isEditing ? Number(this.supplierId) : null,
      name: this.formData.name,
      address: this.formData.address,
      email: this.formData.email,
      categorySpecialisation: this.formData.categorySpecialisation,
      paymentTerms: this.formData.paymentTerms,
      starRating: Number(this.formData.starRating),
      onTimeDeliveryPercent: Number(this.formData.onTimeDeliveryPercent),
      active: !!this.formData.active,
    };
    
    this.apiService.upsertSupplierManagement(supplierData).subscribe({
      next: (res: any) => {
        if (res?.status === 200) {
          this.showMessage(this.isEditing ? 'Supplier updated successfully' : 'Supplier added successfully');
          this.router.navigate(['/suppliers']);
        } else {
          this.showMessage(res?.message || 'Unable to save supplier');
        }
      },
      error: (error) => {
        this.showMessage(
          error?.error?.message || error?.message || 'Unable to save supplier ' + error
        );
      },
    });
  }







  showMessage(message: string) {
    this.message = message;
    setTimeout(() => {
      this.message = '';
    }, 4000);
  }
}
