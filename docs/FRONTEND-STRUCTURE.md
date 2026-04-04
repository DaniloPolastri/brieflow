# BriefFlow — Frontend Structure

**Stack:** Angular (Latest) + Standalone Components + Signals + PrimeNG + Tailwind  
**Padrão:** Arquitetura por domínio (feature-based)  
**Referência:** ESTRUTURA-FRONTEND.md + angular-best-practices.md + angular-style-guide.md + folder-rules.md

---

## Estrutura de Pastas

```text
src/
│
├── main.ts
├── index.html
├── styles.css                          # Tailwind imports + PrimeNG theme overrides
│
├── app/
│   ├── app.config.ts                   # Providers, interceptors, router config
│   ├── app.routes.ts                   # Rotas raiz com lazy loading
│   └── app.component.ts               # Root component
│
├── core/                               # Singleton, global, uma única instância
│   │
│   ├── services/
│   │   ├── api.service.ts              # HttpClient wrapper com base URL
│   │   ├── auth.service.ts             # Login, registro, token management
│   │   ├── storage.service.ts          # LocalStorage wrapper para tokens
│   │   └── notification.service.ts     # Toast notifications (PrimeNG)
│   │
│   ├── guards/
│   │   ├── auth.guard.ts              # Redireciona se não autenticado
│   │   └── role.guard.ts             # Verifica role (owner/manager/creative)
│   │
│   ├── interceptors/
│   │   └── auth.interceptor.ts        # Adiciona JWT no header + refresh token logic
│   │
│   └── models/
│       ├── user.model.ts
│       └── api-response.model.ts
│
├── shared/                             # Componentes reutilizáveis
│   │
│   ├── components/
│   │   ├── page-header/               # Título da página + breadcrumb + ações
│   │   │   ├── page-header.component.ts
│   │   │   └── page-header.component.html
│   │   │
│   │   ├── empty-state/              # Estado vazio com ícone + mensagem + CTA
│   │   │   ├── empty-state.component.ts
│   │   │   └── empty-state.component.html
│   │   │
│   │   ├── file-upload/              # Componente de upload drag & drop
│   │   │   ├── file-upload.component.ts
│   │   │   └── file-upload.component.html
│   │   │
│   │   ├── status-badge/             # Badge colorido por status
│   │   │   └── status-badge.component.ts
│   │   │
│   │   ├── priority-badge/           # Badge de prioridade
│   │   │   └── priority-badge.component.ts
│   │   │
│   │   ├── avatar/                   # Avatar com iniciais ou foto
│   │   │   └── avatar.component.ts
│   │   │
│   │   └── confirm-dialog/           # Modal de confirmação genérico
│   │       └── confirm-dialog.component.ts
│   │
│   ├── directives/
│   │   └── click-outside.directive.ts
│   │
│   ├── pipes/
│   │   ├── relative-time.pipe.ts     # "há 2 horas", "ontem"
│   │   └── truncate.pipe.ts
│   │
│   └── utils/
│       ├── date.utils.ts
│       └── file.utils.ts
│
├── features/                          # Domínios do sistema ⭐
│   │
│   ├── auth/
│   │   ├── pages/
│   │   │   ├── login/
│   │   │   │   ├── login.component.ts
│   │   │   │   └── login.component.html
│   │   │   │
│   │   │   ├── register/
│   │   │   │   ├── register.component.ts
│   │   │   │   └── register.component.html
│   │   │   │
│   │   │   └── accept-invite/
│   │   │       ├── accept-invite.component.ts
│   │   │       └── accept-invite.component.html
│   │   │
│   │   ├── services/
│   │   │   └── auth-api.service.ts
│   │   │
│   │   ├── models/
│   │   │   ├── login.model.ts
│   │   │   └── register.model.ts
│   │   │
│   │   └── auth.routes.ts
│   │
│   ├── clients/
│   │   ├── pages/
│   │   │   ├── client-list/
│   │   │   │   ├── client-list.component.ts
│   │   │   │   └── client-list.component.html
│   │   │   │
│   │   │   └── client-form/
│   │   │       ├── client-form.component.ts
│   │   │       └── client-form.component.html
│   │   │
│   │   ├── services/
│   │   │   └── client-api.service.ts
│   │   │
│   │   ├── models/
│   │   │   └── client.model.ts
│   │   │
│   │   └── clients.routes.ts
│   │
│   ├── jobs/
│   │   ├── pages/
│   │   │   ├── job-create/
│   │   │   │   ├── job-create.component.ts
│   │   │   │   └── job-create.component.html
│   │   │   │
│   │   │   └── job-detail/
│   │   │       ├── job-detail.component.ts
│   │   │       └── job-detail.component.html
│   │   │
│   │   ├── components/
│   │   │   ├── briefing-form/
│   │   │   │   ├── briefing-form.component.ts
│   │   │   │   └── briefing-form.component.html
│   │   │   │
│   │   │   ├── job-files/
│   │   │   │   ├── job-files.component.ts
│   │   │   │   └── job-files.component.html
│   │   │   │
│   │   │   └── job-history/
│   │   │       ├── job-history.component.ts
│   │   │       └── job-history.component.html
│   │   │
│   │   ├── services/
│   │   │   └── job-api.service.ts
│   │   │
│   │   ├── models/
│   │   │   ├── job.model.ts
│   │   │   ├── briefing.model.ts
│   │   │   └── job-file.model.ts
│   │   │
│   │   └── jobs.routes.ts
│   │
│   ├── kanban/
│   │   ├── pages/
│   │   │   └── kanban-board/
│   │   │       ├── kanban-board.component.ts
│   │   │       └── kanban-board.component.html
│   │   │
│   │   ├── components/
│   │   │   ├── kanban-column/
│   │   │   │   ├── kanban-column.component.ts
│   │   │   │   └── kanban-column.component.html
│   │   │   │
│   │   │   └── kanban-card/
│   │   │       ├── kanban-card.component.ts
│   │   │       └── kanban-card.component.html
│   │   │
│   │   ├── services/
│   │   │   └── kanban-api.service.ts
│   │   │
│   │   ├── models/
│   │   │   └── kanban.model.ts
│   │   │
│   │   └── kanban.routes.ts
│   │
│   ├── dashboard/
│   │   ├── pages/
│   │   │   └── dashboard/
│   │   │       ├── dashboard.component.ts
│   │   │       └── dashboard.component.html
│   │   │
│   │   ├── components/
│   │   │   ├── stats-cards/
│   │   │   │   └── stats-cards.component.ts
│   │   │   │
│   │   │   ├── jobs-by-status-chart/
│   │   │   │   └── jobs-by-status-chart.component.ts
│   │   │   │
│   │   │   └── overdue-jobs-list/
│   │   │       └── overdue-jobs-list.component.ts
│   │   │
│   │   ├── services/
│   │   │   └── dashboard-api.service.ts
│   │   │
│   │   ├── models/
│   │   │   └── dashboard.model.ts
│   │   │
│   │   └── dashboard.routes.ts
│   │
│   ├── members/
│   │   ├── pages/
│   │   │   └── member-list/
│   │   │       ├── member-list.component.ts
│   │   │       └── member-list.component.html
│   │   │
│   │   ├── components/
│   │   │   └── invite-member-dialog/
│   │   │       └── invite-member-dialog.component.ts
│   │   │
│   │   ├── services/
│   │   │   └── member-api.service.ts
│   │   │
│   │   ├── models/
│   │   │   └── member.model.ts
│   │   │
│   │   └── members.routes.ts
│   │
│   ├── approval/                       # Portal público (sem auth)
│   │   ├── pages/
│   │   │   └── approval-page/
│   │   │       ├── approval-page.component.ts
│   │   │       └── approval-page.component.html
│   │   │
│   │   ├── components/
│   │   │   ├── approval-viewer/
│   │   │   │   └── approval-viewer.component.ts
│   │   │   │
│   │   │   └── revision-form/
│   │   │       └── revision-form.component.ts
│   │   │
│   │   ├── services/
│   │   │   └── approval-api.service.ts
│   │   │
│   │   ├── models/
│   │   │   └── approval.model.ts
│   │   │
│   │   └── approval.routes.ts
│   │
│   └── settings/
│       ├── pages/
│       │   └── settings/
│       │       ├── settings.component.ts
│       │       └── settings.component.html
│       │
│       ├── services/
│       │   └── settings-api.service.ts
│       │
│       └── settings.routes.ts
│
├── layout/                             # Layout da aplicação
│   │
│   ├── main-layout/
│   │   ├── main-layout.component.ts
│   │   ├── main-layout.component.html
│   │   └── main-layout.component.css
│   │
│   ├── sidebar/
│   │   ├── sidebar.component.ts
│   │   └── sidebar.component.html
│   │
│   ├── topbar/
│   │   ├── topbar.component.ts
│   │   └── topbar.component.html
│   │
│   └── public-layout/                 # Layout para páginas públicas (approval, auth)
│       ├── public-layout.component.ts
│       └── public-layout.component.html
│
├── environments/
│   ├── environment.ts
│   └── environment.prod.ts
│
└── assets/
    ├── images/
    └── icons/
```

