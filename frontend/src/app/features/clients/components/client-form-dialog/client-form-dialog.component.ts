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
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { ClientApiService } from '@features/clients/services/client-api.service';
import { Client, ClientRequest } from '@features/clients/models/client.model';

const ALLOWED_TYPES = ['image/jpeg', 'image/png'];
const MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB

@Component({
  selector: 'app-client-form-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, DialogModule, InputTextModule, ButtonModule],
  templateUrl: './client-form-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClientFormDialogComponent {
  readonly visible = model(false);
  readonly client = input<Client | null>(null);
  readonly saved = output<void>();

  private readonly clientApi = inject(ClientApiService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly logoFile = signal<File | null>(null);
  readonly logoPreview = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

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
      }
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

        if (file) {
          this.clientApi.uploadLogo(savedClient.id, file).subscribe({
            next: () => this.finishSave(),
            error: () => {
              this.errorMessage.set('Cliente salvo, mas erro ao enviar logo.');
              this.loading.set(false);
            },
          });
        } else if (logoRemoved) {
          this.clientApi.removeLogo(savedClient.id).subscribe({
            next: () => this.finishSave(),
            error: () => {
              this.errorMessage.set('Cliente salvo, mas erro ao remover logo.');
              this.loading.set(false);
            },
          });
        } else {
          this.finishSave();
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
  }

  private finishSave(): void {
    this.loading.set(false);
    this.saved.emit();
    this.visible.set(false);
  }
}
