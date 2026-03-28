import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../service/api.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css',
})
export class ProfileComponent implements OnInit {
  constructor(private apiService: ApiService) {}

  user: any = null;
  message = '';
  saving = false;
  editForm = {
    name: '',
    email: '',
    phoneNumber: '',
    password: '',
  };

  ngOnInit(): void {
    this.fetchUserInfo();
  }

  fetchUserInfo(): void {
    this.apiService.getLoggedInUserInfo().subscribe({
      next: (res) => {
        this.user = res;
        this.editForm = {
          name: res.name ?? '',
          email: res.email ?? '',
          phoneNumber: res.phoneNumber ?? '',
          password: '',
        };
      },
      error: (error) => {
        this.showMessage(
          error?.error?.message ||
            error?.message ||
            'Unable to load profile ' + error
        );
      },
    });
  }

  saveProfile(): void {
    if (!this.user?.id) {
      return;
    }
    this.saving = true;
    const body: Record<string, string> = {
      name: this.editForm.name,
      email: this.editForm.email,
      phoneNumber: this.editForm.phoneNumber,
    };
    if (this.editForm.password?.trim()) {
      body['password'] = this.editForm.password;
    }
    this.apiService.updateUser(this.user.id, body).subscribe({
      next: () => {
        this.saving = false;
        this.showMessage('Profile updated successfully');
        this.fetchUserInfo();
      },
      error: (error) => {
        this.saving = false;
        this.showMessage(
          error?.error?.message ||
            error?.message ||
            'Unable to update profile ' + error
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
