import {
  ChangeDetectionStrategy,
  Component,
  input,
  effect,
  inject,
  computed,
  DestroyRef,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  FormGroup,
  FormControl,
  FormArray,
  Validators,
  ReactiveFormsModule,
} from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { InputNumberModule } from 'primeng/inputnumber';
import { SelectModule } from 'primeng/select';
import { ButtonModule } from 'primeng/button';
import {
  BRIEFING_SCHEMAS,
  type BriefingFieldSchema,
} from '@features/jobs/models/briefing-schemas';
import type { JobType } from '@features/jobs/models/job.model';

@Component({
  selector: 'app-briefing-fields',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    InputTextModule,
    TextareaModule,
    InputNumberModule,
    SelectModule,
    ButtonModule,
  ],
  templateUrl: './briefing-fields.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BriefingFieldsComponent {
  readonly type = input.required<JobType>();
  readonly formGroup = input.required<FormGroup>();

  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    effect(() => {
      const t = this.type();
      const fg = this.formGroup();
      this.buildControls(fg, BRIEFING_SCHEMAS[t]);
    });
  }

  private buildControls(fg: FormGroup, schema: BriefingFieldSchema[]): void {
    Object.keys(fg.controls).forEach((key) => fg.removeControl(key));

    schema.forEach((field) => {
      const validators = field.required ? [Validators.required] : [];
      if (field.type === 'number') {
        if (field.min !== undefined) validators.push(Validators.min(field.min));
        if (field.max !== undefined) validators.push(Validators.max(field.max));
        fg.addControl(field.key, new FormControl<number | null>(null, validators));
      } else if (field.type === 'dynamic-list') {
        fg.addControl(field.key, new FormArray<FormControl<string | null>>([]));
      } else {
        fg.addControl(field.key, new FormControl<string | null>(null, validators));
      }
    });

    this.wireDynamicListsIfNeeded(fg, schema);
  }

  private wireDynamicListsIfNeeded(
    fg: FormGroup,
    schema: BriefingFieldSchema[],
  ): void {
    const slideCount = schema.find((s) => s.key === 'slideCount');
    const slideTexts = schema.find(
      (s) => s.key === 'slideTexts' && s.type === 'dynamic-list',
    );
    if (slideCount && slideTexts) {
      const countCtrl = fg.get('slideCount') as FormControl<number | null>;
      const arr = fg.get('slideTexts') as FormArray<FormControl<string | null>>;
      countCtrl.valueChanges
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe((count) => {
          const n = count ?? 0;
          while (arr.length < n)
            arr.push(new FormControl<string | null>(null));
          while (arr.length > n) arr.removeAt(arr.length - 1);
        });
    }
  }

  readonly currentSchema = computed<BriefingFieldSchema[]>(
    () => BRIEFING_SCHEMAS[this.type()],
  );

  readonly selectOptionsMap = computed<Record<string, { label: string; value: string }[]>>(() => {
    const map: Record<string, { label: string; value: string }[]> = {};
    for (const field of this.currentSchema()) {
      if (field.type === 'select') {
        map[field.key] = (field.options ?? []).map((o) => ({ label: o, value: o }));
      }
    }
    return map;
  });

  schema(): BriefingFieldSchema[] {
    return this.currentSchema();
  }

  asFormArray(key: string): FormArray {
    return this.formGroup().get(key) as FormArray;
  }

  optionsFor(key: string): { label: string; value: string }[] {
    return this.selectOptionsMap()[key] ?? [];
  }

  isInvalid(key: string): boolean {
    const ctrl = this.formGroup().get(key);
    return !!ctrl && ctrl.invalid && (ctrl.touched || ctrl.dirty);
  }
}
