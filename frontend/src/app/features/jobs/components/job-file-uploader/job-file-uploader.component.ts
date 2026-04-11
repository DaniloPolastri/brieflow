import {
  ChangeDetectionStrategy,
  Component,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { HttpEventType } from '@angular/common/http';
import { ButtonModule } from 'primeng/button';
import { ProgressBarModule } from 'primeng/progressbar';
import { JobApiService } from '@features/jobs/services/job-api.service';
import type { JobFile } from '@features/jobs/models/job.model';

const ALLOWED_MIME = [
  'image/jpeg',
  'image/png',
  'image/webp',
  'image/gif',
  'application/pdf',
  'video/mp4',
  'video/quicktime',
];
const MAX_SIZE = 50 * 1024 * 1024;

export type UploaderMode = 'staging' | 'direct';

export interface PendingFile {
  file: File;
  progress: number;
  status: 'pending' | 'uploading' | 'done' | 'error';
  errorMessage?: string;
}

@Component({
  selector: 'app-job-file-uploader',
  standalone: true,
  imports: [ButtonModule, ProgressBarModule],
  templateUrl: './job-file-uploader.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JobFileUploaderComponent {
  private readonly api = inject(JobApiService);

  readonly mode = input.required<UploaderMode>();
  readonly jobId = input<number | null>(null);
  readonly existingFiles = input<JobFile[]>([]);
  readonly canManage = input<boolean>(true);

  readonly pendingFiles = signal<PendingFile[]>([]);
  readonly errors = signal<string[]>([]);
  readonly isDragging = signal(false);

  readonly filesStaged = output<File[]>();
  readonly fileUploaded = output<JobFile>();
  readonly fileDeleted = output<number>();

  addFiles(files: File[]): void {
    const valid: File[] = [];
    const errs: string[] = [];
    for (const f of files) {
      if (!ALLOWED_MIME.includes(f.type)) {
        errs.push(`Formato não suportado: ${f.name}`);
        continue;
      }
      if (f.size > MAX_SIZE) {
        errs.push(`${f.name} excede o limite de 50MB`);
        continue;
      }
      valid.push(f);
    }
    this.errors.set(errs);
    if (valid.length === 0) return;

    if (this.mode() === 'staging') {
      this.pendingFiles.update((list) => [
        ...list,
        ...valid.map((f) => ({
          file: f,
          progress: 0,
          status: 'pending' as const,
        })),
      ]);
      this.filesStaged.emit(valid);
    } else {
      valid.forEach((f) => this.uploadNow(f));
    }
  }

  onFileSelect(event: Event): void {
    const inputEl = event.target as HTMLInputElement;
    const list = Array.from(inputEl.files ?? []);
    this.addFiles(list);
    inputEl.value = '';
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(true);
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(false);
    const list = Array.from(event.dataTransfer?.files ?? []);
    this.addFiles(list);
  }

  uploadPending(jobId: number): void {
    this.pendingFiles().forEach((pf) => {
      if (pf.status === 'pending') {
        this.uploadOne(pf, jobId);
      }
    });
  }

  retry(pf: PendingFile): void {
    const id = this.jobId();
    if (id !== null) this.uploadOne(pf, id);
  }

  removePending(pf: PendingFile): void {
    this.pendingFiles.update((list) => list.filter((p) => p !== pf));
  }

  removeExistingFile(file: JobFile): void {
    const id = this.jobId();
    if (id === null) return;
    this.api.deleteFile(id, file.id).subscribe({
      next: () => this.fileDeleted.emit(file.id),
      error: (err) => {
        console.error('Erro ao remover arquivo:', err);
        this.errors.update((list) => [
          ...list,
          `Erro ao remover ${file.originalFilename}`,
        ]);
      },
    });
  }

  private uploadNow(file: File): void {
    const pf: PendingFile = { file, progress: 0, status: 'uploading' };
    this.pendingFiles.update((list) => [...list, pf]);
    const id = this.jobId();
    if (id !== null) this.uploadOne(pf, id);
  }

  private uploadOne(pf: PendingFile, jobId: number): void {
    pf.status = 'uploading';
    this.api.uploadFile(jobId, pf.file).subscribe({
      next: (event) => {
        if (event.type === HttpEventType.UploadProgress && event.total) {
          pf.progress = Math.round((100 * event.loaded) / event.total);
          this.pendingFiles.update((list) => [...list]);
        } else if (event.type === HttpEventType.Response && event.body) {
          pf.status = 'done';
          pf.progress = 100;
          this.pendingFiles.update((list) => [...list]);
          this.fileUploaded.emit(event.body);
        }
      },
      error: (err) => {
        pf.status = 'error';
        pf.errorMessage = err?.error?.message ?? 'Falha no upload';
        this.pendingFiles.update((list) => [...list]);
      },
    });
  }

  getFileIcon(mimeType: string): string {
    if (mimeType.startsWith('image/')) return 'pi pi-image';
    if (mimeType.startsWith('video/')) return 'pi pi-video';
    if (mimeType === 'application/pdf') return 'pi pi-file-pdf';
    return 'pi pi-file';
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  downloadUrl(fileId: number): string {
    const id = this.jobId();
    if (id === null) return '#';
    return this.api.downloadUrl(id, fileId);
  }
}
