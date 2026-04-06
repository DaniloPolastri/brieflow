export interface InviteInfo {
  workspaceName: string;
  email: string;
  role: string;
  position: string;
  invitedByName: string;
  userExists: boolean;
}

export interface AcceptInviteRequest {
  name?: string;
  password: string;
}
