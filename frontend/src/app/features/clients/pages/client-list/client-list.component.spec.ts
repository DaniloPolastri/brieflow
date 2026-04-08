import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ClientListComponent } from './client-list.component';
import { ClientApiService } from '@features/clients/services/client-api.service';
import { StorageService } from '@core/services/storage.service';
import { ConfirmationService } from 'primeng/api';
import { of } from 'rxjs';
import { Client } from '@features/clients/models/client.model';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

const mockClients: Client[] = [
  {
    id: 1,
    name: 'Maria Silva',
    company: 'Empresa Alfa',
    email: 'maria@alfa.com',
    phone: '(11) 99999-0001',
    logoUrl: null,
    active: true,
    createdAt: '2024-01-01T00:00:00Z',
  },
  {
    id: 2,
    name: 'João Souza',
    company: 'Beta Ltda',
    email: 'joao@beta.com',
    phone: '(11) 99999-0002',
    logoUrl: null,
    active: true,
    createdAt: '2024-01-02T00:00:00Z',
  },
];

describe('ClientListComponent', () => {
  let component: ClientListComponent;
  let fixture: ComponentFixture<ClientListComponent>;
  let clientApiSpy: {
    list: ReturnType<typeof vi.fn>;
    toggleActive: ReturnType<typeof vi.fn>;
  };
  let storageSpy: { getUser: ReturnType<typeof vi.fn> };

  function setup(role: string = 'OWNER') {
    clientApiSpy = {
      list: vi.fn().mockReturnValue(of(mockClients)),
      toggleActive: vi.fn().mockReturnValue(of({ ...mockClients[0], active: false })),
    };
    storageSpy = {
      getUser: vi.fn().mockReturnValue({
        id: 10,
        name: 'Ana Lima',
        email: 'ana@agency.com',
        workspaceId: 1,
        workspaceName: 'Agency',
        role,
        position: 'DESIGNER_GRAFICO',
      }),
    };

    TestBed.configureTestingModule({
      imports: [ClientListComponent, NoopAnimationsModule],
      providers: [
        { provide: ClientApiService, useValue: clientApiSpy },
        { provide: StorageService, useValue: storageSpy },
        ConfirmationService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    fixture = TestBed.createComponent(ClientListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  afterEach(() => {
    vi.clearAllMocks();
    TestBed.resetTestingModule();
  });

  it('should create the component', () => {
    setup();
    expect(component).toBeTruthy();
  });

  it('should load clients on init', () => {
    setup();
    expect(clientApiSpy.list).toHaveBeenCalledOnce();
    expect(component.clients()).toEqual(mockClients);
    expect(component.loading()).toBe(false);
  });

  it('should call list with active=true by default', () => {
    setup();
    expect(clientApiSpy.list).toHaveBeenCalledWith({ active: true });
  });

  it('should display client names in cards', () => {
    setup();
    fixture.detectChanges();
    const compiled: HTMLElement = fixture.nativeElement;
    expect(compiled.textContent).toContain('Maria Silva');
    expect(compiled.textContent).toContain('João Souza');
    expect(compiled.textContent).toContain('maria@alfa.com');
    expect(compiled.textContent).toContain('joao@beta.com');
  });

  it('should show "Novo cliente" button for OWNER', () => {
    setup('OWNER');
    fixture.detectChanges();
    const compiled: HTMLElement = fixture.nativeElement;
    expect(compiled.textContent).toContain('Novo cliente');
  });

  it('should show "Novo cliente" button for MANAGER', () => {
    setup('MANAGER');
    fixture.detectChanges();
    const compiled: HTMLElement = fixture.nativeElement;
    expect(compiled.textContent).toContain('Novo cliente');
  });

  it('should hide "Novo cliente" button for CREATIVE', () => {
    setup('CREATIVE');
    fixture.detectChanges();
    const compiled: HTMLElement = fixture.nativeElement;
    expect(compiled.textContent).not.toContain('Novo cliente');
  });

  it('should hide action menus for CREATIVE', () => {
    setup('CREATIVE');
    fixture.detectChanges();
    const compiled: HTMLElement = fixture.nativeElement;
    const menuButtons = compiled.querySelectorAll('[data-testid="client-menu-button"]');
    expect(menuButtons.length).toBe(0);
  });

  it('should show empty state when no clients', () => {
    clientApiSpy = {
      list: vi.fn().mockReturnValue(of([])),
      toggleActive: vi.fn(),
    };
    storageSpy = {
      getUser: vi.fn().mockReturnValue({
        id: 10,
        name: 'Ana Lima',
        email: 'ana@agency.com',
        workspaceId: 1,
        workspaceName: 'Agency',
        role: 'OWNER',
        position: 'DESIGNER_GRAFICO',
      }),
    };

    TestBed.configureTestingModule({
      imports: [ClientListComponent, NoopAnimationsModule],
      providers: [
        { provide: ClientApiService, useValue: clientApiSpy },
        { provide: StorageService, useValue: storageSpy },
        ConfirmationService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    fixture = TestBed.createComponent(ClientListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const compiled: HTMLElement = fixture.nativeElement;
    expect(compiled.textContent).toContain('Nenhum cliente encontrado');
  });

  it('should generate correct initials', () => {
    setup();
    expect(component.getInitials('Maria Silva')).toBe('MS');
    expect(component.getInitials('João')).toBe('J');
    expect(component.getInitials('Ana Beatriz Costa')).toBe('AB');
  });

  it('should return a valid avatar color class', () => {
    setup();
    const color = component.getAvatarColor('Maria Silva');
    expect(color).toBeTruthy();
    expect(color).toContain('bg-');
  });

  it('should toggle client active status with confirmation', () => {
    setup('OWNER');
    const confirmService = fixture.debugElement.injector.get(ConfirmationService);
    const confirmSpy = vi.spyOn(confirmService, 'confirm').mockImplementation((cfg: any) => {
      cfg.accept();
      return confirmService;
    });

    component.toggleActive(mockClients[0]);

    expect(confirmSpy).toHaveBeenCalled();
    expect(clientApiSpy.toggleActive).toHaveBeenCalledWith(1);
  });

  it('should open create dialog', () => {
    setup();
    component.openCreateDialog();
    expect(component.showFormDialog()).toBe(true);
    expect(component.editingClient()).toBeNull();
  });

  it('should open edit dialog with client', () => {
    setup();
    component.openEditDialog(mockClients[0]);
    expect(component.showFormDialog()).toBe(true);
    expect(component.editingClient()).toEqual(mockClients[0]);
  });
});
