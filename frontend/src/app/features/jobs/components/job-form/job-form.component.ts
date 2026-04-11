import {
  ChangeDetectionStrategy,
  Component,
  inject,
  input,
  output,
  effect,
  signal,
} from '@angular/core';
import {
  FormBuilder,
  Validators,
  ReactiveFormsModule,
  FormGroup,
} from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { DatePickerModule } from 'primeng/datepicker';
import { ClientApiService } from '@features/clients/services/client-api.service';
import { MemberApiService } from '@features/members/services/member-api.service';
import { BriefingFieldsComponent } from '../briefing-fields/briefing-fields.component';
import {
  JobFileUploaderComponent,
  type UploaderMode,
} from '../job-file-uploader/job-file-uploader.component';
import type {
  Job,
  JobRequest,
  JobType,
  JobPriority,
  JobFile,
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

@Component({
  selector: 'app-job-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    InputTextModule,
    TextareaModule,
    SelectModule,
    DatePickerModule,
    BriefingFieldsComponent,
    JobFileUploaderComponent,
  ],
  templateUrl: './job-form.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JobFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly clientApi = inject(ClientApiService);
  private readonly memberApi = inject(MemberApiService);

  readonly mode = input.required<'create' | 'edit'>();
  readonly initialJob = input<Job | null>(null);
  readonly jobId = input<number | null>(null);
  readonly existingFiles = input<JobFile[]>([]);

  readonly submitted = output<JobRequest>();
  readonly filesStaged = output<File[]>();
  readonly fileUploaded = output<JobFile>();
  readonly fileDeleted = output<number>();

  readonly clientOptions = signal<{ label: string; value: number }[]>([]);
  readonly creativeOptions = signal<{ label: string; value: number }[]>([]);

  readonly typeOptions = [
    { label: 'Post Feed', value: 'POST_FEED' as JobType },
    { label: 'Stories', value: 'STORIES' as JobType },
    { label: 'Carrossel', value: 'CARROSSEL' as JobType },
    { label: 'Reels/Vídeo', value: 'REELS_VIDEO' as JobType },
    { label: 'Banner', value: 'BANNER' as JobType },
    { label: 'Logo', value: 'LOGO' as JobType },
    { label: 'Outros', value: 'OUTROS' as JobType },
  ];
  readonly priorityOptions = [
    { label: 'Baixa', value: 'BAIXA' as JobPriority },
    { label: 'Normal', value: 'NORMAL' as JobPriority },
    { label: 'Alta', value: 'ALTA' as JobPriority },
    { label: 'Urgente', value: 'URGENTE' as JobPriority },
  ];

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    clientId: this.fb.control<number | null>(null, [Validators.required]),
    type: this.fb.nonNullable.control<JobType>('POST_FEED', [
      Validators.required,
    ]),
    description: [''],
    deadline: this.fb.control<Date | null>(null),
    priority: this.fb.nonNullable.control<JobPriority>('NORMAL', [
      Validators.required,
    ]),
    assignedCreativeId: this.fb.control<number | null>(null),
    briefingData: this.fb.group({}),
  });

  readonly currentType = signal<JobType>('POST_FEED');

  constructor() {
    this.clientApi.list({ active: true }).subscribe({
      next: (clients) =>
        this.clientOptions.set(
          clients.map((c) => ({ label: c.name, value: c.id })),
        ),
      error: (err) => console.error('Erro ao carregar clientes:', err),
    });

    this.form.get('clientId')!.valueChanges.subscribe((clientId) => {
      if (clientId !== null) this.loadCreativesForClient(clientId);
      else this.creativeOptions.set([]);
    });

    this.form.get('type')!.valueChanges.subscribe((t) => {
      if (t) this.currentType.set(t);
    });

    effect(() => {
      const job = this.initialJob();
      if (job && this.mode() === 'edit') {
        this.form.patchValue({
          title: job.title,
          clientId: job.client.id,
          type: job.type,
          description: job.description ?? '',
          deadline: job.deadline ? new Date(job.deadline) : null,
          priority: job.priority,
          assignedCreativeId: job.assignedCreative?.id ?? null,
        });
        this.currentType.set(job.type);
        queueMicrotask(() => {
          (this.form.get('briefingData') as FormGroup).patchValue(
            job.briefingData,
          );
        });
      }
    });
  }

  private loadCreativesForClient(clientId: number): void {
    this.memberApi.list().subscribe({
      next: ({ members }) => {
        this.clientApi.getAssignedMembers(clientId).subscribe({
          next: (assignedIds) => {
            const options = members
              .filter((m) => m.role === 'CREATIVE' && assignedIds.includes(m.id))
              .map((m) => ({ label: m.userName, value: m.id }));
            this.creativeOptions.set(options);
          },
          error: (err) => console.error('Erro ao carregar criativos:', err),
        });
      },
      error: (err) => console.error('Erro ao carregar membros:', err),
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const request: JobRequest = {
      title: raw.title,
      clientId: raw.clientId!,
      type: raw.type,
      priority: raw.priority,
      briefingData: raw.briefingData as Record<string, unknown>,
    };
    if (raw.description) request.description = raw.description;
    if (raw.deadline)
      request.deadline = raw.deadline.toISOString().split('T')[0];
    if (raw.assignedCreativeId !== null)
      request.assignedCreativeId = raw.assignedCreativeId;
    this.submitted.emit(request);
  }

  getTypeLabel(): string {
    return TYPE_LABELS[this.currentType()];
  }

  getBriefingGroup(): FormGroup {
    return this.form.get('briefingData') as FormGroup;
  }

  getUploaderMode(): UploaderMode {
    return this.mode() === 'create' ? 'staging' : 'direct';
  }

  onFilesStaged(files: File[]): void {
    this.filesStaged.emit(files);
  }

  onFileUploaded(file: JobFile): void {
    this.fileUploaded.emit(file);
  }

  onFileDeleted(fileId: number): void {
    this.fileDeleted.emit(fileId);
  }
}
