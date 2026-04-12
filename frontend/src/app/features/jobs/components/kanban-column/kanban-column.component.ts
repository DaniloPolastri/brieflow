import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { CdkDropList, CdkDrag, CdkDragDrop } from '@angular/cdk/drag-drop';
import { KanbanCardComponent } from '../kanban-card/kanban-card.component';
import { JobStatus, JobListItem } from '../../models/job.model';

@Component({
  selector: 'app-kanban-column',
  standalone: true,
  imports: [CdkDropList, CdkDrag, KanbanCardComponent],
  templateUrl: './kanban-column.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KanbanColumnComponent {
  readonly status = input.required<JobStatus>();
  readonly label = input.required<string>();
  readonly bgColor = input.required<string>();
  readonly textColor = input.required<string>();
  readonly dotColor = input.required<string>();
  readonly jobs = input.required<JobListItem[]>();
  readonly canDragFn = input.required<(job: JobListItem) => boolean>();

  readonly jobDropped = output<CdkDragDrop<JobListItem[]>>();
  readonly jobClicked = output<JobListItem>();

  onDrop(event: CdkDragDrop<JobListItem[]>): void {
    this.jobDropped.emit(event);
  }

  onCardClick(job: JobListItem): void {
    this.jobClicked.emit(job);
  }
}
