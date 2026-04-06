export type MemberRole = 'OWNER' | 'MANAGER' | 'CREATIVE';

export type MemberPosition =
  | 'DESIGNER_GRAFICO'
  | 'EDITOR_DE_VIDEO'
  | 'SOCIAL_MEDIA'
  | 'COPYWRITER'
  | 'GESTOR_DE_TRAFEGO'
  | 'DIRETOR_DE_ARTE'
  | 'ATENDIMENTO'
  | 'FOTOGRAFO'
  | 'ILUSTRADOR'
  | 'MOTION_DESIGNER';

export const MEMBER_ROLE_LABELS: Record<MemberRole, string> = {
  OWNER: 'Proprietário',
  MANAGER: 'Gestor',
  CREATIVE: 'Criativo',
};

export const MEMBER_POSITION_LABELS: Record<MemberPosition, string> = {
  DESIGNER_GRAFICO: 'Designer Gráfico',
  EDITOR_DE_VIDEO: 'Editor de Vídeo',
  SOCIAL_MEDIA: 'Social Media',
  COPYWRITER: 'Copywriter',
  GESTOR_DE_TRAFEGO: 'Gestor de Tráfego',
  DIRETOR_DE_ARTE: 'Diretor de Arte',
  ATENDIMENTO: 'Atendimento',
  FOTOGRAFO: 'Fotógrafo',
  ILUSTRADOR: 'Ilustrador',
  MOTION_DESIGNER: 'Motion Designer',
};

export interface Member {
  id: number;
  userId: number;
  userName: string;
  userEmail: string;
  role: MemberRole;
  position: MemberPosition;
  createdAt: string;
}

export interface InviteMemberRequest {
  email: string;
  role: MemberRole;
  position: MemberPosition;
}

export interface InviteResponse {
  id: number;
  email: string;
  role: string;
  position: string;
  inviteLink: string;
  expiresAt: string;
}

export interface UpdateMemberRoleRequest {
  role: MemberRole;
}

export interface MembersListResponse {
  members: Member[];
  pendingInvites: InviteResponse[];
}
