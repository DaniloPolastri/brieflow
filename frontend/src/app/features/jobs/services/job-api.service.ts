import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpEvent, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import type {
  Job,
  JobFile,
  JobListFilters,
  JobListItem,
  JobRequest,
  JobStatus,
  JobStatusResponse,
  UpdateJobStatusRequest,
} from '../models/job.model';

@Injectable({ providedIn: 'root' })
export class JobApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/jobs`;

  list(filters?: JobListFilters): Observable<JobListItem[]> {
    let params = new HttpParams();
    if (filters?.search) params = params.set('search', filters.search);
    if (filters?.clientId !== undefined) params = params.set('clientId', String(filters.clientId));
    if (filters?.type) params = params.set('type', filters.type);
    if (filters?.priority) params = params.set('priority', filters.priority);
    if (filters?.assignedCreativeId !== undefined) {
      params = params.set('assignedCreativeId', String(filters.assignedCreativeId));
    }
    if (filters?.archived !== undefined) {
      params = params.set('archived', String(filters.archived));
    }
    return this.http.get<JobListItem[]>(this.baseUrl, { params });
  }

  getById(id: number): Observable<Job> {
    return this.http.get<Job>(`${this.baseUrl}/${id}`);
  }

  create(request: JobRequest): Observable<Job> {
    return this.http.post<Job>(this.baseUrl, request);
  }

  update(id: number, request: JobRequest): Observable<Job> {
    return this.http.put<Job>(`${this.baseUrl}/${id}`, request);
  }

  archive(id: number, archived: boolean): Observable<Job> {
    return this.http.patch<Job>(`${this.baseUrl}/${id}/archive`, { archived });
  }

  uploadFile(jobId: number, file: File): Observable<HttpEvent<JobFile>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<JobFile>(`${this.baseUrl}/${jobId}/files`, formData, {
      reportProgress: true,
      observe: 'events',
    });
  }

  deleteFile(jobId: number, fileId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${jobId}/files/${fileId}`);
  }

  downloadUrl(jobId: number, fileId: number): string {
    return `${this.baseUrl}/${jobId}/files/${fileId}/download`;
  }

  updateStatus(jobId: number, status: JobStatus, confirm = false): Observable<JobStatusResponse> {
    const body: UpdateJobStatusRequest = { status, confirm };
    return this.http.patch<JobStatusResponse>(`${this.baseUrl}/${jobId}/status`, body);
  }
}
