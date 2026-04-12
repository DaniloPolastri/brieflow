import { TestBed, ComponentFixture } from '@angular/core/testing';
import { JobListComponent } from './job-list.component';
import { JobApiService } from '@features/jobs/services/job-api.service';
import { JobSseService } from '@features/jobs/services/job-sse.service';
import { StorageService } from '@core/services/storage.service';
import { MemberApiService } from '@features/members/services/member-api.service';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of, EMPTY } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import type { JobListItem } from '@features/jobs/models/job.model';

const mockJobs: JobListItem[] = [
  {
    id: 1,
    code: 'JOB-001',
    title: 'Post lançamento',
    clientName: 'Acme',
    type: 'POST_FEED',
    deadline: '2026-05-01',
    priority: 'NORMAL',
    assignedCreativeId: null,
    assignedCreativeName: null,
    status: 'NOVO',
    isOverdue: false,
  },
  {
    id: 2,
    code: 'JOB-002',
    title: 'Carrossel Black Friday',
    clientName: 'Beta',
    type: 'CARROSSEL',
    deadline: '2026-04-01',
    priority: 'URGENTE',
    assignedCreativeId: 5,
    assignedCreativeName: 'João',
    status: 'NOVO',
    isOverdue: true,
  },
];

describe('JobListComponent', () => {
  let fixture: ComponentFixture<JobListComponent>;
  let component: JobListComponent;
  let jobApiSpy: { list: ReturnType<typeof vi.fn>; archive: ReturnType<typeof vi.fn>; updateStatus: ReturnType<typeof vi.fn> };
  let memberApiSpy: { list: ReturnType<typeof vi.fn> };
  let storageSpy: { getUser: ReturnType<typeof vi.fn>; getAccessToken: ReturnType<typeof vi.fn> };
  let sseSpy: { connect: ReturnType<typeof vi.fn>; disconnect: ReturnType<typeof vi.fn> };

  function setup(role = 'OWNER') {
    // Clear localStorage so viewMode defaults to 'kanban'
    localStorage.removeItem('jobViewMode');

    jobApiSpy = {
      list: vi.fn().mockReturnValue(of(mockJobs)),
      archive: vi.fn().mockReturnValue(of(mockJobs[0])),
      updateStatus: vi.fn().mockReturnValue(of({ applied: true, skippedSteps: false })),
    };
    memberApiSpy = {
      list: vi.fn().mockReturnValue(of({ members: [], pendingInvites: [] })),
    };
    storageSpy = {
      getUser: vi.fn().mockReturnValue({
        id: 10,
        name: 'Ana',
        email: 'ana@a.com',
        workspaceId: 1,
        workspaceName: 'A',
        role,
      }),
      getAccessToken: vi.fn().mockReturnValue('test-token'),
    };
    sseSpy = {
      connect: vi.fn().mockReturnValue(EMPTY),
      disconnect: vi.fn(),
    };

    TestBed.configureTestingModule({
      imports: [JobListComponent, NoopAnimationsModule],
      providers: [
        { provide: JobApiService, useValue: jobApiSpy },
        { provide: MemberApiService, useValue: memberApiSpy },
        { provide: StorageService, useValue: storageSpy },
        { provide: JobSseService, useValue: sseSpy },
        ConfirmationService,
        MessageService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: convertToParamMap({ clientId: '1' }) },
          },
        },
      ],
    });

    fixture = TestBed.createComponent(JobListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  afterEach(() => {
    vi.clearAllMocks();
    localStorage.removeItem('jobViewMode');
    TestBed.resetTestingModule();
  });

  it('should load jobs on init with archived=false and clientId from route', () => {
    setup();
    expect(jobApiSpy.list).toHaveBeenCalledWith({ archived: false, clientId: 1 });
    expect(component.jobs()).toEqual(mockJobs);
  });

  it('should render job codes and titles in list view', () => {
    setup();
    component.setViewMode('list');
    fixture.detectChanges();
    const text: string = fixture.nativeElement.textContent;
    expect(text).toContain('JOB-001');
    expect(text).toContain('Post lançamento');
    expect(text).toContain('JOB-002');
  });

  it('should show "Novo Job" button for MANAGER', () => {
    setup('MANAGER');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Novo Job');
  });

  it('should hide "Novo Job" button for CREATIVE', () => {
    setup('CREATIVE');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Novo Job');
  });

  it('should hide action menus for CREATIVE (only view)', () => {
    setup('CREATIVE');
    fixture.detectChanges();
    // CREATIVE still sees the menu button but with only "Ver" inside it
    const items = component.getMenuItems(mockJobs[0]);
    expect(items.length).toBe(1);
    expect(items[0].label).toBe('Ver');
  });

  it('should debounce search input', async () => {
    setup();
    component.onSearchInput({ target: { value: 'acme' } } as unknown as Event);
    component.onSearchInput({ target: { value: 'acme post' } } as unknown as Event);
    await new Promise((r) => setTimeout(r, 350));
    expect(jobApiSpy.list).toHaveBeenLastCalledWith(
      expect.objectContaining({ search: 'acme post' }),
    );
  });

  it('should toggle archived filter and reload', () => {
    setup();
    component.showArchived.set(true);
    component.loadJobs();
    expect(jobApiSpy.list).toHaveBeenLastCalledWith(
      expect.objectContaining({ archived: true }),
    );
  });

  it('should call archive with confirmation', () => {
    setup('OWNER');
    const confirm = fixture.debugElement.injector.get(ConfirmationService);
    vi.spyOn(confirm, 'confirm').mockImplementation((cfg: any) => {
      cfg.accept();
      return confirm;
    });
    component.archiveJob(mockJobs[0]);
    expect(jobApiSpy.archive).toHaveBeenCalledWith(1, true);
  });

  it('should show empty state when no jobs', () => {
    localStorage.removeItem('jobViewMode');
    const emptyJobApi = {
      list: vi.fn().mockReturnValue(of([])),
      archive: vi.fn().mockReturnValue(of(mockJobs[0])),
      updateStatus: vi.fn().mockReturnValue(of({ applied: true, skippedSteps: false })),
    };
    const emptyMemberApi = {
      list: vi.fn().mockReturnValue(of({ members: [], pendingInvites: [] })),
    };
    const emptyStorage = {
      getUser: vi.fn().mockReturnValue({
        id: 10,
        name: 'Ana',
        email: 'ana@a.com',
        workspaceId: 1,
        workspaceName: 'A',
        role: 'OWNER',
      }),
      getAccessToken: vi.fn().mockReturnValue('test-token'),
    };

    TestBed.configureTestingModule({
      imports: [JobListComponent, NoopAnimationsModule],
      providers: [
        { provide: JobApiService, useValue: emptyJobApi },
        { provide: MemberApiService, useValue: emptyMemberApi },
        { provide: StorageService, useValue: emptyStorage },
        { provide: JobSseService, useValue: { connect: vi.fn().mockReturnValue(EMPTY), disconnect: vi.fn() } },
        ConfirmationService,
        MessageService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: convertToParamMap({ clientId: '1' }) },
          },
        },
      ],
    });

    fixture = TestBed.createComponent(JobListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Nenhum job');
  });

  // --- RF05 Kanban tests ---

  it('should default to kanban view', () => {
    setup();
    expect(component.viewMode()).toBe('kanban');
  });

  it('should persist view mode in localStorage', () => {
    setup();
    component.setViewMode('list');
    expect(localStorage.getItem('jobViewMode')).toBe('list');
  });

  it('should show kanban board when viewMode is kanban', () => {
    setup();
    component.setViewMode('kanban');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-kanban-board')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('p-table')).toBeFalsy();
  });

  it('should show table when viewMode is list', () => {
    setup();
    component.setViewMode('list');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-kanban-board')).toBeFalsy();
    expect(fixture.nativeElement.querySelector('p-table')).toBeTruthy();
  });

  it('should default myJobsOnly to false for OWNER', () => {
    setup('OWNER');
    expect(component.myJobsOnly()).toBe(false);
  });

  it('should default myJobsOnly to true for CREATIVE', () => {
    setup('CREATIVE');
    expect(component.myJobsOnly()).toBe(true);
  });

  it('should filter jobs when myJobsOnly is true', () => {
    setup('OWNER');
    component.myJobsOnly.set(true);
    // Only jobs assigned to user id=10 should pass
    const filtered = component.filteredJobs();
    expect(filtered.length).toBe(0); // no jobs are assigned to id=10
  });
});