---

## Regras de Dependência

```text
core    → Nunca depende de features ou shared
shared  → Pode ser usado por features e layout
features → Pode ser usado apenas por features (entre si NÃO — cada feature é independente)
layout  → Usa shared e core
```

---

## Exemplos de Código

### MODEL — job.model.ts

```typescript
export interface Job {
  id: number;
  code: string;
  title: string;
  type: JobType;
  priority: JobPriority;
  clientName: string;
  assigneeName: string | null;
  statusName: string;
  statusId: number;
  dueDate: string | null;
  isOverdue: boolean;
  briefing: Briefing;
  files: JobFile[];
  createdAt: string;
  updatedAt: string;
}

export interface JobListItem {
  id: number;
  code: string;
  title: string;
  type: JobType;
  priority: JobPriority;
  clientName: string;
  assigneeName: string | null;
  statusName: string;
  statusId: number;
  dueDate: string | null;
  isOverdue: boolean;
}

export interface CreateJobRequest {
  title: string;
  type: JobType;
  priority: JobPriority;
  clientId: number;
  assigneeId?: number;
  dueDate?: string;
  briefing: Briefing;
}

export enum JobType {
  POST_FEED = 'POST_FEED',
  STORIES = 'STORIES',
  CARROSSEL = 'CARROSSEL',
  REELS_VIDEO = 'REELS_VIDEO',
  BANNER = 'BANNER',
  LOGO = 'LOGO',
  OUTROS = 'OUTROS'
}

export enum JobPriority {
  BAIXA = 'BAIXA',
  NORMAL = 'NORMAL',
  ALTA = 'ALTA',
  URGENTE = 'URGENTE'
}
```

