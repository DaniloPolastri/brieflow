import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
  OnInit,
} from '@angular/core';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmationService } from 'primeng/api';
import { MemberApiService } from '../../services/member-api.service';
import { StorageService } from '../../../../core/services/storage.service';
import {
  Member,
  InviteResponse,
  MEMBER_ROLE_LABELS,
  MEMBER_POSITION_LABELS,
  MemberRole,
  MemberPosition,
} from '../../models/member.model';
import { InviteMemberDialogComponent } from '../../components/invite-member-dialog/invite-member-dialog.component';

@Component({
  selector: 'app-member-list',
  standalone: true,
  imports: [TableModule, ButtonModule, TagModule, ConfirmDialogModule, TooltipModule, InviteMemberDialogComponent],
  templateUrl: './member-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [ConfirmationService],
})
export class MemberListComponent implements OnInit {
  private readonly memberApi = inject(MemberApiService);
  private readonly storage = inject(StorageService);
  private readonly confirmService = inject(ConfirmationService);

  readonly members = signal<Member[]>([]);
  readonly pendingInvites = signal<InviteResponse[]>([]);
  readonly loading = signal(true);
  readonly showInviteDialog = signal(false);

  readonly copiedInviteId = signal<number | null>(null);
  readonly currentUser = this.storage.getUser();
  readonly canManage = this.currentUser?.role === 'OWNER' || this.currentUser?.role === 'MANAGER';

  ngOnInit(): void {
    this.loadMembers();
  }

  loadMembers(): void {
    this.loading.set(true);
    this.memberApi.list().subscribe({
      next: (response) => {
        this.members.set(response.members);
        this.pendingInvites.set(response.pendingInvites);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  removeMember(member: Member): void {
    this.confirmService.confirm({
      message: `Deseja remover ${member.userName} do workspace?`,
      header: 'Remover membro',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Remover',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.memberApi.remove(member.id).subscribe({
          next: () => this.loadMembers(),
          error: (err) => {
            console.error('Erro ao remover membro:', err);
          },
        });
      },
    });
  }

  getRoleLabel(role: string): string {
    return MEMBER_ROLE_LABELS[role as MemberRole] ?? role;
  }

  getPositionLabel(position: string): string {
    return MEMBER_POSITION_LABELS[position as MemberPosition] ?? position;
  }

  getInitials(name: string): string {
    return name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .substring(0, 2)
      .toUpperCase();
  }

  getRoleSeverity(role: string): 'danger' | 'warn' | 'info' {
    switch (role) {
      case 'OWNER':
        return 'danger';
      case 'MANAGER':
        return 'warn';
      default:
        return 'info';
    }
  }

  copyInviteLink(invite: InviteResponse): void {
    navigator.clipboard.writeText(invite.inviteLink).then(() => {
      this.copiedInviteId.set(invite.id);
      setTimeout(() => this.copiedInviteId.set(null), 2000);
    });
  }

  cancelInvite(invite: InviteResponse): void {
    this.confirmService.confirm({
      message: `Cancelar convite para ${invite.email}?`,
      header: 'Cancelar convite',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Cancelar convite',
      rejectLabel: 'Voltar',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.memberApi.cancelInvite(invite.id).subscribe({
          next: () => this.loadMembers(),
          error: (err) => console.error('Erro ao cancelar convite:', err),
        });
      },
    });
  }

  formatExpiresAt(isoDate: string): string {
    const date = new Date(isoDate);
    return date.toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
}
