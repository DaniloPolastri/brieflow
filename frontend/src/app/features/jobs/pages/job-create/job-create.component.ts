import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
  viewChild,
  computed,
  OnInit,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { JobApiService } from '@features/jobs/services/job-api.service';
import { JobFormComponent } from '../../components/job-form/job-form.component';
import { JobSummarySidebarComponent } from '../../components/job-summary-sidebar/job-summary-sidebar.component';
import type { JobRequest } from '@features/jobs/models/job.model';

@Component({
  selector: 'app-job-create',
  standalone: true,
  imports: [
    RouterLink,
    JobFormComponent,
    JobSummarySidebarComponent,
    ToastModule,
  ],
  templateUrl: './job-create.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [MessageService],
})
export class JobCreateComponent implements OnInit {
  private readonly jobApi = inject(JobApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly msg = inject(MessageService);

  readonly jobForm = viewChild(JobFormComponent);

  readonly loading = signal(false);
  readonly stagedFiles = signal<File[]>([]);
  readonly clientId = signal<number>(0);

  readonly formGroup = computed(() => this.jobForm()?.form ?? null);

  ngOnInit(): void {
    const cid = Number(this.route.snapshot.paramMap.get('clientId'));
    if (!cid || Number.isNaN(cid)) {
      console.error('clientId ausente na rota de criação de job');
      this.router.navigate(['/clients']);
      return;
    }
    this.clientId.set(cid);
  }

  onFormSubmit(request: JobRequest): void {
    this.loading.set(true);
    this.jobApi.create(request).subscribe({
      next: (job) => {
        this.loading.set(false);
        this.msg.add({
          severity: 'success',
          summary: 'Job criado',
          detail: job.code,
        });
        this.uploadStagedFiles(job.id);
        this.router.navigate(['/clients', this.clientId(), 'jobs', job.id]);
      },
      error: (err) => {
        this.loading.set(false);
        const detail =
          err?.error?.message ?? 'Erro ao criar job. Tente novamente.';
        this.msg.add({ severity: 'error', summary: 'Erro', detail });
      },
    });
  }

  private uploadStagedFiles(jobId: number): void {
    const files = this.stagedFiles();
    for (const f of files) {
      this.jobApi.uploadFile(jobId, f).subscribe({
        error: (err) => console.error('Erro ao enviar arquivo:', err),
      });
    }
  }

  onFilesStaged(files: File[]): void {
    this.stagedFiles.update((list) => [...list, ...files]);
  }

  onSave(): void {
    const form = this.jobForm();
    if (form) form.onSubmit();
  }

  onCancel(): void {
    this.router.navigate(['/clients', this.clientId(), 'jobs']);
  }
}
