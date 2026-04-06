import { ChangeDetectionStrategy, Component, inject, computed } from '@angular/core';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  imports: [ButtonModule],
  template: `
    <div class="min-h-screen bg-gray-50/50 font-sans">
      <!-- Top bar -->
      <header class="flex h-14 items-center justify-between border-b border-gray-200 bg-white px-6">
        <div class="flex items-center gap-2.5">
          <div class="flex h-7 w-7 items-center justify-center rounded-md bg-indigo-500">
            <svg class="h-3.5 w-3.5 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <span class="text-[15px] font-semibold tracking-tight text-gray-900">BriefFlow</span>
        </div>

        <div class="flex items-center gap-4">
          @if (userName()) {
            <div class="flex items-center gap-2.5">
              <div class="flex h-7 w-7 items-center justify-center rounded-full bg-indigo-100 text-[12px] font-semibold text-indigo-700">
                {{ userInitial() }}
              </div>
              <span class="text-[13px] font-medium text-gray-700">{{ userName() }}</span>
            </div>
          }
          <button (click)="logout()"
                  class="flex items-center gap-1.5 rounded-md px-2.5 py-1.5 text-[13px] font-medium text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-700">
            <i class="pi pi-sign-out" style="font-size: 13px"></i>
            Sair
          </button>
        </div>
      </header>

      <!-- Content -->
      <main class="flex flex-col items-center justify-center px-6" style="min-height: calc(100vh - 56px)">
        <div class="text-center max-w-md">
          <!-- Empty state illustration -->
          <div class="mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-2xl bg-indigo-50">
            <svg class="h-8 w-8 text-indigo-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="3" y="3" width="7" height="7" rx="1.5" />
              <rect x="14" y="3" width="7" height="7" rx="1.5" />
              <rect x="3" y="14" width="7" height="7" rx="1.5" />
              <rect x="14" y="14" width="7" height="7" rx="1.5" />
            </svg>
          </div>

          <h1 class="text-[22px] font-semibold tracking-tight text-gray-900">
            @if (userName()) {
              Ola, {{ userName() }}!
            } @else {
              Bem-vindo ao BriefFlow
            }
          </h1>
          <p class="mt-2 text-[14px] leading-relaxed text-gray-500">
            O dashboard e o kanban estao sendo construidos.<br>
            Em breve voce podera gerenciar briefings, jobs e aprovacoes aqui.
          </p>

          <div class="mt-8 flex flex-col gap-3">
            <div class="flex items-center gap-3 rounded-lg border border-gray-200 bg-white px-4 py-3 text-left">
              <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-emerald-50">
                <i class="pi pi-check text-emerald-500" style="font-size: 14px"></i>
              </div>
              <div>
                <p class="text-[13px] font-medium text-gray-900">Conta criada</p>
                <p class="text-[12px] text-gray-400">Autenticacao funcionando</p>
              </div>
            </div>
            <div class="flex items-center gap-3 rounded-lg border border-dashed border-gray-300 bg-white/50 px-4 py-3 text-left">
              <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-gray-100">
                <i class="pi pi-clock text-gray-400" style="font-size: 14px"></i>
              </div>
              <div>
                <p class="text-[13px] font-medium text-gray-500">Kanban de producao</p>
                <p class="text-[12px] text-gray-400">Em desenvolvimento</p>
              </div>
            </div>
            <div class="flex items-center gap-3 rounded-lg border border-dashed border-gray-300 bg-white/50 px-4 py-3 text-left">
              <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-gray-100">
                <i class="pi pi-clock text-gray-400" style="font-size: 14px"></i>
              </div>
              <div>
                <p class="text-[13px] font-medium text-gray-500">Portal de aprovacao</p>
                <p class="text-[12px] text-gray-400">Em desenvolvimento</p>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly userName = computed(() => this.authService.currentUser()?.name ?? null);
  readonly userInitial = computed(() => this.userName()?.charAt(0).toUpperCase() ?? '?');

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
