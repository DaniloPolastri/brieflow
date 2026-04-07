import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MemberListComponent } from './member-list.component';
import { MemberApiService } from '@features/members/services/member-api.service';
import { StorageService } from '@core/services/storage.service';
import { ConfirmationService } from 'primeng/api';
import { of } from 'rxjs';
import { Member, InviteResponse } from '@features/members/models/member.model';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

const mockMembers: Member[] = [
  {
    id: 1,
    userId: 10,
    userName: 'Ana Lima',
    userEmail: 'ana@agency.com',
    role: 'OWNER',
    position: 'DESIGNER_GRAFICO',
    createdAt: '2024-01-01T00:00:00Z',
  },
  {
    id: 2,
    userId: 11,
    userName: 'Bruno Costa',
    userEmail: 'bruno@agency.com',
    role: 'MANAGER',
    position: 'SOCIAL_MEDIA',
    createdAt: '2024-01-02T00:00:00Z',
  },
];

const mockPendingInvites: InviteResponse[] = [
  {
    id: 100,
    email: 'pending@agency.com',
    role: 'CREATIVE',
    position: 'COPYWRITER',
    inviteLink: 'http://localhost/invite/abc123',
    expiresAt: '2024-12-31T00:00:00Z',
  },
];

describe('MemberListComponent', () => {
  let component: MemberListComponent;
  let fixture: ComponentFixture<MemberListComponent>;
  let memberApiSpy: { list: ReturnType<typeof vi.fn>; remove: ReturnType<typeof vi.fn> };
  let storageSpy: { getUser: ReturnType<typeof vi.fn> };

  function setup(role: string = 'OWNER') {
    memberApiSpy = {
      list: vi.fn().mockReturnValue(of({ members: mockMembers, pendingInvites: mockPendingInvites })),
      remove: vi.fn().mockReturnValue(of(undefined)),
    };
    storageSpy = {
      getUser: vi.fn().mockReturnValue({ id: 10, name: 'Ana Lima', email: 'ana@agency.com', workspaceId: 1, workspaceName: 'Agency', role, position: 'DESIGNER_GRAFICO' }),
    };

    TestBed.configureTestingModule({
      imports: [MemberListComponent, NoopAnimationsModule],
      providers: [
        { provide: MemberApiService, useValue: memberApiSpy },
        { provide: StorageService, useValue: storageSpy },
        ConfirmationService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    fixture = TestBed.createComponent(MemberListComponent);
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

  it('should load members on init', () => {
    setup();
    expect(memberApiSpy.list).toHaveBeenCalledOnce();
    expect(component.members()).toEqual(mockMembers);
    expect(component.pendingInvites()).toEqual(mockPendingInvites);
    expect(component.loading()).toBe(false);
  });

  it('should display members in table', () => {
    setup();
    fixture.detectChanges();
    const compiled: HTMLElement = fixture.nativeElement;
    expect(compiled.textContent).toContain('Ana Lima');
    expect(compiled.textContent).toContain('Bruno Costa');
    expect(compiled.textContent).toContain('ana@agency.com');
    expect(compiled.textContent).toContain('bruno@agency.com');
  });

  it('should show invite button for OWNER', () => {
    setup('OWNER');
    fixture.detectChanges();
    const compiled: HTMLElement = fixture.nativeElement;
    expect(compiled.textContent).toContain('Convidar membro');
  });

  it('should show invite button for MANAGER', () => {
    setup('MANAGER');
    fixture.detectChanges();
    const compiled: HTMLElement = fixture.nativeElement;
    expect(compiled.textContent).toContain('Convidar membro');
  });

  it('should hide invite button for CREATIVE', () => {
    setup('CREATIVE');
    fixture.detectChanges();
    const compiled: HTMLElement = fixture.nativeElement;
    expect(compiled.textContent).not.toContain('Convidar membro');
  });

  it('should call removeMember with confirmation', () => {
    setup('OWNER');
    // ConfirmationService is provided at component level, so use the component's injector
    const confirmService = fixture.debugElement.injector.get(ConfirmationService);
    const confirmSpy = vi.spyOn(confirmService, 'confirm').mockImplementation((cfg: any) => {
      cfg.accept();
      return confirmService;
    });

    component.removeMember(mockMembers[1]);

    expect(confirmSpy).toHaveBeenCalled();
    expect(memberApiSpy.remove).toHaveBeenCalledWith(mockMembers[1].id);
  });

  it('should display pending invites', () => {
    setup();
    fixture.detectChanges();
    const compiled: HTMLElement = fixture.nativeElement;
    expect(compiled.textContent).toContain('pending@agency.com');
    expect(compiled.textContent).toContain('Convites pendentes');
    expect(compiled.textContent).toContain('Pendente');
  });
});
