import { Injectable } from '@angular/core';
import { ApiService } from './api.service';
import { ActivatedRouteSnapshot, CanActivate, GuardResult, MaybeAsync, Router, RouterStateSnapshot } from '@angular/router';


@Injectable({
  providedIn: 'root'
})

export class GuardService implements CanActivate {

  constructor(private apiService: ApiService, private router: Router) { }

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot): boolean {

    const isAuthenticated = this.apiService.isAuthenticated();
    if (!isAuthenticated) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
      return false;
    }

    const requiredRoles = route.data['roles'] as Array<string>;
    if (!requiredRoles || requiredRoles.length === 0) {
      return true;
    }

    const userRole = this.apiService.getRole();
    const hasRole = userRole && requiredRoles.includes(userRole);

    if (hasRole) {
      return true;
    } else {
      // User is authenticated but doesn't have the right role
      this.router.navigate(['/dashboard']);
      return false;
    }
  }
}
