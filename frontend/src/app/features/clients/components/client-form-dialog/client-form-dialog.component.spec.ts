import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ClientFormDialogComponent } from './client-form-dialog.component';
import { ClientApiService } from '@features/clients/services/client-api.service';
import { MemberApiService } from '@features/members/services/member-api.service';

describe('ClientFormDialogComponent', () => {
  let clientApiMock: {
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    uploadLogo: ReturnType<typeof vi.fn>;
    removeLogo: ReturnType<typeof vi.fn>;
    assignMembers: ReturnType<typeof vi.fn>;
    getAssignedMembers: ReturnType<typeof vi.fn>;
  };
  let memberApiMock: {
    list: ReturnType<typeof vi.fn>;
  };

  const mockClient = {
    id: 1,
    name: 'Acme Corp',
    company: 'Acme',
    email: 'contato@acme.com',
    phone: '11999999999',
    logoUrl: null,
    active: true,
    createdAt: '2026-01-01T00:00:00Z',
  };

  beforeEach(async () => {
    clientApiMock = {
      create: vi.fn(),
      update: vi.fn(),
      uploadLogo: vi.fn(),
      removeLogo: vi.fn(),
      assignMembers: vi.fn().mockReturnValue(of(undefined)),
      getAssignedMembers: vi.fn().mockReturnValue(of([])),
    };
    memberApiMock = {
      list: vi.fn().mockReturnValue(of({ members: [], pendingInvites: [] })),
    };

    await TestBed.configureTestingModule({
      imports: [ClientFormDialogComponent, NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ClientApiService, useValue: clientApiMock },
        { provide: MemberApiService, useValue: memberApiMock },
      ],
    }).compileComponents();
  });

  it('should create the component', () => {
    const fixture = TestBed.createComponent(ClientFormDialogComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should have invalid form when name is empty', () => {
    const fixture = TestBed.createComponent(ClientFormDialogComponent);
    const component = fixture.componentInstance;

    component.form.patchValue({ name: '' });
    fixture.detectChanges();

    expect(component.form.invalid).toBe(true);
  });

  it('should have valid form when only name is filled', () => {
    const fixture = TestBed.createComponent(ClientFormDialogComponent);
    const component = fixture.componentInstance;

    component.form.patchValue({ name: 'Acme Corp' });
    fixture.detectChanges();

    expect(component.form.valid).toBe(true);
  });

  it('should have invalid form when email format is wrong', () => {
    const fixture = TestBed.createComponent(ClientFormDialogComponent);
    const component = fixture.componentInstance;

    component.form.patchValue({ name: 'Acme Corp', email: 'invalid-email' });
    fixture.detectChanges();

    expect(component.form.invalid).toBe(true);
    expect(component.form.get('email')?.hasError('email')).toBe(true);
  });

  it('should call clientApi.create on submit for new client', () => {
    clientApiMock.create.mockReturnValue(of(mockClient));

    const fixture = TestBed.createComponent(ClientFormDialogComponent);
    const component = fixture.componentInstance;

    component.form.patchValue({ name: 'Acme Corp', company: 'Acme', email: 'contato@acme.com', phone: '11999999999' });
    fixture.detectChanges();

    component.onSubmit();

    expect(clientApiMock.create).toHaveBeenCalledWith({
      name: 'Acme Corp',
      company: 'Acme',
      email: 'contato@acme.com',
      phone: '11999999999',
    });
  });

  it('should not include empty optional fields in request', () => {
    clientApiMock.create.mockReturnValue(of(mockClient));

    const fixture = TestBed.createComponent(ClientFormDialogComponent);
    const component = fixture.componentInstance;

    component.form.patchValue({ name: 'Acme Corp' });
    fixture.detectChanges();

    component.onSubmit();

    const calledWith = clientApiMock.create.mock.calls[0][0];
    expect(calledWith.name).toBe('Acme Corp');
    expect(calledWith.company).toBeUndefined();
    expect(calledWith.email).toBeUndefined();
    expect(calledWith.phone).toBeUndefined();
  });

  it('should set loading state during submit', () => {
    clientApiMock.create.mockReturnValue(of(mockClient));

    const fixture = TestBed.createComponent(ClientFormDialogComponent);
    const component = fixture.componentInstance;

    expect(component.loading()).toBe(false);

    component.form.patchValue({ name: 'Acme Corp' });
    fixture.detectChanges();

    component.onSubmit();

    // After synchronous observable completes, loading is reset
    expect(component.loading()).toBe(false);
  });

  it('should show error message on save failure', () => {
    clientApiMock.create.mockReturnValue(throwError(() => new Error('Server error')));

    const fixture = TestBed.createComponent(ClientFormDialogComponent);
    const component = fixture.componentInstance;

    component.form.patchValue({ name: 'Acme Corp' });
    fixture.detectChanges();

    component.onSubmit();

    expect(component.errorMessage()).toBe('Erro ao salvar cliente. Tente novamente.');
    expect(component.loading()).toBe(false);
  });

  it('should reset form when dialog is hidden', () => {
    const fixture = TestBed.createComponent(ClientFormDialogComponent);
    const component = fixture.componentInstance;

    component.form.patchValue({ name: 'Acme Corp', company: 'Acme' });
    component.errorMessage.set('Some error');
    fixture.detectChanges();

    component.onHide();
    fixture.detectChanges();

    expect(component.form.get('name')?.value).toBe('');
    expect(component.form.get('company')?.value).toBe('');
    expect(component.errorMessage()).toBeNull();
    expect(component.logoFile()).toBeNull();
    expect(component.logoPreview()).toBeNull();
  });

  it('should validate logo file type', () => {
    const fixture = TestBed.createComponent(ClientFormDialogComponent);
    const component = fixture.componentInstance;

    const invalidFile = new File(['data'], 'doc.pdf', { type: 'application/pdf' });
    const event = { target: { files: [invalidFile] } } as unknown as Event;

    component.onFileSelect(event);

    expect(component.logoFile()).toBeNull();
    expect(component.errorMessage()).toBe('Formato inválido. Use JPG ou PNG.');
  });

  it('should validate logo file size', () => {
    const fixture = TestBed.createComponent(ClientFormDialogComponent);
    const component = fixture.componentInstance;

    const largeContent = new Uint8Array(3 * 1024 * 1024); // 3MB
    const largeFile = new File([largeContent], 'logo.png', { type: 'image/png' });
    const event = { target: { files: [largeFile] } } as unknown as Event;

    component.onFileSelect(event);

    expect(component.logoFile()).toBeNull();
    expect(component.errorMessage()).toBe('Arquivo muito grande. Máximo 2MB.');
  });

  it('should call assignMembers on submit in edit mode', () => {
    clientApiMock.update.mockReturnValue(of(mockClient));
    clientApiMock.assignMembers.mockReturnValue(of(undefined));

    const fixture = TestBed.createComponent(ClientFormDialogComponent);
    const component = fixture.componentInstance;

    fixture.componentRef.setInput('client', mockClient);
    fixture.detectChanges();

    component.selectedMemberIds = [2, 3];
    component.form.patchValue({ name: 'Acme Corp' });
    fixture.detectChanges();

    component.onSubmit();

    expect(clientApiMock.update).toHaveBeenCalled();
    expect(clientApiMock.assignMembers).toHaveBeenCalledWith(1, [2, 3]);
  });

  it('should reset member state on hide', () => {
    const fixture = TestBed.createComponent(ClientFormDialogComponent);
    const component = fixture.componentInstance;

    component.availableMembers.set([{ label: 'John', value: 1 }]);
    component.selectedMemberIds = [1];
    fixture.detectChanges();

    component.onHide();
    fixture.detectChanges();

    expect(component.availableMembers()).toEqual([]);
    expect(component.selectedMemberIds).toEqual([]);
  });

  it('should filter out OWNER from available members', () => {
    const membersResponse = {
      members: [
        { id: 1, userId: 1, userName: 'Owner', userEmail: 'o@t.com', role: 'OWNER', position: 'ATENDIMENTO', createdAt: '' },
        { id: 2, userId: 2, userName: 'Manager', userEmail: 'm@t.com', role: 'MANAGER', position: 'SOCIAL_MEDIA', createdAt: '' },
        { id: 3, userId: 3, userName: 'Creative', userEmail: 'c@t.com', role: 'CREATIVE', position: 'DESIGNER_GRAFICO', createdAt: '' },
      ],
      pendingInvites: [],
    };
    memberApiMock.list.mockReturnValue(of(membersResponse));
    clientApiMock.getAssignedMembers.mockReturnValue(of([2]));

    const fixture = TestBed.createComponent(ClientFormDialogComponent);
    const component = fixture.componentInstance;

    fixture.componentRef.setInput('client', mockClient);
    fixture.detectChanges();

    expect(component.availableMembers().length).toBe(2);
    expect(component.availableMembers().find(m => m.label === 'Owner')).toBeUndefined();
    expect(component.selectedMemberIds).toEqual([2]);
  });
});
