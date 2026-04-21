import { EventEmitter, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import CryptoJS from "crypto-js";



@Injectable({
  providedIn: 'root',
})


export class ApiService {

  authStatuschanged = new EventEmitter<void>();
  private static BASE_URL = 'http://localhost:5050/api';
  /** Use for static files served by Spring (e.g. {@code /products/...} uploads). */
  static readonly API_ORIGIN = 'http://localhost:5050';
  private static ENCRYPTION_KEY = "phegon-dev-inventory";

  /** Absolute URL for product images when the API stores paths like {@code products/uuid.png}. */
  static resolvePublicAsset(path: string | null | undefined): string | undefined {
    if (!path) return undefined;
    const p = String(path).trim();
    if (!p || p.startsWith('http') || p.startsWith('data:')) return p;
    const slash = p.startsWith('/') ? p : '/' + p;
    return `${ApiService.API_ORIGIN}${slash}`;
  }


  constructor(private http: HttpClient) { }

  // Encrypt data and save to localStorage
  encryptAndSaveToStorage(key: string, value: string): void {
    const encryptedValue = CryptoJS.AES.encrypt(value, ApiService.ENCRYPTION_KEY).toString();
    localStorage.setItem(key, encryptedValue);
  }

  // Retreive from localStorage and Decrypt
  private getFromStorageAndDecrypt(key: string): any {
    try {
      const encryptedValue = localStorage.getItem(key);
      if (!encryptedValue) return null;
      return CryptoJS.AES.decrypt(encryptedValue, ApiService.ENCRYPTION_KEY).toString(CryptoJS.enc.Utf8);
    } catch (error) {
      return null;
    }
  }


  private clearAuth() {
    localStorage.removeItem("token");
    localStorage.removeItem("role");
  }



  private getHeader(): HttpHeaders {
    const token = this.getFromStorageAndDecrypt("token");
    return new HttpHeaders({
      Authorization: `Bearer ${token}`,
    });
  }







  /***AUTH & USERS API METHODS */

  registerUser(body: any): Observable<any> {
    return this.http.post(`${ApiService.BASE_URL}/auth/register`, body);
  }

  loginUser(body: any): Observable<any> {
    return this.http.post(`${ApiService.BASE_URL}/auth/login`, body);
  }

  getLoggedInUserInfo(): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/users/current`, {
      headers: this.getHeader(),
    });
  }

  updateUser(id: string | number, body: any): Observable<any> {
    return this.http.put(`${ApiService.BASE_URL}/users/update/${id}`, body, {
      headers: this.getHeader(),
    });
  }

  getDashboardSummary(): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/dashboard/summary`, {
      headers: this.getHeader(),
    });
  }









  /**CATEGOTY ENDPOINTS */
  createCategory(body: any): Observable<any> {
    return this.http.post(`${ApiService.BASE_URL}/categories/add`, body, {
      headers: this.getHeader(),
    });
  }

  getAllCategory(): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/categories/all`, {
      headers: this.getHeader(),
    });
  }

  getCategoryById(id: string): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/categories/${id}`, {
      headers: this.getHeader(),
    });
  }

  updateCategory(id: string, body: any): Observable<any> {
    return this.http.put(
      `${ApiService.BASE_URL}/categories/update/${id}`,
      body,
      {
        headers: this.getHeader(),
      }
    );
  }

  deleteCategory(id: string): Observable<any> {
    return this.http.delete(`${ApiService.BASE_URL}/categories/delete/${id}`, {
      headers: this.getHeader(),
    });
  }






  /** SUPPLIER API */
  addSupplier(body: any): Observable<any> {
    return this.http.post(`${ApiService.BASE_URL}/suppliers/add`, body, {
      headers: this.getHeader(),
    });
  }

  getAllSuppliers(): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/suppliers/all`, {
      headers: this.getHeader(),
    });
  }

  getSupplierById(id: string): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/suppliers/${id}`, {
      headers: this.getHeader(),
    });
  }

  updateSupplier(id: string, body: any): Observable<any> {
    return this.http.put(
      `${ApiService.BASE_URL}/suppliers/update/${id}`,
      body,
      {
        headers: this.getHeader(),
      }
    );
  }

  deleteSupplier(id: string): Observable<any> {
    return this.http.delete(`${ApiService.BASE_URL}/suppliers/delete/${id}`, {
      headers: this.getHeader(),
    });
  }

  updateSupplierStatus(id: string | number, status: string): Observable<any> {
    return this.http.patch(`${ApiService.BASE_URL}/suppliers/${id}/status`, null, {
      headers: this.getHeader(),
      params: { status }
    });
  }







  /**PRODUICTS ENDPOINTS */
  addProduct(formData: any): Observable<any> {
    return this.http.post(`${ApiService.BASE_URL}/products/add`, formData, {
      headers: this.getHeader(),
    });
  }

  updateProduct(formData: any): Observable<any> {
    return this.http.put(`${ApiService.BASE_URL}/products/update`, formData, {
      headers: this.getHeader(),
    });
  }

  getAllProducts(): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/products/all`, {
      headers: this.getHeader(),
    });
  }

  getProductById(id: string): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/products/${id}`, {
      headers: this.getHeader(),
    });
  }

  deleteProduct(id: string): Observable<any> {
    return this.http.delete(`${ApiService.BASE_URL}/products/delete/${id}`, {
      headers: this.getHeader(),
    });
  }








  /**Transactions Endpoints */

  purchaseProduct(body: any): Observable<any> {
    return this.http.post(
      `${ApiService.BASE_URL}/transactions/purchase`,
      body,
      {
        headers: this.getHeader(),
      }
    );
  }

  sellProduct(body: any): Observable<any> {
    return this.http.post(`${ApiService.BASE_URL}/transactions/sell`, body, {
      headers: this.getHeader(),
    });
  }

  getAllTransactions(
    searchText: string,
    page: number = 0,
    size: number = 10,
    transactionType?: string,
    status?: string
  ): Observable<any> {
    const params: Record<string, string | number> = {
      searchText: searchText ?? '',
      page,
      size,
    };
    if (transactionType) {
      params['transactionType'] = transactionType;
    }
    if (status) {
      params['status'] = status;
    }
    return this.http.get(`${ApiService.BASE_URL}/transactions/all`, {
      params,
      headers: this.getHeader(),
    });
  }

  getTransactionById(id: string): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/transactions/${id}`, {
      headers: this.getHeader(),
    });
  }

  getTransactionAnalytics(
    month?: number,
    year?: number
  ): Observable<any> {
    const params: Record<string, number> = {};
    if (month != null && year != null) {
      params['month'] = month;
      params['year'] = year;
    }
    return this.http.get(`${ApiService.BASE_URL}/transactions/analytics`, {
      params: Object.keys(params).length ? params : undefined,
      headers: this.getHeader(),
    });
  }


  updateTransactionStatus(id: string, status: string): Observable<any> {
    return this.http.put(`${ApiService.BASE_URL}/transactions/update/${id}`, JSON.stringify(status), {
      headers: this.getHeader().set("Content-Type", "application/json")
    });
  }


  getTransactionsByMonthAndYear(month: number, year: number): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/transactions/by-month-year`, {
      headers: this.getHeader(),
      params: {
        month: month,
        year: year,
      },
    });
  }

  /** Stock alerts (active alerts only) */
  getActiveAlertsCount(): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/alerts/count`, {
      headers: this.getHeader(),
    });
  }

  getActiveAlerts(): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/alerts`, {
      headers: this.getHeader(),
    });
  }

  /** Purchase Orders */
  getPurchaseOrderSummary(): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/purchase-orders/summary`, {
      headers: this.getHeader(),
    });
  }

  getPurchaseOrders(status: string): Observable<any> {
    const params: any = {};
    if (status && status !== 'ALL') params.status = status;
    return this.http.get(`${ApiService.BASE_URL}/purchase-orders`, {
      headers: this.getHeader(),
      params,
    });
  }

  createPurchaseOrder(body: any): Observable<any> {
    return this.http.post(`${ApiService.BASE_URL}/purchase-orders`, body, {
      headers: this.getHeader(),
    });
  }

  approvePurchaseOrder(id: string): Observable<any> {
    return this.http.put(`${ApiService.BASE_URL}/purchase-orders/${id}/approve`, null, {
      headers: this.getHeader(),
    });
  }

  receivePurchaseOrder(id: string): Observable<any> {
    return this.http.put(`${ApiService.BASE_URL}/purchase-orders/${id}/receive`, null, {
      headers: this.getHeader(),
    });
  }

  /** Supplier management (enhanced supplier screen) */
  getSupplierManagementSummary(): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/suppliers/management/summary`, {
      headers: this.getHeader(),
    });
  }

  getSupplierManagementAll(): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/suppliers/management/all`, {
      headers: this.getHeader(),
    });
  }

  upsertSupplierManagement(body: any): Observable<any> {
    return this.http.post(`${ApiService.BASE_URL}/suppliers/management/upsert`, body, {
      headers: this.getHeader(),
    });
  }

  getSupplierManagementById(id: string | number): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/suppliers/management/${id}`, {
      headers: this.getHeader(),
    });
  }

  /** Reports & Analytics */
  getAnalyticsSummary(range: string): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/analytics/summary`, {
      headers: this.getHeader(),
      params: { range },
    });
  }

  getBiSummary(): Observable<any> {
    return this.http.get(`${ApiService.BASE_URL}/analytics/bi-summary`, {
      headers: this.getHeader(),
    });
  }












  /**AUTHENTICATION CHECKER */

  logout(): void {
    this.clearAuth()
  }

  isAuthenticated(): boolean {
    const token = this.getFromStorageAndDecrypt("token");
    return !!token;
  }

  isAdmin(): boolean {
    const role = this.getFromStorageAndDecrypt("role");
    return role === "ADMIN";
  }

  getRole(): string | null {
    return this.getFromStorageAndDecrypt("role");
  }

  isStaff(): boolean {
    return this.getRole() === "STAFF";
  }

  isWarehouseManager(): boolean {
    return this.getRole() === "WAREHOUSE_MANAGER";
  }

  isProcurementOfficer(): boolean {
    return this.getRole() === "PROCUREMENT_OFFICER" || this.getRole() === "PROCUREMENT";
  }

}
