import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
  OnInit,
  OnDestroy,
} from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil, forkJoin, of } from 'rxjs';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { SelectModule } from 'primeng/select';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { MenuModule } from 'primeng/menu';
import { TagModule } from 'primeng/tag';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MenuItem } from 'primeng/api';
import { JobApiService } from '@features/jobs/services/job-api.service';
import { ClientApiService } from '@features/clients/services/client-api.service';
import { MemberApiService } from '@features/members/services/member-api.service';
import { StorageService } from '@core/services/storage.service';
import type {
  JobListItem,
  JobType,
  JobPriority,
  JobListFilters,
} from '@features/jobs/models/job.model';

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
  selector: 'app-job-list',
  standalone: true,
  imports: [
    FormsModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    IconFieldModule,
    InputIconModule,
    SelectModule,
    ToggleSwitchModule,
    MenuModule,
    TagModule,
    ConfirmDialogModule,
  ],
  templateUrl: './job-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [ConfirmationService],
})
export class JobListComponent implements OnInit, OnDestroy {
  private readonly jobApi = inject(JobApiService);
  private readonly clientApi = inject(ClientApiService);
  private readonly memberApi = inject(MemberApiService);
  private readonly storage = inject(StorageService);
  private readonly confirmService = inject(ConfirmationService);
  private readonly router = inject(Router);

  readonly jobs = signal<JobListItem[]>([]);
  readonly loading = signal(true);
  readonly searchTerm = signal('');
  readonly showArchived = signal(false);
  readonly clientFilter = signal<number | null>(null);
  readonly typeFilter = signal<JobType | null>(null);
  readonly priorityFilter = signal<JobPriority | null>(null);
  readonly creativeFilter = signal<number | null>(null);

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

  private readonly searchSubject = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  readonly currentUser = this.storage.getUser();
  readonly canManage =
    this.currentUser?.role === 'OWNER' || this.currentUser?.role === 'MANAGER';

  ngOnInit(): void {
    this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe((term) => {
        this.searchTerm.set(term);
        this.loadJobs();
      });

    this.loadJobs();
    this.loadFilterOptions();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadJobs(): void {
    this.loading.set(true);
    const filters: JobListFilters = { archived: this.showArchived() };
    const s = this.searchTerm();
    if (s) filters.search = s;
    const c = this.clientFilter();
    if (c !== null) filters.clientId = c;
    const t = this.typeFilter();
    if (t) filters.type = t;
    const p = this.priorityFilter();
    if (p) filters.priority = p;
    const cr = this.creativeFilter();
    if (cr !== null) filters.assignedCreativeId = cr;

    this.jobApi.list(filters).subscribe({
      next: (jobs) => {
        this.jobs.set(jobs);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Erro ao carregar jobs:', err);
        this.loading.set(false);
      },
    });
  }

  private loadFilterOptions(): void {
    forkJoin({
      clients: this.clientApi.list({ active: true }),
      members: this.canManage
        ? this.memberApi.list()
        : of({ members: [], pendingInvites: [] }),
    }).subscribe({
      next: ({ clients, members }) => {
        this.clientOptions.set(
          clients.map((c) => ({ label: c.name, value: c.id })),
        );
        this.creativeOptions.set(
          members.members
            .filter((m) => m.role === 'CREATIVE')
            .map((m) => ({ label: m.userName, value: m.id })),
        );
      },
      error: (err) => {
        console.error('Erro ao carregar filtros:', err);
      },
    });
  }

  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchSubject.next(value);
  }

  onFilterChange(): void {
    this.loadJobs();
  }

  goToNew(): void {
    this.router.navigate(['/jobs/new']);
  }

  goToDetail(job: JobListItem): void {
    this.router.navigate(['/jobs', job.id]);
  }

  goToEdit(job: JobListItem): void {
    this.router.navigate(['/jobs', job.id, 'edit']);
  }

  archiveJob(job: JobListItem): void {
    this.confirmService.confirm({
      message: `Deseja arquivar o job "${job.code} — ${job.title}"?`,
      header: 'Arquivar job',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Arquivar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.jobApi.archive(job.id, true).subscribe({
          next: () => this.loadJobs(),
          error: (err) => console.error('Erro ao arquivar job:', err),
        });
      },
    });
  }

  unarchiveJob(job: JobListItem): void {
    this.jobApi.archive(job.id, false).subscribe({
      next: () => this.loadJobs(),
      error: (err) => console.error('Erro ao restaurar job:', err),
    });
  }

  getMenuItems(job: JobListItem): MenuItem[] {
    const items: MenuItem[] = [
      { label: 'Ver', icon: 'pi pi-eye', command: () => this.goToDetail(job) },
    ];
    if (this.canManage) {
      items.push(
        {
          label: 'Editar',
          icon: 'pi pi-pencil',
          command: () => this.goToEdit(job),
        },
        this.showArchived()
          ? {
              label: 'Restaurar',
              icon: 'pi pi-replay',
              command: () => this.unarchiveJob(job),
            }
          : {
              label: 'Arquivar',
              icon: 'pi pi-inbox',
              command: () => this.archiveJob(job),
            },
      );
    }
    return items;
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

  getTypeLabel(t: JobType): string {
    return TYPE_LABELS[t];
  }

  getPriorityLabel(p: JobPriority): string {
    return PRIORITY_LABELS[p];
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

  formatDeadline(deadline: string | null): string {
    if (!deadline) return '—';
    const d = new Date(deadline);
    return d.toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }
}
