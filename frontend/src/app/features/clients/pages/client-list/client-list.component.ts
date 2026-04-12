import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
  OnInit,
  OnDestroy,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { SelectButtonModule } from 'primeng/selectbutton';
import { MenuModule } from 'primeng/menu';
import { ConfirmationService, MenuItem } from 'primeng/api';
import { ClientApiService } from '@features/clients/services/client-api.service';
import { StorageService } from '@core/services/storage.service';
import { Client } from '@features/clients/models/client.model';
import { ClientFormDialogComponent } from '@features/clients/components/client-form-dialog/client-form-dialog.component';

type StatusFilter = 'ALL' | 'ACTIVE' | 'INACTIVE';

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
  selector: 'app-client-list',
  standalone: true,
  imports: [
    ButtonModule,
    InputTextModule,
    IconFieldModule,
    InputIconModule,
    ConfirmDialogModule,
    SelectButtonModule,
    MenuModule,
    FormsModule,
    RouterLink,
    ClientFormDialogComponent,
  ],
  templateUrl: './client-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [ConfirmationService],
})
export class ClientListComponent implements OnInit, OnDestroy {
  private readonly clientApi = inject(ClientApiService);
  private readonly storage = inject(StorageService);
  private readonly confirmService = inject(ConfirmationService);

  readonly clients = signal<Client[]>([]);
  readonly loading = signal(true);
  readonly showFormDialog = signal(false);
  readonly editingClient = signal<Client | null>(null);
  readonly searchTerm = signal('');
  readonly statusFilter = signal<StatusFilter>('ACTIVE');

  private readonly searchSubject = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  readonly currentUser = this.storage.getUser();
  readonly canManage = this.currentUser?.role === 'OWNER' || this.currentUser?.role === 'MANAGER';

  readonly statusOptions = [
    { label: 'Todos', value: 'ALL' },
    { label: 'Ativos', value: 'ACTIVE' },
    { label: 'Inativos', value: 'INACTIVE' },
  ];

  ngOnInit(): void {
    this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe((term) => {
        this.searchTerm.set(term);
        this.loadClients();
      });

    this.loadClients();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadClients(): void {
    this.loading.set(true);

    const params: { search?: string; active?: boolean } = {};

    const search = this.searchTerm();
    if (search) {
      params.search = search;
    }

    const filter = this.statusFilter();
    if (filter === 'ACTIVE') {
      params.active = true;
    } else if (filter === 'INACTIVE') {
      params.active = false;
    }

    this.clientApi.list(params).subscribe({
      next: (clients) => {
        this.clients.set(clients);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchSubject.next(value);
  }

  onStatusFilterChange(): void {
    this.loadClients();
  }

  openCreateDialog(): void {
    this.editingClient.set(null);
    this.showFormDialog.set(true);
  }

  openEditDialog(client: Client): void {
    this.editingClient.set(client);
    this.showFormDialog.set(true);
  }

  toggleActive(client: Client): void {
    const action = client.active ? 'desativar' : 'reativar';
    this.confirmService.confirm({
      message: `Deseja ${action} o cliente "${client.name}"?`,
      header: `${client.active ? 'Desativar' : 'Reativar'} cliente`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: client.active ? 'Desativar' : 'Reativar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: client.active ? 'p-button-danger' : 'p-button-success',
      accept: () => {
        this.clientApi.toggleActive(client.id).subscribe({
          next: () => this.loadClients(),
          error: (err) => {
            console.error('Erro ao alterar status do cliente:', err);
          },
        });
      },
    });
  }

  getMenuItems(client: Client): MenuItem[] {
    return [
      {
        label: 'Editar',
        icon: 'pi pi-pencil',
        command: () => this.openEditDialog(client),
      },
      {
        label: client.active ? 'Desativar' : 'Reativar',
        icon: client.active ? 'pi pi-ban' : 'pi pi-check-circle',
        command: () => this.toggleActive(client),
      },
    ];
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
}
