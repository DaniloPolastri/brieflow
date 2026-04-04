import { Component } from '@angular/core';
import { ButtonModule } from 'primeng/button';

@Component({
  selector: 'app-root',
  imports: [ButtonModule],
  template: `
    <div class="flex items-center justify-center min-h-screen bg-gray-100 gap-4">
      <h1 class="text-3xl font-bold text-indigo-500">BriefFlow</h1>
      <p-button label="Test PrimeNG" />
    </div>
  `
})
export class App {}
