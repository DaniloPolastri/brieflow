import { TestBed, ComponentFixture } from '@angular/core/testing';
import { JobFormComponent } from './job-form.component';
import { JobApiService } from '@features/jobs/services/job-api.service';
import { ClientApiService } from '@features/clients/services/client-api.service';
import { MemberApiService } from '@features/members/services/member-api.service';
import { of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import type { Job } from '@features/jobs/models/job.model';

describe('JobFormComponent', () => {
  let fixture: ComponentFixture<JobFormComponent>;
  let component: JobFormComponent;
  let jobApiSpy: any;
  let clientApiSpy: any;
  let memberApiSpy: any;

  beforeEach(() => {
    jobApiSpy = { create: vi.fn(), update: vi.fn() };
    clientApiSpy = {
      list: vi.fn().mockReturnValue(of([{ id: 1, name: 'Acme', active: true }])),
      getAssignedMembers: vi.fn().mockReturnValue(of([10])),
    };
    memberApiSpy = {
      list: vi.fn().mockReturnValue(
        of({
          members: [
            {
              id: 10,
              userId: 100,
              userName: 'João',
              userEmail: 'j@j.com',
              role: 'CREATIVE',
              position: 'DESIGNER_GRAFICO',
              createdAt: '2026-04-11',
            },
            {
              id: 11,
              userId: 101,
              userName: 'Ana',
              userEmail: 'a@a.com',
              role: 'MANAGER',
              position: 'ATENDIMENTO',
              createdAt: '2026-04-11',
            },
          ],
          pendingInvites: [],
        }),
      ),
    };
    TestBed.configureTestingModule({
      imports: [JobFormComponent, NoopAnimationsModule],
      providers: [
        { provide: JobApiService, useValue: jobApiSpy },
        { provide: ClientApiService, useValue: clientApiSpy },
        { provide: MemberApiService, useValue: memberApiSpy },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    fixture = TestBed.createComponent(JobFormComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('mode', 'create');
    fixture.componentRef.setInput('initialJob', null);
    fixture.detectChanges();
  });

  it('should create with default values', () => {
    expect(component.form.get('priority')!.value).toBe('NORMAL');
    expect(component.form.get('type')!.value).toBe('POST_FEED');
  });

  it('should be invalid when required fields are empty', () => {
    expect(component.form.valid).toBe(false);
  });

  it('should be valid with required fields filled', () => {
    component.form.patchValue({ title: 'Post', clientId: 1 });
    (component.form.get('briefingData') as any).patchValue({
      captionText: 'abc',
      format: '1:1',
    });
    expect(component.form.valid).toBe(true);
  });

  it('should reload creatives when client changes', () => {
    component.form.get('clientId')!.setValue(1);
    expect(clientApiSpy.getAssignedMembers).toHaveBeenCalledWith(1);
  });

  it('should filter creative options to CREATIVE role only', () => {
    component.form.get('clientId')!.setValue(1);
    fixture.detectChanges();
    expect(component.creativeOptions().map((o) => o.value)).toEqual([10]);
  });

  it('should emit submit when onSubmit called with valid form', () => {
    const spy = vi.fn();
    component.submitted.subscribe(spy);
    component.form.patchValue({ title: 'Post', clientId: 1 });
    (component.form.get('briefingData') as any).patchValue({
      captionText: 'abc',
      format: '1:1',
    });
    component.onSubmit();
    expect(spy).toHaveBeenCalled();
  });

  it('should populate form from initialJob in edit mode', async () => {
    const job: Job = {
      id: 1,
      code: 'JOB-001',
      title: 'Existing',
      client: { id: 1, name: 'Acme' },
      type: 'STORIES',
      description: 'desc',
      deadline: '2026-06-01',
      priority: 'ALTA',
      assignedCreative: { id: 10, name: 'João' },
      status: 'NOVO',
      briefingData: { text: 'hi', format: '9:16' },
      archived: false,
      files: [],
      createdAt: '2026-04-11',
      updatedAt: '2026-04-11',
      createdByName: 'Maria',
    };
    fixture.componentRef.setInput('mode', 'edit');
    fixture.componentRef.setInput('initialJob', job);
    fixture.detectChanges();
    await Promise.resolve();
    expect(component.form.get('title')!.value).toBe('Existing');
    expect(component.form.get('type')!.value).toBe('STORIES');
  });
});
