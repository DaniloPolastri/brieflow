import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { InviteInfo, AcceptInviteRequest } from '../models/invite.model';
import { TokenResponse } from '../../../core/models/user.model';

@Injectable({ providedIn: 'root' })
export class InviteApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/v1/invite`;

  getInfo(token: string): Observable<InviteInfo> {
    return this.http.get<InviteInfo>(`${this.apiUrl}/${token}`);
  }

  accept(token: string, request: AcceptInviteRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${this.apiUrl}/${token}/accept`, request);
  }
}
