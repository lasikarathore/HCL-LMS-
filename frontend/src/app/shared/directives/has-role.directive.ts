import { Directive, Input, TemplateRef, ViewContainerRef, OnInit } from '@angular/core';
import { ApiService } from '../../service/api.service';

@Directive({
  selector: '[appHasRole]',
  standalone: true
})
export class HasRoleDirective implements OnInit {
  private roles: string[] = [];
  private isVisible = false;

  constructor(
    private templateRef: TemplateRef<any>,
    private viewContainer: ViewContainerRef,
    private apiService: ApiService
  ) {}

  @Input()
  set appHasRole(val: string | string[]) {
    this.roles = Array.isArray(val) ? val : [val];
    this.updateView();
  }

  ngOnInit() {
    this.updateView();
  }

  private updateView() {
    const userRole = this.apiService.getRole();
    
    // If user has one of the required roles, show the element
    const hasPermission = userRole && this.roles.some(r => r === userRole);

    if (hasPermission && !this.isVisible) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.isVisible = true;
    } else if (!hasPermission && this.isVisible) {
      this.viewContainer.clear();
      this.isVisible = false;
    }
  }
}
