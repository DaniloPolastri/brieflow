import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { WorkspaceApiService } from './workspace-api.service';
import { environment } from '../../../../environments/environment';
import { Workspace, UpdateWorkspaceRequest } from '../models/workspace.model';

describe('WorkspaceApiService', () => {
  let service: WorkspaceApiService;
  let httpMock: HttpTestingController;

  const baseUrl = `${environment.apiUrl}/api/v1/workspace`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(WorkspaceApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should get workspace', () => {
    const mockWorkspace: Workspace = { id: 1, name: 'Agencia XYZ', slug: 'agencia-xyz' };

    service.get().subscribe(res => expect(res).toEqual(mockWorkspace));

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');
    req.flush(mockWorkspace);
  });

  it('should update workspace', () => {
    const request: UpdateWorkspaceRequest = { name: 'Agencia Nova' };
    const mockWorkspace: Workspace = { id: 1, name: 'Agencia Nova', slug: 'agencia-xyz' };

    service.update(request).subscribe(res => expect(res).toEqual(mockWorkspace));

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(request);
    req.flush(mockWorkspace);
  });
});
