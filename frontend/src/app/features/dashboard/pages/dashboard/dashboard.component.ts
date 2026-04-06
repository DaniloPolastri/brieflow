import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  imports: [ButtonModule],
  template: `
    <div class="flex min-h-screen items-center justify-center bg-gray-50">
      <div class="text-center">
        <h1 class="text-3xl font-bold text-gray-900">Bem-vindo ao BriefFlow</h1>
        <p class="mt-2 text-gray-500">
          @if (authService.currentUser(); as user) {
            Ola, {{ user.name }}! O dashboard sera implementado em breve.
          } @else {
            O dashboard sera implementado em breve.
          }
        </p>
        <p-button label="Sair" severity="secondary" (onClick)="logout()" styleClass="mt-4" />
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent {
  readonly authService = inject(AuthService);
  private readonly router = inject(Router);

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
