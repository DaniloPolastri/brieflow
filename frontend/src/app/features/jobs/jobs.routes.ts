import { Routes } from '@angular/router';
import { roleGuard } from '@core/guards/role.guard';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/job-list/job-list.component').then(m => m.JobListComponent),
  },
  {
    path: 'new',
    loadComponent: () =>
      import('./pages/job-create/job-create.component').then(m => m.JobCreateComponent),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./pages/job-detail/job-detail.component').then(m => m.JobDetailComponent),
  },
  {
    path: ':id/edit',
    canActivate: [roleGuard('OWNER', 'MANAGER')],
    loadComponent: () =>
      import('./pages/job-edit/job-edit.component').then(m => m.JobEditComponent),
  },
];

export default routes;
