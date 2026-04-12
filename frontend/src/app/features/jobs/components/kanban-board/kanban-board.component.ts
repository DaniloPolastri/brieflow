import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  computed,
  signal,
  inject,
  OnInit,
  OnDestroy,
  effect,
} from '@angular/core';
import { Subscription } from 'rxjs';
import { CdkDropListGroup, CdkDragDrop } from '@angular/cdk/drag-drop';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MessageService } from 'primeng/api';
import { KanbanColumnComponent } from '../kanban-column/kanban-column.component';
import { JobApiService } from '../../services/job-api.service';
import { JobSseService } from '../../services/job-sse.service';
import {
  KANBAN_COLUMNS,
  KanbanColumn,
  JobListItem,
  JobStatus,
  JobStatusEvent,
} from '../../models/job.model';

@Component({
  selector: 'app-kanban-board',
  standalone: true,
  imports: [CdkDropListGroup, KanbanColumnComponent, ConfirmDialogModule, ToastModule],
  templateUrl: './kanban-board.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [ConfirmationService, MessageService],
})
export class KanbanBoardComponent implements OnInit, OnDestroy {
  private readonly jobApi = inject(JobApiService);
  private readonly sseService = inject(JobSseService);
  private readonly confirmService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  readonly clientId = input.required<number>();
  readonly jobs = input.required<JobListItem[]>();
  readonly currentUserId = input<number | null>(null);
  readonly canManage = input<boolean>(false);
  readonly jobClicked = output<JobListItem>();
  readonly statusChanged = output<void>();

  readonly columns: KanbanColumn[] = KANBAN_COLUMNS;
  readonly columnIds: string[] = KANBAN_COLUMNS.map(c => c.status);

  // Internal writable copy for optimistic updates
  readonly internalJobs = signal<JobListItem[]>([]);

  private sseSub: Subscription | null = null;

  constructor() {
    // Sync external jobs input into internal signal
    effect(() => {
      this.internalJobs.set([...this.jobs()]);
    });
  }

  readonly columnJobs = computed(() => {
    const jobs = this.internalJobs();
    const grouped: Record<JobStatus, JobListItem[]> = {
      NOVO: [],
      EM_CRIACAO: [],
      REVISAO_INTERNA: [],
      AGUARDANDO_APROVACAO: [],
      APROVADO: [],
      PUBLICADO: [],
    };
    for (const job of jobs) {
      if (grouped[job.status]) {
        grouped[job.status].push(job);
      }
    }
    return grouped;
  });

  readonly canDragFn = computed(() => {
    const manage = this.canManage();
    const userId = this.currentUserId();
    return (job: JobListItem): boolean => {
      if (manage) return true;
      return job.assignedCreativeId === userId;
    };
  });

  ngOnInit(): void {
    this.connectSse();
  }

  ngOnDestroy(): void {
    this.sseSub?.unsubscribe();
    this.sseService.disconnect();
  }

  private connectSse(): void {
    this.sseSub = this.sseService.connect(this.clientId()).subscribe((event: JobStatusEvent) => {
      this.applyRemoteStatusChange(event);
    });
  }

  private applyRemoteStatusChange(event: JobStatusEvent): void {
    this.internalJobs.update(jobs =>
      jobs.map(j =>
        j.id === event.jobId ? { ...j, status: event.newStatus } : j,
      ),
    );
  }

  onColumnDrop(event: CdkDragDrop<JobListItem[]>): void {
    if (event.previousContainer === event.container) return;

    const job: JobListItem = event.item.data;
    const targetStatus = event.container.id as JobStatus;

    if (job.status === targetStatus) return;

    this.onJobDrop(job.id, targetStatus, false);
  }

  onJobDrop(jobId: number, targetStatus: JobStatus, confirm: boolean): void {
    const jobs = this.internalJobs();
    const job = jobs.find(j => j.id === jobId);
    if (!job) return;

    const previousStatus = job.status;

    // Optimistic update
    this.internalJobs.update(list =>
      list.map(j => (j.id === jobId ? { ...j, status: targetStatus } : j)),
    );

    this.jobApi.updateStatus(jobId, targetStatus, confirm).subscribe({
      next: (response) => {
        if (response.skippedSteps && !response.applied) {
          // Revert optimistic update first
          this.internalJobs.update(list =>
            list.map(j => (j.id === jobId ? { ...j, status: previousStatus } : j)),
          );

          // Show confirmation dialog for skip-step
          this.confirmService.confirm({
            message: 'Este job vai pular etapas do fluxo. Deseja continuar?',
            header: 'Pular etapas',
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: 'Continuar',
            rejectLabel: 'Cancelar',
            acceptButtonStyleClass: 'p-button-warning',
            accept: () => {
              this.onJobDrop(jobId, targetStatus, true);
            },
          });
        } else if (response.applied) {
          this.statusChanged.emit();
        }
      },
      error: () => {
        // Revert optimistic update
        this.internalJobs.update(list =>
          list.map(j => (j.id === jobId ? { ...j, status: previousStatus } : j)),
        );
        this.messageService.add({
          severity: 'error',
          summary: 'Erro',
          detail: 'Não foi possível atualizar o status do job.',
          life: 3000,
        });
      },
    });
  }

  onCardClick(job: JobListItem): void {
    this.jobClicked.emit(job);
  }
}
