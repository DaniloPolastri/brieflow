import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  effect,
  inject,
  input,
  output,
} from '@angular/core';
import { FormGroup } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { merge } from 'rxjs';
import type { JobPriority, JobType } from '@features/jobs/models/job.model';

const REQUIRED_LABELS: Record<string, string> = {
  title: 'Título',
  type: 'Tipo',
  priority: 'Prioridade',
};

const TYPE_LABELS: Record<JobType, string> = {
  POST_FEED: 'Post Feed',
  STORIES: 'Stories',
  CARROSSEL: 'Carrossel',
  REELS_VIDEO: 'Reels/Vídeo',
  BANNER: 'Banner',
  LOGO: 'Logo',
  OUTROS: 'Outros',
};

const PRIORITY_LABELS: Record<JobPriority, string> = {
  BAIXA: 'Baixa',
  NORMAL: 'Normal',
  ALTA: 'Alta',
  URGENTE: 'Urgente',
};

@Component({
  selector: 'app-job-summary-sidebar',
  standalone: true,
  imports: [ButtonModule],
  templateUrl: './job-summary-sidebar.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JobSummarySidebarComponent {
  readonly form = input.required<FormGroup>();
  readonly mode = input.required<'create' | 'edit'>();
  readonly clientName = input<string | null>(null);
  readonly creativeName = input<string | null>(null);
  readonly loading = input<boolean>(false);

  readonly save = output<void>();
  readonly cancel = output<void>();

  private readonly cdr = inject(ChangeDetectorRef);

  constructor() {
    // FormGroup reference is stable, so OnPush never re-renders when its
    // inner values change. Subscribe to valueChanges/statusChanges of the
    // current form and trigger markForCheck so the template getters re-run
    // against fresh state.
    effect((onCleanup) => {
      const f = this.form();
      const sub = merge(f.valueChanges, f.statusChanges).subscribe(() =>
        this.cdr.markForCheck(),
      );
      onCleanup(() => sub.unsubscribe());
    });
  }

  canSave(): boolean {
    return this.form().valid && !this.loading();
  }

  buttonLabel(): string {
    return this.mode() === 'create' ? 'Salvar Job' : 'Atualizar Job';
  }

  missingRequiredFields(): string[] {
    const f = this.form();
    return Object.keys(REQUIRED_LABELS)
      .filter((k) => f.get(k)?.invalid ?? false)
      .map((k) => REQUIRED_LABELS[k]);
  }

  getTitle(): string {
    return this.form().get('title')?.value || '—';
  }

  getTypeLabel(): string {
    const t = this.form().get('type')?.value as JobType | null;
    return t ? TYPE_LABELS[t] : '—';
  }

  getPriorityLabel(): string {
    const p = this.form().get('priority')?.value as JobPriority | null;
    return p ? PRIORITY_LABELS[p] : '—';
  }

  getDeadline(): string {
    const d = this.form().get('deadline')?.value;
    if (!d) return '—';
    const date = new Date(d);
    return date.toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }

  onSave(): void {
    this.save.emit();
  }

  onCancel(): void {
    this.cancel.emit();
  }
}
