import { ChangeDetectionStrategy, Component, inject, computed } from '@angular/core';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  imports: [],
  templateUrl: './dashboard.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent {
  private readonly authService = inject(AuthService);

  readonly userName = computed(() => this.authService.currentUser()?.name ?? null);
}
