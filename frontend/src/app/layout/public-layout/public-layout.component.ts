import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-public-layout',
  imports: [RouterOutlet],
  template: `
    <div class="flex min-h-screen items-center justify-center bg-gray-50">
      <div class="w-full max-w-md p-8">
        <div class="mb-8 text-center">
          <h1 class="text-3xl font-bold text-indigo-600">BriefFlow</h1>
          <p class="mt-2 text-sm text-gray-500">Gestao de producao criativa</p>
        </div>
        <router-outlet />
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicLayoutComponent {}
