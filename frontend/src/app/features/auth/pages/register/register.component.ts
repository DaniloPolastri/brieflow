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
    <div>
      <h1 class="text-[22px] font-semibold tracking-tight text-gray-900">Criar sua conta</h1>
      <p class="mt-1.5 text-[14px] text-gray-500">
        Comece gratis. Configure seu workspace em minutos.
      </p>

      @if (errorMessage()) {
        <div class="mt-5 flex items-start gap-2.5 rounded-lg border border-red-200 bg-red-50 px-3.5 py-3 text-[13px] text-red-700">
          <i class="pi pi-exclamation-circle mt-0.5 text-red-400" style="font-size: 14px"></i>
          <span>{{ errorMessage() }}</span>
        </div>
      }

      <form [formGroup]="form" (ngSubmit)="onSubmit()" class="mt-7 space-y-5">
        <div>
          <label for="name" class="mb-1.5 block text-[13px] font-medium text-gray-700">Nome completo</label>
          <input pInputText id="name" formControlName="name" placeholder="Seu nome" class="w-full" />
          @if (form.controls.name.touched && form.controls.name.hasError('required')) {
            <small class="mt-1 block text-[12px] text-red-500">Nome e obrigatorio</small>
          }
        </div>

        <div>
          <label for="email" class="mb-1.5 block text-[13px] font-medium text-gray-700">Email profissional</label>
          <input pInputText id="email" formControlName="email" type="email"
                 placeholder="voce@agencia.com" class="w-full" />
          @if (form.controls.email.touched && form.controls.email.hasError('required')) {
            <small class="mt-1 block text-[12px] text-red-500">Email e obrigatorio</small>
          }
          @if (form.controls.email.touched && form.controls.email.hasError('email')) {
            <small class="mt-1 block text-[12px] text-red-500">Email invalido</small>
          }
        </div>

        <div>
          <label for="password" class="mb-1.5 block text-[13px] font-medium text-gray-700">Senha</label>
          <p-password id="password" formControlName="password"
                      [feedback]="false" [toggleMask]="true" styleClass="w-full" inputStyleClass="w-full" />
          @if (form.controls.password.touched && form.controls.password.hasError('required')) {
            <small class="mt-1 block text-[12px] text-red-500">Senha e obrigatoria</small>
          }
          @if (form.controls.password.touched && form.controls.password.hasError('minlength')) {
            <small class="mt-1 block text-[12px] text-red-500">Minimo de 8 caracteres</small>
          }
        </div>

        <div>
          <label for="confirmPassword" class="mb-1.5 block text-[13px] font-medium text-gray-700">Confirmar senha</label>
          <p-password id="confirmPassword" formControlName="confirmPassword"
                      [feedback]="false" [toggleMask]="true" styleClass="w-full" inputStyleClass="w-full" />
          @if (form.controls.confirmPassword.touched && form.hasError('passwordMismatch')) {
            <small class="mt-1 block text-[12px] text-red-500">Senhas nao conferem</small>
          }
        </div>

        <p-button type="submit" label="Criar conta" [loading]="loading()"
                  [disabled]="form.invalid || loading()" styleClass="w-full !mt-7" />
      </form>

      <div class="mt-6 flex items-center gap-3">
        <div class="h-px flex-1 bg-gray-200"></div>
        <span class="text-[12px] font-medium text-gray-400 uppercase tracking-wider">ou</span>
        <div class="h-px flex-1 bg-gray-200"></div>
      </div>

      <p class="mt-5 text-center text-[13px] text-gray-500">
        Ja tem uma conta?
        <a routerLink="/auth/login" class="font-medium text-indigo-600 hover:text-indigo-500 transition-colors">
          Entrar
        </a>
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
