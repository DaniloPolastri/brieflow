import { TestBed, ComponentFixture } from '@angular/core/testing';
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule,
} from '@angular/forms';
import { JobSummarySidebarComponent } from './job-summary-sidebar.component';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('JobSummarySidebarComponent', () => {
  let fixture: ComponentFixture<JobSummarySidebarComponent>;
  let component: JobSummarySidebarComponent;
  let form: FormGroup;
  let fb: FormBuilder;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        JobSummarySidebarComponent,
        ReactiveFormsModule,
        NoopAnimationsModule,
      ],
    });
    fb = TestBed.inject(FormBuilder);
    form = fb.nonNullable.group({
      title: ['', [Validators.required]],
      clientId: [null as number | null, [Validators.required]],
      type: ['POST_FEED', [Validators.required]],
      priority: ['NORMAL', [Validators.required]],
      deadline: [''],
      assignedCreativeId: [null as number | null],
    });

    fixture = TestBed.createComponent(JobSummarySidebarComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('form', form);
    fixture.componentRef.setInput('mode', 'create');
    fixture.componentRef.setInput('clientName', null);
    fixture.componentRef.setInput('creativeName', null);
    fixture.detectChanges();
  });

  it('should show "Salvar Job" button in create mode', () => {
    expect(fixture.nativeElement.textContent).toContain('Salvar Job');
  });

  it('should show "Atualizar Job" in edit mode', () => {
    fixture.componentRef.setInput('mode', 'edit');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Atualizar Job');
  });

  it('should disable save button when form is invalid', () => {
    expect(component.canSave()).toBe(false);
  });

  it('should enable save button when form is valid', () => {
    form.patchValue({ title: 'Post', clientId: 1 });
    fixture.detectChanges();
    expect(component.canSave()).toBe(true);
  });

  it('should emit save when save button triggers', () => {
    const spy = vi.fn();
    component.save.subscribe(spy);
    form.patchValue({ title: 'Post', clientId: 1 });
    fixture.detectChanges();
    component.onSave();
    expect(spy).toHaveBeenCalled();
  });

  it('should list missing required fields', () => {
    const missing = component.missingRequiredFields();
    expect(missing).toContain('Título');
    expect(missing).toContain('Cliente');
  });
});
