import { ComponentFixture, TestBed } from '@angular/core/testing';
import { KanbanColumnComponent } from './kanban-column.component';
import type { JobListItem } from '../../models/job.model';

describe('KanbanColumnComponent', () => {
  let fixture: ComponentFixture<KanbanColumnComponent>;

  const mockJobs: JobListItem[] = [
    {
      id: 1, code: 'JOB-001', title: 'Job 1', type: 'BANNER', priority: 'NORMAL',
      status: 'NOVO', assignedCreativeId: null, assignedCreativeName: null,
      deadline: null, isOverdue: false, clientName: 'C',
    },
    {
      id: 2, code: 'JOB-002', title: 'Job 2', type: 'LOGO', priority: 'ALTA',
      status: 'NOVO', assignedCreativeId: 1, assignedCreativeName: 'Ana',
      deadline: '2026-05-01', isOverdue: false, clientName: 'D',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [KanbanColumnComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(KanbanColumnComponent);
    fixture.componentRef.setInput('status', 'NOVO');
    fixture.componentRef.setInput('label', 'Novo');
    fixture.componentRef.setInput('bgColor', 'bg-blue-50');
    fixture.componentRef.setInput('textColor', 'text-blue-800');
    fixture.componentRef.setInput('dotColor', 'bg-blue-500');
    fixture.componentRef.setInput('jobs', mockJobs);
    fixture.componentRef.setInput('canDragFn', () => true);
    fixture.detectChanges();
  });

  it('should render column label', () => {
    const label = fixture.nativeElement.querySelector('[data-testid="column-label"]');
    expect(label.textContent.trim()).toBe('Novo');
  });

  it('should render job count badge', () => {
    const count = fixture.nativeElement.querySelector('[data-testid="column-count"]');
    expect(count.textContent.trim()).toBe('2');
  });

  it('should render a card for each job', () => {
    const cards = fixture.nativeElement.querySelectorAll('app-kanban-card');
    expect(cards.length).toBe(2);
  });

  it('should show empty state when no jobs', () => {
    fixture.componentRef.setInput('jobs', []);
    fixture.detectChanges();
    const empty = fixture.nativeElement.querySelector('[data-testid="empty-state"]');
    expect(empty).toBeTruthy();
    expect(empty.textContent).toContain('Arraste jobs aqui');
  });

  it('should emit jobClicked when card emits cardClicked', () => {
    const spy = vi.fn();
    fixture.componentInstance.jobClicked.subscribe(spy);
    // Click the first kanban card
    const card = fixture.nativeElement.querySelector('[data-testid="kanban-card"]');
    card.click();
    expect(spy).toHaveBeenCalledWith(mockJobs[0]);
  });
});
