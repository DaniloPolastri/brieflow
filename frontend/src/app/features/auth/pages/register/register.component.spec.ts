import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { RegisterComponent } from './register.component';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have invalid form when empty', () => {
    expect(component.form.valid).toBe(false);
  });

  it('should have valid form when filled correctly', () => {
    component.form.patchValue({
      name: 'John Doe',
      email: 'john@test.com',
      password: 'password123',
      confirmPassword: 'password123',
    });
    expect(component.form.valid).toBe(true);
  });

  it('should be invalid when passwords do not match', () => {
    component.form.patchValue({
      name: 'John Doe',
      email: 'john@test.com',
      password: 'password123',
      confirmPassword: 'different',
    });
    expect(component.form.hasError('passwordMismatch')).toBe(true);
  });

  it('should be invalid when password is too short', () => {
    component.form.controls.password.setValue('short');
    component.form.controls.password.markAsTouched();
    expect(component.form.controls.password.hasError('minlength')).toBe(true);
  });
});
