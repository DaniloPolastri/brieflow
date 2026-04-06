import { ChangeDetectionStrategy, Component, inject, computed } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { StorageService } from '../../core/services/storage.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  roles?: string[];
}

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    :host ::ng-deep .active-nav-item {
      background-color: #EEF2FF;
      color: #4F46E5;
      font-weight: 500;
    }
    :host ::ng-deep .active-nav-item:hover {
      background-color: #EEF2FF;
    }
    :host ::ng-deep .active-nav-item i {
      color: #6366F1;
    }
  `],
})
export class SidebarComponent {
  private readonly authService = inject(AuthService);
  private readonly storage = inject(StorageService);
  private readonly router = inject(Router);

  readonly currentUser = computed(() => this.authService.currentUser());
  readonly userName = computed(() => this.currentUser()?.name ?? '');
  readonly userEmail = computed(() => this.currentUser()?.email ?? '');
  readonly userInitial = computed(() => this.userName()?.charAt(0).toUpperCase() ?? '?');
  readonly userRole = computed(() => this.currentUser()?.role ?? '');

  readonly navItems: NavItem[] = [
    { label: 'Dashboard', icon: 'pi pi-objects-column', route: '/dashboard' },
    { label: 'Equipe', icon: 'pi pi-users', route: '/members' },
    { label: 'Configurações', icon: 'pi pi-cog', route: '/settings', roles: ['OWNER', 'MANAGER'] },
  ];

  readonly visibleNavItems = computed(() =>
    this.navItems.filter(item => {
      if (!item.roles) return true;
      return item.roles.includes(this.userRole());
    })
  );

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/auth/login']),
      error: () => {
        this.authService.clearAuth();
        this.router.navigate(['/auth/login']);
      },
    });
  }
}
