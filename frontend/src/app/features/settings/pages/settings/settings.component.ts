import { ChangeDetectionStrategy, Component, inject, signal, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { WorkspaceApiService } from '@features/settings/services/workspace-api.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [ReactiveFormsModule, InputTextModule, ButtonModule, MessageModule],
  templateUrl: './settings.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SettingsComponent implements OnInit {
  private readonly workspaceApi = inject(WorkspaceApiService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly successMessage = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
  });

  ngOnInit(): void {
    this.loading.set(true);
    this.workspaceApi.get().subscribe({
      next: workspace => {
        this.form.patchValue({ name: workspace.name });
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Erro ao carregar dados do workspace');
        this.loading.set(false);
      },
    });
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.successMessage.set(null);
    this.errorMessage.set(null);

    this.workspaceApi.update({ name: this.form.getRawValue().name }).subscribe({
      next: () => {
        this.saving.set(false);
        this.successMessage.set('Workspace atualizado com sucesso!');
      },
      error: err => {
        this.saving.set(false);
        this.errorMessage.set(err.error?.message ?? 'Erro ao atualizar workspace');
      },
    });
  }
}
