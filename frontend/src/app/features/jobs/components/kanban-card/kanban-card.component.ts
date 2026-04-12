import { Component, ChangeDetectionStrategy, input, output, computed } from '@angular/core';
import { KANBAN_COLUMNS, KanbanColumn, JobListItem } from '../../models/job.model';

@Component({
  selector: 'app-kanban-card',
  standalone: true,
  imports: [],
  templateUrl: './kanban-card.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KanbanCardComponent {
  readonly job = input.required<JobListItem>();
  readonly disabled = input<boolean>(false);
  readonly cardClicked = output<JobListItem>();

  readonly statusColumn = computed<KanbanColumn | undefined>(() =>
    KANBAN_COLUMNS.find(c => c.status === this.job().status),
  );

  readonly typeLabel = computed(() => {
    const map: Record<string, string> = {
      POST_FEED: 'Post Feed',
      STORIES: 'Stories',
      CARROSSEL: 'Carrossel',
      REELS_VIDEO: 'Reels',
      BANNER: 'Banner',
      LOGO: 'Logo',
      OUTROS: 'Outros',
    };
    return map[this.job().type] ?? this.job().type;
  });

  readonly priorityConfig = computed(() => {
    const map: Record<string, { label: string; class: string }> = {
      BAIXA: { label: 'Baixa', class: 'bg-gray-100 text-gray-600' },
      NORMAL: { label: 'Normal', class: 'bg-blue-50 text-blue-700' },
      ALTA: { label: 'Alta', class: 'bg-amber-50 text-amber-700' },
      URGENTE: { label: 'Urgente', class: 'bg-red-50 text-red-700' },
    };
    return map[this.job().priority] ?? { label: this.job().priority, class: 'bg-gray-100 text-gray-600' };
  });

  readonly initials = computed(() => {
    const name = this.job().assignedCreativeName;
    if (!name) return '';
    return name
      .split(' ')
      .map(n => n[0])
      .join('')
      .substring(0, 2)
      .toUpperCase();
  });

  readonly formattedDeadline = computed(() => {
    const d = this.job().deadline;
    if (!d) return null;
    return new Date(d).toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
    });
  });

  onClick(): void {
    this.cardClicked.emit(this.job());
  }
}
