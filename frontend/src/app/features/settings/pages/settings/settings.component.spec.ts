import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { SettingsComponent } from './settings.component';
import { WorkspaceApiService } from '../../services/workspace-api.service';
import { Workspace } from '../../models/workspace.model';

describe('SettingsComponent', () => {
  let workspaceApiMock: {
    get: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };

  const mockWorkspace: Workspace = {
    id: 1,
    name: 'Minha Agência',
    slug: 'minha-agencia',
  };

  beforeEach(async () => {
    workspaceApiMock = {
      get: vi.fn(),
      update: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [SettingsComponent, NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: WorkspaceApiService, useValue: workspaceApiMock },
      ],
    }).compileComponents();
  });

  it('should create the component', () => {
    workspaceApiMock.get.mockReturnValue(of(mockWorkspace));
    const fixture = TestBed.createComponent(SettingsComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should load workspace name on init', () => {
    workspaceApiMock.get.mockReturnValue(of(mockWorkspace));

    const fixture = TestBed.createComponent(SettingsComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(workspaceApiMock.get).toHaveBeenCalled();
    expect(component.form.controls.name.value).toBe('Minha Agência');
    expect(component.loading()).toBe(false);
  });

  it('should call workspaceApi.update on submit', () => {
    workspaceApiMock.get.mockReturnValue(of(mockWorkspace));
    workspaceApiMock.update.mockReturnValue(of(mockWorkspace));

    const fixture = TestBed.createComponent(SettingsComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.form.controls.name.setValue('Nova Agência');
    component.onSubmit();

    expect(workspaceApiMock.update).toHaveBeenCalledWith({ name: 'Nova Agência' });
  });

  it('should show success message after update', () => {
    workspaceApiMock.get.mockReturnValue(of(mockWorkspace));
    workspaceApiMock.update.mockReturnValue(of(mockWorkspace));

    const fixture = TestBed.createComponent(SettingsComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.form.controls.name.setValue('Nova Agência');
    component.onSubmit();
    fixture.detectChanges();

    expect(component.successMessage()).toBe('Workspace atualizado com sucesso!');
    expect(component.saving()).toBe(false);
  });
});
