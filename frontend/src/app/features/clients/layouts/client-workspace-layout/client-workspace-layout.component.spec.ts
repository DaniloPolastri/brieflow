import { TestBed, ComponentFixture } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { ClientWorkspaceLayoutComponent } from './client-workspace-layout.component';
import { ClientApiService } from '@features/clients/services/client-api.service';
import type { Client } from '@features/clients/models/client.model';

const mockClient: Client = {
  id: 42,
  name: 'Acme Corp',
  company: 'Acme Inc.',
  email: 'contact@acme.com',
  phone: null,
  logoUrl: null,
  active: true,
  createdAt: '2026-04-11',
};

describe('ClientWorkspaceLayoutComponent', () => {
  let fixture: ComponentFixture<ClientWorkspaceLayoutComponent>;
  let component: ClientWorkspaceLayoutComponent;
  let clientApi: { getById: ReturnType<typeof vi.fn> };

  function setup(clientIdParam: string | null = '42', getByIdImpl = () => of(mockClient)) {
    clientApi = {
      getById: vi.fn().mockImplementation(getByIdImpl),
    };

    TestBed.configureTestingModule({
      imports: [ClientWorkspaceLayoutComponent, NoopAnimationsModule],
      providers: [
        { provide: ClientApiService, useValue: clientApi },
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap(
                clientIdParam !== null ? { clientId: clientIdParam } : {},
              ),
            },
          },
        },
      ],
    });

    fixture = TestBed.createComponent(ClientWorkspaceLayoutComponent);
    component = fixture.componentInstance;
  }

  afterEach(() => {
    vi.clearAllMocks();
    TestBed.resetTestingModule();
  });

  it('should load client by id from route paramMap', () => {
    setup();
    fixture.detectChanges();
    expect(clientApi.getById).toHaveBeenCalledWith(42);
    expect(component.client()).toEqual(mockClient);
  });

  it('should expose client name via computed signal', () => {
    setup();
    fixture.detectChanges();
    expect(component.clientName()).toBe('Acme Corp');
  });

  it('should render client name in the sidebar once loaded', () => {
    setup();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Acme Corp');
  });

  it('should navigate to /clients when clientId is missing', () => {
    setup(null);
    const router = TestBed.inject(Router);
    const navSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    expect(navSpy).toHaveBeenCalledWith(['/clients']);
  });

  it('should navigate to /clients when client lookup fails', () => {
    setup('42', () => throwError(() => ({ status: 404 })));
    const router = TestBed.inject(Router);
    const navSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    expect(navSpy).toHaveBeenCalledWith(['/clients']);
  });
});
