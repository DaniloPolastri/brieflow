import { TestBed, ComponentFixture } from '@angular/core/testing';
import { JobListComponent } from './job-list.component';
import { JobApiService } from '@features/jobs/services/job-api.service';
import { StorageService } from '@core/services/storage.service';
import { MemberApiService } from '@features/members/services/member-api.service';
import { ConfirmationService } from 'primeng/api';
import { of } from 'rxjs';
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
    assignedCreativeName: 'João',
    status: 'NOVO',
    isOverdue: true,
  },
];

describe('JobListComponent', () => {
  let fixture: ComponentFixture<JobListComponent>;
  let component: JobListComponent;
  let jobApiSpy: { list: ReturnType<typeof vi.fn>; archive: ReturnType<typeof vi.fn> };
  let memberApiSpy: { list: ReturnType<typeof vi.fn> };
  let storageSpy: { getUser: ReturnType<typeof vi.fn> };

  function setup(role = 'OWNER') {
    jobApiSpy = {
      list: vi.fn().mockReturnValue(of(mockJobs)),
      archive: vi.fn().mockReturnValue(of(mockJobs[0])),
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
    };

    TestBed.configureTestingModule({
      imports: [JobListComponent, NoopAnimationsModule],
      providers: [
        { provide: JobApiService, useValue: jobApiSpy },
        { provide: MemberApiService, useValue: memberApiSpy },
        { provide: StorageService, useValue: storageSpy },
        ConfirmationService,
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
    TestBed.resetTestingModule();
  });

  it('should load jobs on init with archived=false and clientId from route', () => {
    setup();
    expect(jobApiSpy.list).toHaveBeenCalledWith({ archived: false, clientId: 1 });
    expect(component.jobs()).toEqual(mockJobs);
  });

  it('should render job codes and titles', () => {
    setup();
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
    jobApiSpy = {
      list: vi.fn().mockReturnValue(of([])),
      archive: vi.fn().mockReturnValue(of(mockJobs[0])),
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
        role: 'OWNER',
      }),
    };

    TestBed.configureTestingModule({
      imports: [JobListComponent, NoopAnimationsModule],
      providers: [
        { provide: JobApiService, useValue: jobApiSpy },
        { provide: MemberApiService, useValue: memberApiSpy },
        { provide: StorageService, useValue: storageSpy },
        ConfirmationService,
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
});
