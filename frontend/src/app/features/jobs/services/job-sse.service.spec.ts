import { TestBed } from '@angular/core/testing';
import { JobSseService } from './job-sse.service';
import { StorageService } from '@core/services/storage.service';

class MockEventSource {
  onmessage: ((event: MessageEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;
  addEventListener = vi.fn();
  close = vi.fn();
  constructor(public url: string) {
    MockEventSource.lastInstance = this;
  }
  static lastInstance: MockEventSource;
}

describe('JobSseService', () => {
  let service: JobSseService;
  let originalEventSource: typeof EventSource;

  beforeEach(() => {
    originalEventSource = globalThis.EventSource;
    (globalThis as any).EventSource = MockEventSource;

    TestBed.configureTestingModule({
      providers: [
        JobSseService,
        {
          provide: StorageService,
          useValue: { getAccessToken: () => 'test-jwt-token' },
        },
      ],
    });
    service = TestBed.inject(JobSseService);
  });

  afterEach(() => {
    service.disconnect();
    globalThis.EventSource = originalEventSource;
  });

  it('should create EventSource with correct URL and token', () => {
    service.connect(42);
    expect(MockEventSource.lastInstance.url).toContain('/api/v1/clients/42/jobs/stream');
    expect(MockEventSource.lastInstance.url).toContain('token=test-jwt-token');
  });

  it('should emit parsed JobStatusEvent on named event', () => {
    const events: any[] = [];
    service.connect(42).subscribe(e => events.push(e));

    const handler = MockEventSource.lastInstance.addEventListener.mock.calls
      .find((c: any[]) => c[0] === 'job-status-changed');
    expect(handler).toBeTruthy();

    handler![1]({ data: JSON.stringify({ jobId: 1, previousStatus: 'NOVO', newStatus: 'EM_CRIACAO' }) });
    expect(events).toHaveLength(1);
    expect(events[0]).toEqual({ jobId: 1, previousStatus: 'NOVO', newStatus: 'EM_CRIACAO' });
  });

  it('should close EventSource on disconnect', () => {
    service.connect(42);
    const instance = MockEventSource.lastInstance;
    service.disconnect();
    expect(instance.close).toHaveBeenCalled();
  });

  it('should close previous connection when connecting again', () => {
    service.connect(1);
    const first = MockEventSource.lastInstance;
    service.connect(2);
    expect(first.close).toHaveBeenCalled();
  });
});
