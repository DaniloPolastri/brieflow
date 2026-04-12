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
  assignedCreativeId: number | null;
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

// --- RF05: Kanban types ---

export interface UpdateJobStatusRequest {
  status: JobStatus;
  confirm: boolean;
}

export interface JobStatusResponse {
  id: number;
  code: string;
  previousStatus: JobStatus;
  newStatus: JobStatus;
  skippedSteps: boolean;
  applied: boolean;
}

export interface JobStatusEvent {
  jobId: number;
  previousStatus: JobStatus;
  newStatus: JobStatus;
}

export interface KanbanColumn {
  status: JobStatus;
  label: string;
  bgColor: string;
  textColor: string;
  dotColor: string;
}

export const KANBAN_COLUMNS: KanbanColumn[] = [
  { status: 'NOVO', label: 'Novo', bgColor: 'bg-blue-50', textColor: 'text-blue-800', dotColor: 'bg-blue-500' },
  { status: 'EM_CRIACAO', label: 'Em Criação', bgColor: 'bg-amber-50', textColor: 'text-amber-800', dotColor: 'bg-amber-500' },
  { status: 'REVISAO_INTERNA', label: 'Revisão Interna', bgColor: 'bg-purple-50', textColor: 'text-purple-800', dotColor: 'bg-purple-500' },
  { status: 'AGUARDANDO_APROVACAO', label: 'Aguardando Aprovação', bgColor: 'bg-orange-50', textColor: 'text-orange-800', dotColor: 'bg-orange-500' },
  { status: 'APROVADO', label: 'Aprovado', bgColor: 'bg-emerald-50', textColor: 'text-emerald-800', dotColor: 'bg-emerald-500' },
  { status: 'PUBLICADO', label: 'Publicado', bgColor: 'bg-green-50', textColor: 'text-green-800', dotColor: 'bg-green-500' },
];
