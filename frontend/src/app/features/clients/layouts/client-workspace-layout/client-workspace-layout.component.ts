import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ClientApiService } from '@features/clients/services/client-api.service';
import type { Client } from '@features/clients/models/client.model';

@Component({
  selector: 'app-client-workspace-layout',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './client-workspace-layout.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    :host ::ng-deep .active-nav-item {
      background-color: #EEF2FF;
      color: #4F46E5;
      font-weight: 500;
    }
    :host ::ng-deep .active-nav-item:hover {
      background-color: #EEF2FF;
    }
    :host ::ng-deep .active-nav-item i {
      color: #6366F1;
    }
  `],
})
export class ClientWorkspaceLayoutComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly clientApi = inject(ClientApiService);

  readonly client = signal<Client | null>(null);
  readonly clientName = computed(() => this.client()?.name ?? '');

  ngOnInit(): void {
    const clientIdParam = this.route.snapshot.paramMap.get('clientId');
    const clientId = clientIdParam ? Number(clientIdParam) : NaN;
    if (!clientId || Number.isNaN(clientId)) {
      this.router.navigate(['/clients']);
      return;
    }

    this.clientApi.getById(clientId).subscribe({
      next: (client) => this.client.set(client),
      error: (err) => {
        console.error('Erro ao carregar cliente:', err);
        this.router.navigate(['/clients']);
      },
    });
  }
}
