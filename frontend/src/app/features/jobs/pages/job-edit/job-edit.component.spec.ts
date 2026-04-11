import { TestBed, ComponentFixture } from '@angular/core/testing';
import { JobEditComponent } from './job-edit.component';
import { JobApiService } from '@features/jobs/services/job-api.service';
import { ClientApiService } from '@features/clients/services/client-api.service';
import { MemberApiService } from '@features/members/services/member-api.service';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { MessageService } from 'primeng/api';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import type { Job } from '@features/jobs/models/job.model';

describe('JobEditComponent', () => {
  let fixture: ComponentFixture<JobEditComponent>;
  let component: JobEditComponent;
  let jobApi: any;
  let navigateSpy: ReturnType<typeof vi.fn>;

  const mockJob: Job = {
    id: 42,
    code: 'JOB-042',
    title: 'Existing',
    client: { id: 1, name: 'A' },
    type: 'POST_FEED',
    description: null,
    deadline: null,
    priority: 'NORMAL',
    assignedCreative: null,
    status: 'NOVO',
    briefingData: { captionText: 'abc', format: '1:1' },
    archived: false,
    files: [],
    createdAt: '',
    updatedAt: '',
    createdByName: '',
  };

  beforeEach(() => {
    jobApi = {
      getById: vi.fn().mockReturnValue(of(mockJob)),
      update: vi.fn().mockReturnValue(of(mockJob)),
    };
    TestBed.configureTestingModule({
      imports: [JobEditComponent, NoopAnimationsModule],
      providers: [
        { provide: JobApiService, useValue: jobApi },
        {
          provide: ClientApiService,
          useValue: {
            list: () => of([]),
            getAssignedMembers: () => of([]),
          },
        },
        {
          provide: MemberApiService,
          useValue: {
            list: () => of({ members: [], pendingInvites: [] }),
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
    fixture = TestBed.createComponent(JobEditComponent);
    component = fixture.componentInstance;
    const router = TestBed.inject(Router);
    navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true) as any;
    fixture.detectChanges();
  });

  it('should load job by id on init', () => {
    expect(jobApi.getById).toHaveBeenCalledWith(42);
    expect(component.job()).toEqual(mockJob);
  });

  it('should update and navigate to detail on success', () => {
    component.onFormSubmit({
      title: 'X',
      clientId: 1,
      type: 'POST_FEED',
      priority: 'NORMAL',
      briefingData: {},
    });
    expect(jobApi.update).toHaveBeenCalledWith(42, expect.any(Object));
    expect(navigateSpy).toHaveBeenCalledWith(['/jobs', 42]);
  });

  it('should show error toast when update fails', () => {
    jobApi.update = vi
      .fn()
      .mockReturnValue(throwError(() => ({ error: { message: 'fail' } })));
    const msg = fixture.debugElement.injector.get(MessageService);
    const spy = vi.spyOn(msg, 'add');
    component.onFormSubmit({
      title: 'X',
      clientId: 1,
      type: 'POST_FEED',
      priority: 'NORMAL',
      briefingData: {},
    });
    expect(spy).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'error' }),
    );
  });
});
