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
  };

  ngOnInit(): void {
    this.route.paramMap.subscribe((pm) => {
      const id = pm.get('supplierId');
      if (id) {
        this.supplierId = id;
        this.isEditing = true;
        this.fetchSupplier();
      }
    });
  }

  fetchSupplier(): void {
    this.apiService.getSupplierById(this.supplierId!).subscribe({
      next: (res: any) => {
        if (res.status === 200) {
          this.formData = {
            name: res.supplier.name,
            address: res.supplier.address,
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
      name: this.formData.name,
      address: this.formData.address,
    };
    

    if (this.isEditing) {
      this.apiService.updateSupplier(this.supplierId!, supplierData).subscribe({
        next:(res:any) =>{
          if (res.status === 200) {
            this.showMessage("Supplier updated successfully");
            this.router.navigate(['/supplier'])
          }
        },
        error:(error) =>{
          this.showMessage(error?.error?.message || error?.message || "Unable to edit supplier" + error)
        }
      })
    } else {
      this.apiService.addSupplier(supplierData).subscribe({
        next:(res:any) =>{
          if (res.status === 200) {
            this.showMessage("Supplier Added successfully");
            this.router.navigate(['/supplier'])
          }
        },
        error:(error) =>{
          this.showMessage(error?.error?.message || error?.message || "Unable to Add supplier" + error)
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
