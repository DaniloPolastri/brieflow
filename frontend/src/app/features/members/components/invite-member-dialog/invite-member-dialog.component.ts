import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
  model,
  output,
} from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { ButtonModule } from 'primeng/button';
import { MemberApiService } from '@features/members/services/member-api.service';
import { InviteMemberRequest, MemberPosition, MemberRole, MEMBER_POSITION_LABELS } from '@features/members/models/member.model';

@Component({
  selector: 'app-invite-member-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, DialogModule, InputTextModule, SelectModule, ButtonModule],
  templateUrl: './invite-member-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InviteMemberDialogComponent {
  readonly visible = model(false);
  readonly invited = output<void>();

  private readonly memberApi = inject(MemberApiService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly inviteLink = signal<string | null>(null);
  readonly copied = signal(false);

  readonly roleOptions = [
    { label: 'Gestor', value: 'MANAGER' },
    { label: 'Criativo', value: 'CREATIVE' },
  ];

  readonly positionOptions = Object.entries(MEMBER_POSITION_LABELS).map(([value, label]) => ({
    label,
    value,
  }));

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    role: ['CREATIVE', [Validators.required]],
    position: ['DESIGNER_GRAFICO' as string, [Validators.required]],
  });

  onSubmit(): void {
    if (this.form.invalid || this.loading()) return;

    this.loading.set(true);

    const { email, role, position } = this.form.getRawValue();

    const request: InviteMemberRequest = {
      email,
      role: role as MemberRole,
      position: position as MemberPosition,
    };

    this.memberApi
      .invite(request)
      .subscribe({
        next: (response) => {
          this.inviteLink.set(response.inviteLink);
          this.loading.set(false);
          this.invited.emit();
        },
        error: () => {
          this.loading.set(false);
        },
      });
  }

  copyLink(): void {
    const link = this.inviteLink();
    if (!link) return;

    navigator.clipboard.writeText(link).then(() => {
      this.copied.set(true);
    });
  }

  onHide(): void {
    this.form.reset({ email: '', role: 'CREATIVE', position: 'DESIGNER_GRAFICO' });
    this.inviteLink.set(null);
    this.copied.set(false);
  }
}
