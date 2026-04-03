import { Routes } from '@angular/router';
import { GuardService } from './service/guard.service';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { CategoryComponent } from './category/category.component';
import { SupplierComponent } from './supplier/supplier.component';
import { AddEditSupplierComponent } from './add-edit-supplier/add-edit-supplier.component';
import { ProductComponent } from './product/product.component';
import { AddEditProductComponent } from './add-edit-product/add-edit-product.component';
import { PurchaseComponent } from './purchase/purchase.component';
import { SellComponent } from './sell/sell.component';
import { TransactionComponent } from './transaction/transaction.component';
import { TransactionDetailsComponent } from './transaction-details/transaction-details.component';
import { ProfileComponent } from './profile/profile.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { StockAlertsComponent } from './stock-alerts/stock-alerts.component';
import { PurchaseOrdersComponent } from './purchase-orders/purchase-orders.component';
import { AnalyticsComponent } from './analytics/analytics.component';

export const routes: Routes = [

  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },

  { path: 'category', component: CategoryComponent, canActivate: [GuardService], data: { roles: ['ADMIN', 'WAREHOUSE_MANAGER'] } },
  { path: 'suppliers', component: SupplierComponent, canActivate: [GuardService], data: { roles: ['ADMIN', 'PROCUREMENT_OFFICER'] } },
  { path: 'edit-supplier/:supplierId', component: AddEditSupplierComponent, canActivate: [GuardService], data: { roles: ['ADMIN', 'PROCUREMENT_OFFICER'] } },
  { path: 'add-supplier', component: AddEditSupplierComponent, canActivate: [GuardService], data: { roles: ['ADMIN', 'PROCUREMENT_OFFICER'] } },

  { path: 'product', component: ProductComponent, canActivate: [GuardService], data: { roles: ['ADMIN', 'WAREHOUSE_MANAGER', 'PROCUREMENT_OFFICER', 'STAFF'] } },
  { path: 'edit-product/:productId', component: AddEditProductComponent, canActivate: [GuardService], data: { roles: ['ADMIN', 'WAREHOUSE_MANAGER'] } },
  { path: 'add-product', component: AddEditProductComponent, canActivate: [GuardService], data: { roles: ['ADMIN', 'WAREHOUSE_MANAGER'] } },

  { path: 'purchase-orders', component: PurchaseOrdersComponent, canActivate: [GuardService], data: { roles: ['ADMIN', 'PROCUREMENT_OFFICER', 'WAREHOUSE_MANAGER'] } },
  { path: 'purchase', component: PurchaseComponent, canActivate: [GuardService], data: { roles: ['ADMIN', 'WAREHOUSE_MANAGER'] } },
  { path: 'sell', component: SellComponent, canActivate: [GuardService], data: { roles: ['ADMIN', 'STAFF'] } },

  { path: 'transaction', component: TransactionComponent, canActivate: [GuardService], data: { roles: ['ADMIN', 'STAFF', 'WAREHOUSE_MANAGER', 'PROCUREMENT_OFFICER'] } },
  { path: 'transaction/:transactionId', component: TransactionDetailsComponent, canActivate: [GuardService], data: { roles: ['ADMIN', 'STAFF', 'WAREHOUSE_MANAGER', 'PROCUREMENT_OFFICER'] } },

  { path: 'profile', component: ProfileComponent, canActivate: [GuardService] },
  { path: 'dashboard', component: DashboardComponent, canActivate: [GuardService] },
  { path: 'stock-alerts', component: StockAlertsComponent, canActivate: [GuardService], data: { roles: ['ADMIN', 'WAREHOUSE_MANAGER', 'PROCUREMENT_OFFICER'] } },
  { path: 'analytics', component: AnalyticsComponent, canActivate: [GuardService], data: { roles: ['ADMIN'] } },

  //   WIDE CARD
  { path: "", redirectTo: "/login", pathMatch: 'full' },
  // {path: "**", redirectTo: "/dashboard"}

];
