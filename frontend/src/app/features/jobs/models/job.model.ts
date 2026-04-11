export type JobType =
  | 'POST_FEED'
  | 'STORIES'
  | 'CARROSSEL'
  | 'REELS_VIDEO'
  | 'BANNER'
  | 'LOGO'
  | 'OUTROS';

export type JobPriority = 'BAIXA' | 'NORMAL' | 'ALTA' | 'URGENTE';

export type JobStatus =
  | 'NOVO'
  | 'EM_CRIACAO'
  | 'REVISAO_INTERNA'
  | 'AGUARDANDO_APROVACAO'
  | 'APROVADO'
  | 'PUBLICADO';

export interface ClientSummary {
  id: number;
  name: string;
}

export interface MemberSummary {
  id: number;
  name: string;
}

export interface JobFile {
  id: number;
  originalFilename: string;
  mimeType: string;
  sizeBytes: number;
  uploadedAt: string;
  downloadUrl: string;
}

export interface Job {
  id: number;
  code: string;
  title: string;
  client: ClientSummary;
  type: JobType;
  description: string | null;
  deadline: string | null;
  priority: JobPriority;
  assignedCreative: MemberSummary | null;
  status: JobStatus;
  briefingData: Record<string, unknown>;
  archived: boolean;
  files: JobFile[];
  createdAt: string;
  updatedAt: string;
  createdByName: string;
}

export interface JobListItem {
  id: number;
  code: string;
  title: string;
  clientName: string;
  type: JobType;
  deadline: string | null;
  priority: JobPriority;
  assignedCreativeName: string | null;
  status: JobStatus;
  isOverdue: boolean;
}

export interface JobRequest {
  title: string;
  clientId: number;
  type: JobType;
  description?: string;
  deadline?: string;
  priority: JobPriority;
  assignedCreativeId?: number;
  briefingData: Record<string, unknown>;
}

export interface JobListFilters {
  search?: string;
  clientId?: number;
  type?: JobType;
  priority?: JobPriority;
  assignedCreativeId?: number;
  archived?: boolean;
}
