import { ComponentFixture, TestBed } from '@angular/core/testing';
import { KanbanCardComponent } from './kanban-card.component';
import type { JobListItem } from '../../models/job.model';

describe('KanbanCardComponent', () => {
  let fixture: ComponentFixture<KanbanCardComponent>;

  const mockJob: JobListItem = {
    id: 1,
    code: 'JOB-001',
    title: 'Banner para campanha de verão',
    type: 'BANNER',
    priority: 'ALTA',
    status: 'NOVO',
    clientName: 'Acme Corp',
    assignedCreativeName: 'João Silva',
    assignedCreativeId: 5,
    deadline: '2026-04-15',
    isOverdue: false,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [KanbanCardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(KanbanCardComponent);
    fixture.componentRef.setInput('job', mockJob);
    fixture.componentRef.setInput('disabled', false);
    fixture.detectChanges();
  });

  it('should render job code in monospace', () => {
    const code = fixture.nativeElement.querySelector('[data-testid="job-code"]');
    expect(code.textContent.trim()).toBe('JOB-001');
  });

  it('should render job title', () => {
    const title = fixture.nativeElement.querySelector('[data-testid="job-title"]');
    expect(title.textContent.trim()).toBe('Banner para campanha de verão');
  });

  it('should render type badge', () => {
    const badge = fixture.nativeElement.querySelector('[data-testid="type-badge"]');
    expect(badge.textContent.trim()).toContain('Banner');
  });

  it('should render priority badge', () => {
    const badge = fixture.nativeElement.querySelector('[data-testid="priority-badge"]');
    expect(badge).toBeTruthy();
  });

  it('should show deadline text', () => {
    const deadline = fixture.nativeElement.querySelector('[data-testid="deadline"]');
    expect(deadline).toBeTruthy();
  });

  it('should apply overdue styling when job is overdue', () => {
    fixture.componentRef.setInput('job', { ...mockJob, isOverdue: true });
    fixture.detectChanges();
    const deadline = fixture.nativeElement.querySelector('[data-testid="deadline"]');
    expect(deadline.classList.contains('text-red-500')).toBe(true);
  });

  it('should show creative avatar when assigned', () => {
    const avatar = fixture.nativeElement.querySelector('[data-testid="creative-avatar"]');
    expect(avatar).toBeTruthy();
  });

  it('should apply opacity-50 when disabled', () => {
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();
    const card = fixture.nativeElement.querySelector('[data-testid="kanban-card"]');
    expect(card.classList.contains('opacity-50')).toBe(true);
  });

  it('should emit cardClicked on click', () => {
    const spy = vi.fn();
    fixture.componentInstance.cardClicked.subscribe(spy);
    const card = fixture.nativeElement.querySelector('[data-testid="kanban-card"]');
    card.click();
    expect(spy).toHaveBeenCalledWith(mockJob);
  });
});