---

### SERVICE — job-api.service.ts

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Job, JobListItem, CreateJobRequest } from '../models/job.model';

@Injectable({
  providedIn: 'root'
})
export class JobApiService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/jobs`;

  list(filters?: { clientId?: number; assigneeId?: number; statusId?: number }): Observable<JobListItem[]> {
    let params = new HttpParams();
    if (filters?.clientId) params = params.set('clientId', filters.clientId);
    if (filters?.assigneeId) params = params.set('assigneeId', filters.assigneeId);
    if (filters?.statusId) params = params.set('statusId', filters.statusId);

    return this.http.get<JobListItem[]>(this.baseUrl, { params });
  }

  getById(id: number): Observable<Job> {
    return this.http.get<Job>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateJobRequest): Observable<Job> {
    return this.http.post<Job>(this.baseUrl, request);
  }

  moveStatus(jobId: number, statusId: number): Observable<Job> {
    return this.http.patch<Job>(`${this.baseUrl}/${jobId}/move`, { statusId });
  }

  uploadFile(jobId: number, file: File): Observable<Job> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Job>(`${this.baseUrl}/${jobId}/files`, formData);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
```

---

### COMPONENT — kanban-card.component.ts

```typescript
import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { JobListItem, JobPriority } from '../../models/job.model';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge/status-badge.component';
import { PriorityBadgeComponent } from '../../../../shared/components/priority-badge/priority-badge.component';
import { AvatarComponent } from '../../../../shared/components/avatar/avatar.component';
import { RelativeTimePipe } from '../../../../shared/pipes/relative-time.pipe';

@Component({
  selector: 'app-kanban-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [StatusBadgeComponent, PriorityBadgeComponent, AvatarComponent, RelativeTimePipe],
  template: `
    <div
      class="bg-white border border-gray-200 rounded-lg p-3 cursor-pointer
             hover:shadow-sm transition-shadow duration-150"
      [class.border-l-red-500]="job().isOverdue"
      [class.border-l-4]="job().isOverdue"
      (click)="cardClick.emit(job())"
    >
      <div class="flex items-center justify-between mb-2">
        <span class="text-xs font-mono text-gray-400">{{ job().code }}</span>
        <app-priority-badge [priority]="job().priority" />
      </div>

      <h4 class="text-sm font-semibold text-gray-900 mb-2 line-clamp-2">
        {{ job().title }}
      </h4>

      <div class="flex items-center justify-between">
        <span class="text-xs text-gray-500">{{ job().clientName }}</span>
        @if (job().assigneeName) {
          <app-avatar [name]="job().assigneeName!" size="sm" />
        }
      </div>

      @if (job().dueDate) {
        <div class="mt-2 text-xs" [class.text-red-500]="job().isOverdue" [class.text-gray-400]="!job().isOverdue">
          {{ job().dueDate | relativeTime }}
        </div>
      }
    </div>
  `
})
export class KanbanCardComponent {

  readonly job = input.required<JobListItem>();
  readonly cardClick = output<JobListItem>();

}
```

