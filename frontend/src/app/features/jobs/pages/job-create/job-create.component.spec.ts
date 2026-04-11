import { TestBed, ComponentFixture } from '@angular/core/testing';
import { JobCreateComponent } from './job-create.component';
import { JobApiService } from '@features/jobs/services/job-api.service';
import { ClientApiService } from '@features/clients/services/client-api.service';
import { MemberApiService } from '@features/members/services/member-api.service';
import { Router, provideRouter } from '@angular/router';
import { MessageService } from 'primeng/api';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import type { Job, JobRequest } from '@features/jobs/models/job.model';

describe('JobCreateComponent', () => {
  let fixture: ComponentFixture<JobCreateComponent>;
  let component: JobCreateComponent;
  let jobApi: any;
  let navigateSpy: ReturnType<typeof vi.fn>;

  const mockJob: Job = {
    id: 42,
    code: 'JOB-042',
    title: 'X',
    client: { id: 1, name: 'A' },
    type: 'POST_FEED',
    description: null,
    deadline: null,
    priority: 'NORMAL',
    assignedCreative: null,
    status: 'NOVO',
    briefingData: {},
    archived: false,
    files: [],
    createdAt: '',
    updatedAt: '',
    createdByName: 'M',
  };

  beforeEach(() => {
    jobApi = {
      create: vi.fn().mockReturnValue(of(mockJob)),
      uploadFile: vi.fn().mockReturnValue(of({})),
    };

    TestBed.configureTestingModule({
      imports: [JobCreateComponent, NoopAnimationsModule],
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
      ],
    });
    fixture = TestBed.createComponent(JobCreateComponent);
    component = fixture.componentInstance;
    const router = TestBed.inject(Router);
    navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true) as any;
    fixture.detectChanges();
  });

  it('should navigate to detail after successful create', () => {
    const req: JobRequest = {
      title: 'X',
      clientId: 1,
      type: 'POST_FEED',
      priority: 'NORMAL',
      briefingData: { captionText: 'x', format: '1:1' },
    };
    component.onFormSubmit(req);
    expect(jobApi.create).toHaveBeenCalledWith(req);
    expect(navigateSpy).toHaveBeenCalledWith(['/jobs', 42]);
  });

  it('should show error toast on failed create', () => {
    jobApi.create = vi
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
