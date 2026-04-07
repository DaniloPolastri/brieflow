import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import {
  MembersListResponse,
  InviteMemberRequest,
  InviteResponse,
  UpdateMemberRoleRequest,
} from '../models/member.model';

@Injectable({ providedIn: 'root' })
export class MemberApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/v1/members`;

  list(): Observable<MembersListResponse> {
    return this.http.get<MembersListResponse>(this.apiUrl);
  }

  invite(request: InviteMemberRequest): Observable<InviteResponse> {
    return this.http.post<InviteResponse>(`${this.apiUrl}/invite`, request);
  }

  cancelInvite(inviteId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/invite/${inviteId}`);
  }

  remove(memberId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${memberId}`);
  }

  updateRole(memberId: number, request: UpdateMemberRoleRequest): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${memberId}/role`, request);
  }
}
