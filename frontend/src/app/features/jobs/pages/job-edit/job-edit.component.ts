import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
  viewChild,
  computed,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { JobApiService } from '@features/jobs/services/job-api.service';
import { JobFormComponent } from '../../components/job-form/job-form.component';
import { JobSummarySidebarComponent } from '../../components/job-summary-sidebar/job-summary-sidebar.component';
import type { Job, JobFile, JobRequest } from '@features/jobs/models/job.model';

@Component({
  selector: 'app-job-edit',
  standalone: true,
  imports: [
    RouterLink,
    JobFormComponent,
    JobSummarySidebarComponent,
    ToastModule,
  ],
  templateUrl: './job-edit.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [MessageService],
})
export class JobEditComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly jobApi = inject(JobApiService);
  private readonly msg = inject(MessageService);

  readonly jobForm = viewChild(JobFormComponent);

  readonly job = signal<Job | null>(null);
  readonly existingFiles = signal<JobFile[]>([]);
  readonly loading = signal(false);
  readonly clientId = signal<number>(0);
  jobId = 0;

  readonly formGroup = computed(() => this.jobForm()?.form ?? null);

  ngOnInit(): void {
    const pm = this.route.snapshot.paramMap;
    this.jobId = Number(pm.get('id') ?? this.route.snapshot.params['id']);
    const cid = Number(pm.get('clientId'));
    if (cid && !Number.isNaN(cid)) this.clientId.set(cid);

    this.jobApi.getById(this.jobId).subscribe({
      next: (job) => {
        this.job.set(job);
        this.existingFiles.set(job.files);
        if (this.clientId() === 0) this.clientId.set(job.client.id);
      },
      error: (err) => {
        console.error('Erro ao carregar job:', err);
        this.msg.add({
          severity: 'error',
          summary: 'Erro',
          detail: 'Job não encontrado.',
        });
        this.router.navigate(['/clients']);
      },
    });
  }

  onFormSubmit(request: JobRequest): void {
    this.loading.set(true);
    this.jobApi.update(this.jobId, request).subscribe({
      next: () => {
        this.loading.set(false);
        this.msg.add({
          severity: 'success',
          summary: 'Job atualizado',
          detail: '',
        });
        this.router.navigate(['/clients', this.clientId(), 'jobs', this.jobId]);
      },
      error: (err) => {
        this.loading.set(false);
        const detail = err?.error?.message ?? 'Erro ao atualizar job.';
        this.msg.add({ severity: 'error', summary: 'Erro', detail });
      },
    });
  }

  onFileUploaded(file: JobFile): void {
    this.existingFiles.update((list) => [...list, file]);
  }

  onFileDeleted(fileId: number): void {
    this.existingFiles.update((list) => list.filter((f) => f.id !== fileId));
  }

  onSave(): void {
    const form = this.jobForm();
    if (form) form.onSubmit();
  }

  onCancel(): void {
    this.router.navigate(['/clients', this.clientId(), 'jobs', this.jobId]);
  }
}
