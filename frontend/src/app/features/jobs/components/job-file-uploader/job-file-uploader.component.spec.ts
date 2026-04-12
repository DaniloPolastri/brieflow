import { TestBed, ComponentFixture } from '@angular/core/testing';
import { JobFileUploaderComponent } from './job-file-uploader.component';
import { JobApiService } from '@features/jobs/services/job-api.service';
import { HttpEventType } from '@angular/common/http';
import { of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import type { JobFile } from '@features/jobs/models/job.model';

describe('JobFileUploaderComponent', () => {
  let fixture: ComponentFixture<JobFileUploaderComponent>;
  let component: JobFileUploaderComponent;
  let apiSpy: {
    uploadFile: ReturnType<typeof vi.fn>;
    deleteFile: ReturnType<typeof vi.fn>;
    downloadUrl: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    apiSpy = {
      uploadFile: vi.fn().mockReturnValue(
        of({ type: HttpEventType.Response, body: { id: 99 } as JobFile }),
      ),
      deleteFile: vi.fn().mockReturnValue(of(void 0)),
      downloadUrl: vi.fn().mockReturnValue('/download/1/2'),
    };
    TestBed.configureTestingModule({
      imports: [JobFileUploaderComponent, NoopAnimationsModule],
      providers: [{ provide: JobApiService, useValue: apiSpy }],
    });
    fixture = TestBed.createComponent(JobFileUploaderComponent);
    component = fixture.componentInstance;
  });

  it('should reject MIME type not allowed', () => {
    fixture.componentRef.setInput('mode', 'staging');
    fixture.detectChanges();
    const file = new File(['x'], 'file.exe', {
      type: 'application/x-msdownload',
    });
    component.addFiles([file]);
    expect(component.pendingFiles().length).toBe(0);
    expect(component.errors().length).toBeGreaterThan(0);
  });

  it('should reject file larger than 50MB', () => {
    fixture.componentRef.setInput('mode', 'staging');
    fixture.detectChanges();
    const big = new File([new Uint8Array(51 * 1024 * 1024)], 'big.pdf', {
      type: 'application/pdf',
    });
    component.addFiles([big]);
    expect(component.pendingFiles().length).toBe(0);
  });

  it('should stage valid files in staging mode without uploading', () => {
    fixture.componentRef.setInput('mode', 'staging');
    fixture.detectChanges();
    const file = new File(['data'], 'ref.jpg', { type: 'image/jpeg' });
    component.addFiles([file]);
    expect(component.pendingFiles().length).toBe(1);
    expect(apiSpy.uploadFile).not.toHaveBeenCalled();
  });

  it('should upload immediately in direct mode', () => {
    fixture.componentRef.setInput('mode', 'direct');
    fixture.componentRef.setInput('jobId', 1);
    fixture.detectChanges();
    const file = new File(['data'], 'ref.jpg', { type: 'image/jpeg' });
    component.addFiles([file]);
    expect(apiSpy.uploadFile).toHaveBeenCalledWith(1, file);
  });

  it('should emit filesStaged after adding in staging mode', () => {
    fixture.componentRef.setInput('mode', 'staging');
    fixture.detectChanges();
    const spy = vi.fn();
    component.filesStaged.subscribe(spy);
    const file = new File(['data'], 'ref.jpg', { type: 'image/jpeg' });
    component.addFiles([file]);
    expect(spy).toHaveBeenCalled();
  });

  it('should call deleteFile in direct mode when removing existing file', () => {
    fixture.componentRef.setInput('mode', 'direct');
    fixture.componentRef.setInput('jobId', 1);
    fixture.detectChanges();
    const existing: JobFile = {
      id: 5,
      originalFilename: 'a.jpg',
      mimeType: 'image/jpeg',
      sizeBytes: 100,
      uploadedAt: '2026-04-11',
      downloadUrl: '/x',
    };
    component.removeExistingFile(existing);
    expect(apiSpy.deleteFile).toHaveBeenCalledWith(1, 5);
  });
});
