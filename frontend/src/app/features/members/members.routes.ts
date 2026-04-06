import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/member-list/member-list.component').then(m => m.MemberListComponent),
  },
];

export default routes;
