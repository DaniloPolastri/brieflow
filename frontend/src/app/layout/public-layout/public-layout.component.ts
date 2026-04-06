import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-public-layout',
  imports: [RouterOutlet],
  template: `
    <div class="flex min-h-screen font-sans">
      <!-- Brand Panel -->
      <div class="hidden lg:flex lg:w-[480px] flex-col justify-between bg-indigo-900 p-10 text-white relative overflow-hidden">
        <!-- Dot grid pattern -->
        <div class="absolute inset-0 opacity-[0.07]"
             style="background-image: radial-gradient(circle, rgba(255,255,255,0.8) 1px, transparent 1px); background-size: 24px 24px;"></div>

        <!-- Gradient glow -->
        <div class="absolute -bottom-32 -left-32 h-80 w-80 rounded-full bg-indigo-500/20 blur-[100px]"></div>
        <div class="absolute -top-20 -right-20 h-60 w-60 rounded-full bg-violet-500/15 blur-[80px]"></div>

        <div class="relative z-10">
          <div class="flex items-center gap-3">
            <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-white/10 backdrop-blur-sm">
              <svg class="h-5 w-5 text-indigo-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <span class="text-xl font-semibold tracking-tight">BriefFlow</span>
          </div>
        </div>

        <div class="relative z-10 space-y-6">
          <h2 class="text-[28px] font-semibold leading-[1.2] tracking-tight">
            Producao criativa<br>sem caos.
          </h2>
          <p class="text-[15px] leading-relaxed text-indigo-200/80 max-w-[340px]">
            Centralize briefings, gerencie entregas no kanban e
            receba aprovacoes dos clientes — tudo em um so lugar.
          </p>

          <div class="flex items-center gap-3 pt-2">
            <div class="flex -space-x-2">
              <div class="h-8 w-8 rounded-full bg-indigo-400/30 ring-2 ring-indigo-900 flex items-center justify-center text-xs font-medium">M</div>
              <div class="h-8 w-8 rounded-full bg-violet-400/30 ring-2 ring-indigo-900 flex items-center justify-center text-xs font-medium">C</div>
              <div class="h-8 w-8 rounded-full bg-pink-400/30 ring-2 ring-indigo-900 flex items-center justify-center text-xs font-medium">D</div>
            </div>
            <span class="text-[13px] text-indigo-300/70">Para agencias de 2 a 15 pessoas</span>
          </div>
        </div>

        <div class="relative z-10">
          <p class="text-[13px] text-indigo-400/50">&copy; 2026 BriefFlow</p>
        </div>
      </div>

      <!-- Form Panel -->
      <div class="flex flex-1 flex-col items-center justify-center bg-gray-50/50 px-6 py-12">
        <!-- Mobile logo -->
        <div class="mb-10 flex items-center gap-2.5 lg:hidden">
          <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-500">
            <svg class="h-4.5 w-4.5 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <span class="text-lg font-semibold tracking-tight text-gray-900">BriefFlow</span>
        </div>

        <div class="w-full max-w-[400px]">
          <router-outlet />
        </div>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicLayoutComponent {}
