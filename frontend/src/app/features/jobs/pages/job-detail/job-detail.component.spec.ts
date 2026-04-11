import { TestBed, ComponentFixture } from '@angular/core/testing';
import { JobDetailComponent } from './job-detail.component';
import { JobApiService } from '@features/jobs/services/job-api.service';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { StorageService } from '@core/services/storage.service';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import type { Job } from '@features/jobs/models/job.model';

const mockJob: Job = {
  id: 42,
  code: 'JOB-042',
  title: 'Post Black Friday',
  client: { id: 1, name: 'Acme' },
  type: 'POST_FEED',
  description: 'Lorem',
  deadline: '2026-05-01',
  priority: 'ALTA',
  assignedCreative: { id: 10, name: 'João' },
  status: 'NOVO',
  briefingData: { captionText: 'texto', format: '1:1' },
  archived: false,
  files: [
    {
      id: 1,
      originalFilename: 'ref.jpg',
      mimeType: 'image/jpeg',
      sizeBytes: 123,
      uploadedAt: '2026-04-11',
      downloadUrl: '/x',
    },
  ],
  createdAt: '2026-04-11',
  updatedAt: '2026-04-11',
  createdByName: 'Maria',
};

describe('JobDetailComponent', () => {
  let fixture: ComponentFixture<JobDetailComponent>;
  let component: JobDetailComponent;
  let api: any;
  let navigateSpy: ReturnType<typeof vi.fn>;

  function setup(role = 'OWNER') {
    api = {
      getById: vi.fn().mockReturnValue(of(mockJob)),
      archive: vi.fn().mockReturnValue(of({ ...mockJob, archived: true })),
      downloadUrl: vi.fn().mockReturnValue('/download/42/1'),
      deleteFile: vi.fn().mockReturnValue(of(void 0)),
      uploadFile: vi.fn().mockReturnValue(of({})),
    };

    TestBed.configureTestingModule({
      imports: [JobDetailComponent, NoopAnimationsModule],
      providers: [
        { provide: JobApiService, useValue: api },
        {
          provide: StorageService,
          useValue: {
            getUser: () => ({
              id: 1,
              name: 'U',
              email: 'u@u.com',
              workspaceId: 1,
              workspaceName: 'W',
              role,
            }),
          },
        },
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { params: { id: '42' } } },
        },
      ],
    });
    fixture = TestBed.createComponent(JobDetailComponent);
    component = fixture.componentInstance;
    const router = TestBed.inject(Router);
    navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true) as any;
    fixture.detectChanges();
  }

  afterEach(() => {
    vi.clearAllMocks();
    TestBed.resetTestingModule();
  });

  it('should load job by id', () => {
    setup();
    expect(api.getById).toHaveBeenCalledWith(42);
    expect(component.job()).toEqual(mockJob);
  });

  it('should render job code, title, briefing fields', () => {
    setup();
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('JOB-042');
    expect(text).toContain('Post Black Friday');
    expect(text).toContain('Texto da legenda');
  });

  it('should show Edit/Archive buttons for OWNER', () => {
    setup('OWNER');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Editar');
    expect(fixture.nativeElement.textContent).toContain('Arquivar');
  });

  it('should hide Edit/Archive for CREATIVE', () => {
    setup('CREATIVE');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Editar');
    expect(fixture.nativeElement.textContent).not.toContain('Arquivar');
  });

  it('should archive with confirmation', () => {
    setup('OWNER');
    const confirm = fixture.debugElement.injector.get(ConfirmationService);
    vi.spyOn(confirm, 'confirm').mockImplementation((cfg: any) => {
      cfg.accept();
      return confirm;
    });
    component.archive();
    expect(api.archive).toHaveBeenCalledWith(42, true);
  });

  it('should navigate to list when job not found', () => {
    api = {
      getById: vi.fn().mockReturnValue(throwError(() => ({ status: 404 }))),
      archive: vi.fn(),
      downloadUrl: vi.fn().mockReturnValue('/download'),
      deleteFile: vi.fn(),
      uploadFile: vi.fn(),
    };
    TestBed.configureTestingModule({
      imports: [JobDetailComponent, NoopAnimationsModule],
      providers: [
        { provide: JobApiService, useValue: api },
        {
          provide: StorageService,
          useValue: {
            getUser: () => ({
              id: 1,
              name: 'U',
              email: 'u@u.com',
              workspaceId: 1,
              workspaceName: 'W',
              role: 'OWNER',
            }),
          },
        },
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { params: { id: '42' } } },
        },
      ],
    });
    fixture = TestBed.createComponent(JobDetailComponent);
    component = fixture.componentInstance;
    const router = TestBed.inject(Router);
    const spy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    expect(spy).toHaveBeenCalledWith(['/jobs']);
  });
});
