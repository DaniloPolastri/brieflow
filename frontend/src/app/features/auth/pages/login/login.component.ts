import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, InputTextModule, PasswordModule, ButtonModule, MessageModule],
  template: `
    <div class="rounded-xl bg-white p-8 shadow-sm ring-1 ring-gray-200">
      <h2 class="mb-6 text-xl font-semibold text-gray-900">Entrar</h2>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" styleClass="mb-4 w-full" />
      }

      <form [formGroup]="form" (ngSubmit)="onSubmit()">
        <div class="mb-4">
          <label for="email" class="mb-1 block text-sm font-medium text-gray-700">Email</label>
          <input pInputText id="email" formControlName="email" type="email"
                 placeholder="seu@email.com" class="w-full" />
          @if (form.controls.email.touched && form.controls.email.hasError('required')) {
            <small class="text-red-500">Email e obrigatorio</small>
          }
          @if (form.controls.email.touched && form.controls.email.hasError('email')) {
            <small class="text-red-500">Email invalido</small>
          }
        </div>

        <div class="mb-6">
          <label for="password" class="mb-1 block text-sm font-medium text-gray-700">Senha</label>
          <p-password id="password" formControlName="password"
                      [feedback]="false" [toggleMask]="true" styleClass="w-full" inputStyleClass="w-full" />
          @if (form.controls.password.touched && form.controls.password.hasError('required')) {
            <small class="text-red-500">Senha e obrigatoria</small>
          }
        </div>

        <p-button type="submit" label="Entrar" [loading]="loading()"
                  [disabled]="form.invalid || loading()" styleClass="w-full" />
      </form>

      <p class="mt-4 text-center text-sm text-gray-500">
        Nao tem conta?
        <a routerLink="/auth/register" class="font-medium text-indigo-600 hover:text-indigo-500">Criar conta</a>
      </p>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  onSubmit(): void {
    if (this.form.invalid) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    const { email, password } = this.form.getRawValue();
    this.authService.login({ email, password }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: err => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message ?? 'Erro ao fazer login');
      },
    });
  }
}
