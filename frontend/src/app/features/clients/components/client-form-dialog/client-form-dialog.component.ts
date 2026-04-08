import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  model,
  output,
  signal,
} from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators, FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { MultiSelectModule } from 'primeng/multiselect';
import { ClientApiService } from '@features/clients/services/client-api.service';
import { Client, ClientRequest } from '@features/clients/models/client.model';
import { MemberApiService } from '@features/members/services/member-api.service';
import { forkJoin } from 'rxjs';

const ALLOWED_TYPES = ['image/jpeg', 'image/png'];
const MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB

@Component({
  selector: 'app-client-form-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule, DialogModule, InputTextModule, ButtonModule, MultiSelectModule],
  templateUrl: './client-form-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClientFormDialogComponent {
  readonly visible = model(false);
  readonly client = input<Client | null>(null);
  readonly saved = output<void>();

  private readonly clientApi = inject(ClientApiService);
  private readonly memberApi = inject(MemberApiService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly logoFile = signal<File | null>(null);
  readonly logoPreview = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly availableMembers = signal<{ label: string; value: number }[]>([]);
  selectedMemberIds: number[] = [];

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    company: [''],
    email: ['', [Validators.email]],
    phone: [''],
  });

  readonly isEdit = computed(() => this.client() !== null);
  readonly dialogTitle = computed(() => (this.isEdit() ? 'Editar cliente' : 'Novo cliente'));

  constructor() {
    effect(() => {
      const c = this.client();
      if (c) {
        this.form.patchValue({
          name: c.name,
          company: c.company ?? '',
          email: c.email ?? '',
          phone: c.phone ?? '',
        });
        this.logoPreview.set(c.logoUrl);
        this.loadMembersForEdit(c.id);
      }
    });
  }

  private loadMembersForEdit(clientId: number): void {
    forkJoin({
      members: this.memberApi.list(),
      assigned: this.clientApi.getAssignedMembers(clientId),
    }).subscribe({
      next: ({ members, assigned }) => {
        const options = members.members
          .filter(m => m.role !== 'OWNER')
          .map(m => ({ label: m.userName, value: m.id }));
        this.availableMembers.set(options);
        this.selectedMemberIds = assigned;
      },
      error: () => {
        this.errorMessage.set('Erro ao carregar membros.');
      },
    });
  }

  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input?.files?.[0];
    if (!file) return;

    if (!ALLOWED_TYPES.includes(file.type)) {
      this.errorMessage.set('Formato inválido. Use JPG ou PNG.');
      this.logoFile.set(null);
      return;
    }

    if (file.size > MAX_FILE_SIZE) {
      this.errorMessage.set('Arquivo muito grande. Máximo 2MB.');
      this.logoFile.set(null);
      return;
    }

    this.errorMessage.set(null);
    this.logoFile.set(file);

    const reader = new FileReader();
    reader.onload = () => this.logoPreview.set(reader.result as string);
    reader.readAsDataURL(file);
  }

  removeLogo(): void {
    this.logoFile.set(null);
    this.logoPreview.set(null);
  }

  onSubmit(): void {
    if (this.form.invalid || this.loading()) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    const raw = this.form.getRawValue();
    const request: ClientRequest = { name: raw.name };

    if (raw.company) request.company = raw.company;
    if (raw.email) request.email = raw.email;
    if (raw.phone) request.phone = raw.phone;

    const client = this.client();
    const save$ = client
      ? this.clientApi.update(client.id, request)
      : this.clientApi.create(request);

    save$.subscribe({
      next: (savedClient) => {
        const file = this.logoFile();
        const logoRemoved = !this.logoPreview() && client?.logoUrl;

        const afterSave = () => {
          if (this.isEdit() && this.selectedMemberIds.length > 0) {
            this.clientApi.assignMembers(savedClient.id, this.selectedMemberIds).subscribe({
              next: () => this.finishSave(),
              error: () => {
                this.errorMessage.set('Cliente salvo, mas erro ao vincular membros.');
                this.loading.set(false);
              },
            });
          } else if (this.isEdit()) {
            this.clientApi.assignMembers(savedClient.id, []).subscribe({
              next: () => this.finishSave(),
              error: () => {
                this.errorMessage.set('Cliente salvo, mas erro ao vincular membros.');
                this.loading.set(false);
              },
            });
          } else {
            this.finishSave();
          }
        };

        if (file) {
          this.clientApi.uploadLogo(savedClient.id, file).subscribe({
            next: () => afterSave(),
            error: () => {
              this.errorMessage.set('Cliente salvo, mas erro ao enviar logo.');
              this.loading.set(false);
            },
          });
        } else if (logoRemoved) {
          this.clientApi.removeLogo(savedClient.id).subscribe({
            next: () => afterSave(),
            error: () => {
              this.errorMessage.set('Cliente salvo, mas erro ao remover logo.');
              this.loading.set(false);
            },
          });
        } else {
          afterSave();
        }
      },
      error: () => {
        this.errorMessage.set('Erro ao salvar cliente. Tente novamente.');
        this.loading.set(false);
      },
    });
  }

  onHide(): void {
    this.form.reset({ name: '', company: '', email: '', phone: '' });
    this.logoFile.set(null);
    this.logoPreview.set(null);
    this.errorMessage.set(null);
    this.availableMembers.set([]);
    this.selectedMemberIds = [];
  }

  private finishSave(): void {
    this.loading.set(false);
    this.saved.emit();
    this.visible.set(false);
  }
}
