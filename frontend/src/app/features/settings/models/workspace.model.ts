export interface Workspace {
  id: number;
  name: string;
  slug: string;
}

export interface UpdateWorkspaceRequest {
  name: string;
}
