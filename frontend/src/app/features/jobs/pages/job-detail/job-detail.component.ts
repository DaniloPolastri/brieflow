import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageService, ConfirmationService } from 'primeng/api';
import { JobApiService } from '@features/jobs/services/job-api.service';
import { JobFileUploaderComponent } from '../../components/job-file-uploader/job-file-uploader.component';
import { StorageService } from '@core/services/storage.service';
import { BRIEFING_SCHEMAS } from '@features/jobs/models/briefing-schemas';
import type {
  Job,
  JobPriority,
  JobStatus,
  JobType,
} from '@features/jobs/models/job.model';

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

const STATUS_LABELS: Record<JobStatus, string> = {
  NOVO: 'Novo',
  EM_CRIACAO: 'Em criação',
  REVISAO_INTERNA: 'Revisão interna',
  AGUARDANDO_APROVACAO: 'Aguardando aprovação',
  APROVADO: 'Aprovado',
  PUBLICADO: 'Publicado',
};

const AVATAR_COLORS = [
  'bg-indigo-100 text-indigo-700',
  'bg-emerald-100 text-emerald-700',
  'bg-amber-100 text-amber-700',
  'bg-rose-100 text-rose-700',
  'bg-sky-100 text-sky-700',
  'bg-purple-100 text-purple-700',
  'bg-teal-100 text-teal-700',
  'bg-orange-100 text-orange-700',
];

@Component({
  selector: 'app-job-detail',
  standalone: true,
  imports: [
    RouterLink,
    ButtonModule,
    TagModule,
    ToastModule,
    ConfirmDialogModule,
    JobFileUploaderComponent,
  ],
  templateUrl: './job-detail.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [MessageService, ConfirmationService],
})
export class JobDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(JobApiService);
  private readonly storage = inject(StorageService);
  private readonly msg = inject(MessageService);
  private readonly confirmService = inject(ConfirmationService);

  readonly job = signal<Job | null>(null);
  readonly clientId = signal<number>(0);
  jobId = 0;
  readonly currentUser = this.storage.getUser();
  readonly canManage =
    this.currentUser?.role === 'OWNER' || this.currentUser?.role === 'MANAGER';

  ngOnInit(): void {
    const pm = this.route.snapshot.paramMap;
    this.jobId = Number(pm.get('id') ?? this.route.snapshot.params['id']);
    const cid = Number(pm.get('clientId'));
    if (cid && !Number.isNaN(cid)) this.clientId.set(cid);
    this.loadJob();
  }

  private loadJob(): void {
    this.api.getById(this.jobId).subscribe({
      next: (job) => {
        this.job.set(job);
        if (this.clientId() === 0) this.clientId.set(job.client.id);
      },
      error: (err) => {
        console.error('Erro ao carregar job:', err);
        this.msg.add({
          severity: 'error',
          summary: 'Erro',
          detail: 'Job não encontrado.',
        });
        const cid = this.clientId();
        if (cid) this.router.navigate(['/clients', cid, 'jobs']);
        else this.router.navigate(['/clients']);
      },
    });
  }

  briefingFields(): Array<{ label: string; value: string }> {
    const job = this.job();
    if (!job) return [];
    return BRIEFING_SCHEMAS[job.type].map((field) => {
      const raw = job.briefingData[field.key];
      let value: string;
      if (raw === null || raw === undefined || raw === '') {
        value = '—';
      } else if (Array.isArray(raw)) {
        value = (raw as unknown[]).map((v, i) => `#${i + 1}: ${v}`).join('\n');
      } else {
        value = String(raw);
      }
      return { label: field.label, value };
    });
  }

  goToEdit(): void {
    this.router.navigate(['/clients', this.clientId(), 'jobs', this.jobId, 'edit']);
  }

  archive(): void {
    const job = this.job();
    if (!job) return;
    this.confirmService.confirm({
      message: `Deseja arquivar o job "${job.code}"?`,
      header: 'Arquivar',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Arquivar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.api.archive(this.jobId, true).subscribe({
          next: (updated) => {
            this.job.set(updated);
            this.msg.add({
              severity: 'success',
              summary: 'Job arquivado',
              detail: '',
            });
          },
          error: (err) => {
            console.error('Erro ao arquivar job:', err);
            this.msg.add({
              severity: 'error',
              summary: 'Erro',
              detail: 'Falha ao arquivar.',
            });
          },
        });
      },
    });
  }

  onFileUploaded(): void {
    this.loadJob();
  }

  onFileDeleted(): void {
    this.loadJob();
  }

  getTypeLabel(t: JobType): string {
    return TYPE_LABELS[t];
  }

  getPriorityLabel(p: JobPriority): string {
    return PRIORITY_LABELS[p];
  }

  getStatusLabel(s: JobStatus): string {
    return STATUS_LABELS[s];
  }

  getPrioritySeverity(
    p: JobPriority,
  ): 'info' | 'success' | 'warn' | 'danger' | 'secondary' {
    switch (p) {
      case 'URGENTE':
        return 'danger';
      case 'ALTA':
        return 'warn';
      case 'NORMAL':
        return 'info';
      case 'BAIXA':
        return 'secondary';
    }
  }

  getStatusSeverity(
    s: JobStatus,
  ): 'info' | 'success' | 'warn' | 'danger' | 'secondary' {
    switch (s) {
      case 'APROVADO':
      case 'PUBLICADO':
        return 'success';
      case 'AGUARDANDO_APROVACAO':
        return 'warn';
      case 'REVISAO_INTERNA':
        return 'info';
      case 'EM_CRIACAO':
        return 'info';
      case 'NOVO':
        return 'secondary';
    }
  }

  isOverdue(job: Job): boolean {
    if (!job.deadline) return false;
    const done = job.status === 'APROVADO' || job.status === 'PUBLICADO';
    if (done) return false;
    return new Date(job.deadline).getTime() < Date.now();
  }

  formatDate(date: string | null): string {
    if (!date) return '—';
    const d = new Date(date);
    return d.toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }

  getInitials(name: string): string {
    return name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .substring(0, 2)
      .toUpperCase();
  }

  getAvatarColor(name: string): string {
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    const index = Math.abs(hash) % AVATAR_COLORS.length;
    return AVATAR_COLORS[index];
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  isImage(mimeType: string): boolean {
    return mimeType.startsWith('image/');
  }

  getFileIcon(mimeType: string): string {
    if (mimeType.startsWith('image/')) return 'pi pi-image';
    if (mimeType.startsWith('video/')) return 'pi pi-video';
    if (mimeType === 'application/pdf') return 'pi pi-file-pdf';
    return 'pi pi-file';
  }
}
