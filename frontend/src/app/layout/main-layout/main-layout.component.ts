import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from '../sidebar/sidebar.component';

@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, SidebarComponent],
  templateUrl: './main-layout.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MainLayoutComponent {}
