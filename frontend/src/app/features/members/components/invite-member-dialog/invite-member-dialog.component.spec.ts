import { TestBed } from '@angular/core/testing';
import { Component, signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InviteMemberDialogComponent } from './invite-member-dialog.component';
import { MemberApiService } from '../../services/member-api.service';
import { InviteResponse } from '../../models/member.model';

describe('InviteMemberDialogComponent', () => {
  let memberApiMock: { invite: ReturnType<typeof vi.fn> };

  const mockInviteResponse: InviteResponse = {
    id: 1,
    email: 'test@agencia.com',
    role: 'CREATIVE',
    position: 'DESIGNER_GRAFICO',
    inviteLink: 'http://localhost/invite/token-abc123',
    expiresAt: '2026-05-01T00:00:00Z',
  };

  beforeEach(async () => {
    memberApiMock = { invite: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [InviteMemberDialogComponent, NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MemberApiService, useValue: memberApiMock },
      ],
    }).compileComponents();
  });

  it('should create the component', () => {
    const fixture = TestBed.createComponent(InviteMemberDialogComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should have invalid form when empty', () => {
    const fixture = TestBed.createComponent(InviteMemberDialogComponent);
    const component = fixture.componentInstance;

    component.form.reset({ email: '', role: '', position: '' });
    fixture.detectChanges();

    expect(component.form.invalid).toBe(true);
  });

  it('should have valid form when all fields filled', () => {
    const fixture = TestBed.createComponent(InviteMemberDialogComponent);
    const component = fixture.componentInstance;

    component.form.setValue({
      email: 'membro@agencia.com',
      role: 'MANAGER',
      position: 'SOCIAL_MEDIA',
    });
    fixture.detectChanges();

    expect(component.form.valid).toBe(true);
  });

  it('should call memberApi.invite on submit', () => {
    memberApiMock.invite.mockReturnValue(of(mockInviteResponse));

    const fixture = TestBed.createComponent(InviteMemberDialogComponent);
    const component = fixture.componentInstance;

    component.form.setValue({
      email: 'membro@agencia.com',
      role: 'CREATIVE',
      position: 'DESIGNER_GRAFICO',
    });
    fixture.detectChanges();

    component.onSubmit();

    expect(memberApiMock.invite).toHaveBeenCalledWith({
      email: 'membro@agencia.com',
      role: 'CREATIVE',
      position: 'DESIGNER_GRAFICO',
    });
  });

  it('should show invite link after successful invite', () => {
    memberApiMock.invite.mockReturnValue(of(mockInviteResponse));

    const fixture = TestBed.createComponent(InviteMemberDialogComponent);
    const component = fixture.componentInstance;

    component.form.setValue({
      email: 'membro@agencia.com',
      role: 'CREATIVE',
      position: 'DESIGNER_GRAFICO',
    });
    fixture.detectChanges();

    component.onSubmit();
    fixture.detectChanges();

    expect(component.inviteLink()).toBe('http://localhost/invite/token-abc123');
    expect(component.loading()).toBe(false);
  });

  it('should reset form when dialog is hidden', () => {
    memberApiMock.invite.mockReturnValue(of(mockInviteResponse));

    const fixture = TestBed.createComponent(InviteMemberDialogComponent);
    const component = fixture.componentInstance;

    // Put the component in "link generated" state
    component.form.setValue({
      email: 'membro@agencia.com',
      role: 'CREATIVE',
      position: 'DESIGNER_GRAFICO',
    });
    component.onSubmit();
    fixture.detectChanges();

    expect(component.inviteLink()).not.toBeNull();

    // Hide the dialog
    component.onHide();
    fixture.detectChanges();

    expect(component.inviteLink()).toBeNull();
    expect(component.copied()).toBe(false);
    expect(component.form.get('email')?.value).toBe('');
  });
});
