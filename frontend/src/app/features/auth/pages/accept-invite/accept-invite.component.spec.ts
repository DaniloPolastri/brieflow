import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideRouter, ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { AcceptInviteComponent } from './accept-invite.component';
import { InviteApiService } from '@features/auth/services/invite-api.service';
import { AuthService } from '@core/services/auth.service';
import { InviteInfo } from '@features/auth/models/invite.model';
import { TokenResponse } from '@core/models/user.model';

const mockInviteInfoNewUser: InviteInfo = {
  workspaceName: 'Agência Criativa',
  email: 'novo@example.com',
  role: 'CREATIVE',
  position: 'DESIGNER_GRAFICO',
  invitedByName: 'João Silva',
  userExists: false,
};

const mockInviteInfoExistingUser: InviteInfo = {
  workspaceName: 'Agência Criativa',
  email: 'existente@example.com',
  role: 'MANAGER',
  position: 'SOCIAL_MEDIA',
  invitedByName: 'João Silva',
  userExists: true,
};

const mockTokenResponse: TokenResponse = {
  accessToken: 'access-token-123',
  refreshToken: 'refresh-token-456',
  expiresIn: 900,
  user: {
    id: 1,
    name: 'Test User',
    email: 'test@example.com',
    workspaceId: 10,
    workspaceName: 'Agência Criativa',
    role: 'CREATIVE',
    position: 'DESIGNER_GRAFICO',
  },
};

function createTestBed(inviteInfo: InviteInfo, token = 'test-token-abc') {
  const inviteApiSpy = {
    getInfo: vi.fn().mockReturnValue(of(inviteInfo)),
    accept: vi.fn().mockReturnValue(of(mockTokenResponse)),
  };
  const authServiceSpy = {
    handleAuthResponse: vi.fn(),
  };
  const activatedRouteMock = {
    snapshot: {
      queryParamMap: {
        get: (key: string) => (key === 'token' ? token : null),
      },
    },
  };

  return { inviteApiSpy, authServiceSpy, activatedRouteMock };
}

describe('AcceptInviteComponent', () => {
  let component: AcceptInviteComponent;
  let fixture: ComponentFixture<AcceptInviteComponent>;
  let inviteApiSpy: ReturnType<typeof createTestBed>['inviteApiSpy'];
  let authServiceSpy: ReturnType<typeof createTestBed>['authServiceSpy'];
  let router: Router;

  beforeEach(async () => {
    const deps = createTestBed(mockInviteInfoNewUser);
    inviteApiSpy = deps.inviteApiSpy;
    authServiceSpy = deps.authServiceSpy;

    await TestBed.configureTestingModule({
      imports: [AcceptInviteComponent],
      providers: [
        provideRouter([]),
        { provide: InviteApiService, useValue: inviteApiSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ActivatedRoute, useValue: deps.activatedRouteMock },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(AcceptInviteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should load invite info on init', () => {
    expect(inviteApiSpy.getInfo).toHaveBeenCalledWith('test-token-abc');
    expect(component.inviteInfo()).toEqual(mockInviteInfoNewUser);
    expect(component.loadingInfo()).toBe(false);
  });

  it('should show name field for new user (userExists: false)', () => {
    fixture.detectChanges();
    const nameControl = component.form.controls.name;
    expect(nameControl.validator).not.toBeNull();
    nameControl.setValue('');
    nameControl.markAsTouched();
    expect(nameControl.hasError('required')).toBe(true);
  });

  it('should hide name field for existing user (userExists: true)', async () => {
    const deps = createTestBed(mockInviteInfoExistingUser);

    await TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [AcceptInviteComponent],
      providers: [
        provideRouter([]),
        { provide: InviteApiService, useValue: deps.inviteApiSpy },
        { provide: AuthService, useValue: deps.authServiceSpy },
        { provide: ActivatedRoute, useValue: deps.activatedRouteMock },
      ],
    }).compileComponents();

    const fixtureExisting = TestBed.createComponent(AcceptInviteComponent);
    const componentExisting = fixtureExisting.componentInstance;
    fixtureExisting.detectChanges();

    expect(componentExisting.inviteInfo()?.userExists).toBe(true);
    expect(componentExisting.form.controls.name.validator).toBeNull();
  });

  it('should call inviteApi.accept on submit and navigate to dashboard', async () => {
    const navigateSpy = vi.spyOn(router, 'navigate');

    component.form.controls.name.setValue('Novo Usuario');
    component.form.controls.password.setValue('senha1234');

    component.onSubmit();

    expect(inviteApiSpy.accept).toHaveBeenCalledWith('test-token-abc', {
      name: 'Novo Usuario',
      password: 'senha1234',
    });
    expect(authServiceSpy.handleAuthResponse).toHaveBeenCalledWith(mockTokenResponse);
    expect(navigateSpy).toHaveBeenCalledWith(['/dashboard']);
  });
});