---

### INTERCEPTOR — auth.interceptor.ts

```typescript
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { StorageService } from '../services/storage.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {

  const storage = inject(StorageService);
  const router = inject(Router);

  // Skip auth header for public endpoints
  if (req.url.includes('/api/v1/auth/') || req.url.includes('/api/v1/approval/')) {
    return next(req);
  }

  const token = storage.getAccessToken();

  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        storage.clearTokens();
        router.navigate(['/auth/login']);
      }
      return throwError(() => error);
    })
  );
};
```

---

### GUARD — auth.guard.ts

```typescript
import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {

  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  router.navigate(['/auth/login']);
  return false;
};
```

---

### ROUTES — app.routes.ts

```typescript
import { Routes } from '@angular/router';
import { authGuard } from '../core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'auth',
    loadChildren: () => import('../features/auth/auth.routes').then(m => m.AUTH_ROUTES)
  },
  {
    path: 'approve/:token',
    loadChildren: () => import('../features/approval/approval.routes').then(m => m.APPROVAL_ROUTES)
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('../layout/main-layout/main-layout.component').then(m => m.MainLayoutComponent),
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        loadChildren: () => import('../features/dashboard/dashboard.routes').then(m => m.DASHBOARD_ROUTES)
      },
      {
        path: 'kanban',
        loadChildren: () => import('../features/kanban/kanban.routes').then(m => m.KANBAN_ROUTES)
      },
      {
        path: 'jobs',
        loadChildren: () => import('../features/jobs/jobs.routes').then(m => m.JOBS_ROUTES)
      },
      {
        path: 'clients',
        loadChildren: () => import('../features/clients/clients.routes').then(m => m.CLIENTS_ROUTES)
      },
      {
        path: 'members',
        loadChildren: () => import('../features/members/members.routes').then(m => m.MEMBERS_ROUTES)
      },
      {
        path: 'settings',
        loadChildren: () => import('../features/settings/settings.routes').then(m => m.SETTINGS_ROUTES)
      }
    ]
  },
  {
    path: '**',
    redirectTo: ''
  }
];
```

---

### APP CONFIG — app.config.ts

```typescript
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { routes } from './app.routes';
import { authInterceptor } from '../core/interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimationsAsync()
  ]
};
```

---

## Fluxo de Dados

```text
Component (user interaction)
    ↓
Service (HttpClient call)
    ↓ HTTP
Backend (Spring Boot API)
    ↓
Service (Observable response)
    ↓
Component (signal update → template re-render)
```

---

## Convenções Angular

- **Standalone Components** — Não usar NgModules
- **Signals** — Para state management local
- **input() / output()** — Em vez de @Input / @Output decorators
- **computed()** — Para state derivado
- **ChangeDetectionStrategy.OnPush** — Em todos os componentes
- **inject()** — Em vez de constructor injection
- **@if / @for / @switch** — Em vez de *ngIf / *ngFor / *ngSwitch
- **Reactive Forms** — Em vez de Template-driven
- **class bindings** — Em vez de ngClass
- **Lazy loading** — Para todas as features
- **Vitest** — Para testes unitários (funções globais, sem import)
