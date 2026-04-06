import { Routes } from '@angular/router';
import { PublicLayoutComponent } from './layout/public-layout/public-layout.component';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: 'auth',
    component: PublicLayoutComponent,
    children: [
      {
        path: '',
        loadChildren: () => import('./features/auth/auth.routes'),
      },
    ],
  },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/pages/dashboard/dashboard.component').then(
            m => m.DashboardComponent
          ),
      },
      {
        path: 'members',
        loadChildren: () => import('./features/members/members.routes'),
      },
      {
        path: 'settings',
        canActivate: [roleGuard('OWNER', 'MANAGER')],
        loadChildren: () => import('./features/settings/settings.routes'),
      },
    ],
  },
  { path: '**', redirectTo: 'auth/login' },
];
