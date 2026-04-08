import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Client, ClientRequest } from '../models/client.model';

@Injectable({ providedIn: 'root' })
export class ClientApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/clients`;

  list(params?: { search?: string; active?: boolean }): Observable<Client[]> {
    let httpParams = new HttpParams();
    if (params?.search) httpParams = httpParams.set('search', params.search);
    if (params?.active !== undefined) httpParams = httpParams.set('active', String(params.active));
    return this.http.get<Client[]>(this.baseUrl, { params: httpParams });
  }

  getById(id: number): Observable<Client> {
    return this.http.get<Client>(`${this.baseUrl}/${id}`);
  }

  create(request: ClientRequest): Observable<Client> {
    return this.http.post<Client>(this.baseUrl, request);
  }

  update(id: number, request: ClientRequest): Observable<Client> {
    return this.http.put<Client>(`${this.baseUrl}/${id}`, request);
  }

  toggleActive(id: number): Observable<Client> {
    return this.http.patch<Client>(`${this.baseUrl}/${id}/toggle`, {});
  }

  uploadLogo(id: number, file: File): Observable<Client> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Client>(`${this.baseUrl}/${id}/logo`, formData);
  }

  removeLogo(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}/logo`);
  }

  assignMembers(clientId: number, memberIds: number[]): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${clientId}/members`, { memberIds });
  }

  unassignMember(clientId: number, memberId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${clientId}/members/${memberId}`);
  }

  getAssignedMembers(clientId: number): Observable<number[]> {
    return this.http.get<number[]>(`${this.baseUrl}/${clientId}/members`);
  }
}
