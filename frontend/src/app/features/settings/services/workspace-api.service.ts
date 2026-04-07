import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Workspace, UpdateWorkspaceRequest } from '../models/workspace.model';

@Injectable({ providedIn: 'root' })
export class WorkspaceApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/v1/workspace`;

  get(): Observable<Workspace> {
    return this.http.get<Workspace>(this.apiUrl);
  }

  update(request: UpdateWorkspaceRequest): Observable<Workspace> {
    return this.http.put<Workspace>(this.apiUrl, request);
  }
}
