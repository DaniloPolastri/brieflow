import { Injectable, inject } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { environment } from '@env/environment';
import { StorageService } from '@core/services/storage.service';
import type { JobStatusEvent } from '../models/job.model';

@Injectable({ providedIn: 'root' })
export class JobSseService {
  private readonly storage = inject(StorageService);
  private eventSource: EventSource | null = null;
  private readonly events$ = new Subject<JobStatusEvent>();

  connect(clientId: number): Observable<JobStatusEvent> {
    this.disconnect();

    const token = this.storage.getAccessToken();
    const url = `${environment.apiUrl}/api/v1/clients/${clientId}/jobs/stream?token=${token}`;

    this.eventSource = new EventSource(url);

    this.eventSource.addEventListener('job-status-changed', (event: MessageEvent) => {
      const data: JobStatusEvent = JSON.parse(event.data);
      this.events$.next(data);
    });

    this.eventSource.onerror = () => {
      // EventSource reconnects automatically; no manual handling needed
    };

    return this.events$.asObservable();
  }

  disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }
}
