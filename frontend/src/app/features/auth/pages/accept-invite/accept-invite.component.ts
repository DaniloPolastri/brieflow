import { ChangeDetectionStrategy, Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { InviteApiService } from '../../services/invite-api.service';
import { AuthService } from '../../../../core/services/auth.service';
import { InviteInfo } from '../../models/invite.model';
import { MemberRole, MemberPosition, MEMBER_ROLE_LABELS, MEMBER_POSITION_LABELS } from '../../../members/models/member.model';

@Component({
  selector: 'app-accept-invite',
  standalone: true,
  imports: [ReactiveFormsModule, InputTextModule, PasswordModule, ButtonModule, MessageModule],
  templateUrl: './accept-invite.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AcceptInviteComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly inviteApi = inject(InviteApiService);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  private readonly roleLabels: Record<string, string> = MEMBER_ROLE_LABELS as Record<string, string>;
  private readonly positionLabels: Record<string, string> = MEMBER_POSITION_LABELS as Record<string, string>;

  readonly loading = signal(false);
  readonly loadingInfo = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly inviteInfo = signal<InviteInfo | null>(null);
  readonly token = signal<string>('');

  readonly form = this.fb.nonNullable.group({
    name: [''],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  ngOnInit(): void {
    const tokenParam = this.route.snapshot.queryParamMap.get('token');
    if (!tokenParam) {
      this.router.navigate(['/auth/login']);
      return;
    }
    this.token.set(tokenParam);
    this.loadInviteInfo(tokenParam);
  }

  private loadInviteInfo(token: string): void {
    this.inviteApi.getInfo(token).subscribe({
      next: info => {
        this.inviteInfo.set(info);
        this.loadingInfo.set(false);
        if (!info.userExists) {
          this.form.controls.name.setValidators([Validators.required]);
          this.form.controls.name.updateValueAndValidity();
        }
      },
      error: () => {
        this.errorMessage.set('Convite inválido ou expirado');
        this.loadingInfo.set(false);
      },
    });
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.errorMessage.set(null);

    const { name, password } = this.form.getRawValue();
    const request = this.inviteInfo()?.userExists ? { password } : { name, password };

    this.inviteApi.accept(this.token(), request).subscribe({
      next: response => {
        this.authService.handleAuthResponse(response);
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: err => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message ?? 'Erro ao aceitar convite');
      },
    });
  }

  getRoleLabel(role: string): string {
    return this.roleLabels[role] || role;
  }

  getPositionLabel(position: string): string {
    return this.positionLabels[position] || position;
  }
}
