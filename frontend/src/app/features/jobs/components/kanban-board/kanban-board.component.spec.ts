import { ComponentFixture, TestBed } from '@angular/core/testing';
import { KanbanBoardComponent } from './kanban-board.component';
import { JobApiService } from '../../services/job-api.service';
import { JobSseService } from '../../services/job-sse.service';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of, Subject, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import type { JobListItem, JobStatusResponse } from '../../models/job.model';

describe('KanbanBoardComponent', () => {
  let fixture: ComponentFixture<KanbanBoardComponent>;
  let component: KanbanBoardComponent;
  let jobApi: { updateStatus: ReturnType<typeof vi.fn> };
  let sseEvents$: Subject<any>;

  const mockJobs: JobListItem[] = [
    {
      id: 1, status: 'NOVO', code: 'JOB-001', title: 'A', type: 'BANNER',
      priority: 'NORMAL', assignedCreativeId: null, assignedCreativeName: null,
      deadline: null, isOverdue: false, clientName: 'C',
    },
    {
      id: 2, status: 'EM_CRIACAO', code: 'JOB-002', title: 'B', type: 'LOGO',
      priority: 'ALTA', assignedCreativeId: 1, assignedCreativeName: 'Ana',
      deadline: null, isOverdue: false, clientName: 'D',
    },
  ];

  beforeEach(async () => {
    sseEvents$ = new Subject();
    jobApi = { updateStatus: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [KanbanBoardComponent, NoopAnimationsModule],
      providers: [
        { provide: JobApiService, useValue: jobApi },
        {
          provide: JobSseService,
          useValue: {
            connect: () => sseEvents$.asObservable(),
            disconnect: vi.fn(),
          },
        },
        ConfirmationService,
        MessageService,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(KanbanBoardComponent);
    fixture.componentRef.setInput('clientId', 1);
    fixture.componentRef.setInput('jobs', mockJobs);
    fixture.componentRef.setInput('currentUserId', 1);
    fixture.componentRef.setInput('canManage', true);
    fixture.detectChanges();
    component = fixture.componentInstance;
  });

  it('should render 6 kanban columns', () => {
    const columns = fixture.nativeElement.querySelectorAll('app-kanban-column');
    expect(columns.length).toBe(6);
  });

  it('should group jobs by status into correct columns', () => {
    const grouped = component.columnJobs();
    expect(grouped['NOVO'].length).toBe(1);
    expect(grouped['EM_CRIACAO'].length).toBe(1);
    expect(grouped['REVISAO_INTERNA'].length).toBe(0);
  });

  it('should call updateStatus on drop and apply optimistically', () => {
    const resp: JobStatusResponse = {
      id: 1, code: 'JOB-001', previousStatus: 'NOVO',
      newStatus: 'EM_CRIACAO', skippedSteps: false, applied: true,
    };
    jobApi.updateStatus.mockReturnValue(of(resp));
    component.onJobDrop(1, 'EM_CRIACAO', false);
    expect(jobApi.updateStatus).toHaveBeenCalledWith(1, 'EM_CRIACAO', false);
  });

  it('should move job when SSE event arrives', () => {
    sseEvents$.next({ jobId: 1, previousStatus: 'NOVO', newStatus: 'REVISAO_INTERNA' });
    fixture.detectChanges();
    const grouped = component.columnJobs();
    expect(grouped['NOVO'].length).toBe(0);
    expect(grouped['REVISAO_INTERNA'].length).toBe(1);
  });

  it('should revert optimistic update on API error', () => {
    jobApi.updateStatus.mockReturnValue(throwError(() => ({ status: 403 })));
    component.onJobDrop(1, 'EM_CRIACAO', false);
    fixture.detectChanges();
    // Job should still be in NOVO
    expect(component.columnJobs()['NOVO'].length).toBe(1);
  });

  it('should show confirm dialog for skip-step response', () => {
    const resp: JobStatusResponse = {
      id: 1, code: 'JOB-001', previousStatus: 'NOVO',
      newStatus: 'REVISAO_INTERNA', skippedSteps: true, applied: false,
    };
    jobApi.updateStatus.mockReturnValue(of(resp));
    const confirmSpy = vi.spyOn(
      fixture.debugElement.injector.get(ConfirmationService),
      'confirm',
    );
    component.onJobDrop(1, 'REVISAO_INTERNA', false);
    expect(confirmSpy).toHaveBeenCalled();
  });
});
