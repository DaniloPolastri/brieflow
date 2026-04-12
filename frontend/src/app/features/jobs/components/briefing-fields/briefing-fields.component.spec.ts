import { TestBed, ComponentFixture } from '@angular/core/testing';
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule,
} from '@angular/forms';
import { BriefingFieldsComponent } from './briefing-fields.component';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('BriefingFieldsComponent', () => {
  let fixture: ComponentFixture<BriefingFieldsComponent>;
  let component: BriefingFieldsComponent;
  let fb: FormBuilder;
  let form: FormGroup;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [BriefingFieldsComponent, ReactiveFormsModule, NoopAnimationsModule],
    });
    fb = TestBed.inject(FormBuilder);
    form = fb.group({ briefingData: fb.group({}) });
    fixture = TestBed.createComponent(BriefingFieldsComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('formGroup', form.get('briefingData') as FormGroup);
    fixture.componentRef.setInput('type', 'POST_FEED');
    fixture.detectChanges();
  });

  it('should render POST_FEED fields', () => {
    const group = form.get('briefingData') as FormGroup;
    expect(group.get('captionText')).toBeTruthy();
    expect(group.get('format')).toBeTruthy();
    expect(group.get('colorPalette')).toBeTruthy();
  });

  it('should mark required fields with validators', () => {
    const group = form.get('briefingData') as FormGroup;
    const caption = group.get('captionText')!;
    caption.setValue('');
    expect(caption.hasValidator(Validators.required)).toBe(true);
  });

  it('should replace fields when type changes to STORIES', () => {
    fixture.componentRef.setInput('type', 'STORIES');
    fixture.detectChanges();
    const group = form.get('briefingData') as FormGroup;
    expect(group.get('captionText')).toBeNull();
    expect(group.get('text')).toBeTruthy();
    expect(group.get('format')).toBeTruthy();
  });

  it('should generate N inputs for CARROSSEL slideTexts when slideCount changes', () => {
    fixture.componentRef.setInput('type', 'CARROSSEL');
    fixture.detectChanges();
    const group = form.get('briefingData') as FormGroup;
    group.get('slideCount')!.setValue(3);
    fixture.detectChanges();
    const slides = group.get('slideTexts') as any;
    expect(slides.controls.length).toBe(3);
  });

  it('should validate number min/max on slideCount', () => {
    fixture.componentRef.setInput('type', 'CARROSSEL');
    fixture.detectChanges();
    const group = form.get('briefingData') as FormGroup;
    const sc = group.get('slideCount')!;
    sc.setValue(1);
    expect(sc.invalid).toBe(true);
    sc.setValue(11);
    expect(sc.invalid).toBe(true);
    sc.setValue(5);
    expect(sc.valid).toBe(true);
  });
});
