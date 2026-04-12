import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { JobApiService } from './job-api.service';
import type { JobStatusResponse } from '../models/job.model';

describe('JobApiService', () => {
  let service: JobApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        JobApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(JobApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should call PATCH /jobs/{id}/status with body', () => {
    const mockResponse: JobStatusResponse = {
      id: 1,
      code: 'JOB-001',
      previousStatus: 'NOVO',
      newStatus: 'EM_CRIACAO',
      skippedSteps: false,
      applied: true,
    };

    service.updateStatus(1, 'EM_CRIACAO', false).subscribe(res => {
      expect(res).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(r =>
      r.method === 'PATCH' && r.url.endsWith('/jobs/1/status'),
    );
    expect(req.request.body).toEqual({ status: 'EM_CRIACAO', confirm: false });
    req.flush(mockResponse);
  });

  it('should send confirm=true when skipping steps', () => {
    service.updateStatus(1, 'APROVADO', true).subscribe();

    const req = httpMock.expectOne(r => r.method === 'PATCH');
    expect(req.request.body).toEqual({ status: 'APROVADO', confirm: true });
    req.flush({});
  });
});
