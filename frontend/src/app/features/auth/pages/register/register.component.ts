import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, InputTextModule, PasswordModule, ButtonModule, MessageModule],
  template: `
    <div class="rounded-xl bg-white p-8 shadow-sm ring-1 ring-gray-200">
      <h2 class="mb-6 text-xl font-semibold text-gray-900">Criar conta</h2>

      @if (errorMessage()) {
        <p-message severity="error" [text]="errorMessage()!" styleClass="mb-4 w-full" />
      }

      <form [formGroup]="form" (ngSubmit)="onSubmit()">
        <div class="mb-4">
          <label for="name" class="mb-1 block text-sm font-medium text-gray-700">Nome</label>
          <input pInputText id="name" formControlName="name" placeholder="Seu nome" class="w-full" />
          @if (form.controls.name.touched && form.controls.name.hasError('required')) {
            <small class="text-red-500">Nome e obrigatorio</small>
          }
        </div>

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

        <div class="mb-4">
          <label for="password" class="mb-1 block text-sm font-medium text-gray-700">Senha</label>
          <p-password id="password" formControlName="password"
                      [feedback]="false" [toggleMask]="true" styleClass="w-full" inputStyleClass="w-full" />
          @if (form.controls.password.touched && form.controls.password.hasError('required')) {
            <small class="text-red-500">Senha e obrigatoria</small>
          }
          @if (form.controls.password.touched && form.controls.password.hasError('minlength')) {
            <small class="text-red-500">Senha deve ter no minimo 8 caracteres</small>
          }
        </div>

        <div class="mb-6">
          <label for="confirmPassword" class="mb-1 block text-sm font-medium text-gray-700">Confirmar senha</label>
          <p-password id="confirmPassword" formControlName="confirmPassword"
                      [feedback]="false" [toggleMask]="true" styleClass="w-full" inputStyleClass="w-full" />
          @if (form.controls.confirmPassword.touched && form.hasError('passwordMismatch')) {
            <small class="text-red-500">Senhas nao conferem</small>
          }
        </div>

        <p-button type="submit" label="Criar conta" [loading]="loading()"
                  [disabled]="form.invalid || loading()" styleClass="w-full" />
      </form>

      <p class="mt-4 text-center text-sm text-gray-500">
        Ja tem conta?
        <a routerLink="/auth/login" class="font-medium text-indigo-600 hover:text-indigo-500">Entrar</a>
      </p>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]],
  }, { validators: [passwordMatchValidator] });

  onSubmit(): void {
    if (this.form.invalid) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    const { name, email, password } = this.form.getRawValue();
    this.authService.register({ name, email, password }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: err => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message ?? 'Erro ao criar conta');
      },
    });
  }
}

function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password');
  const confirmPassword = control.get('confirmPassword');
  if (password && confirmPassword && password.value !== confirmPassword.value) {
    return { passwordMismatch: true };
  }
  return null;
}
