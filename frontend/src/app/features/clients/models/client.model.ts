export interface Client {
  id: number;
  name: string;
  company: string | null;
  email: string | null;
  phone: string | null;
  logoUrl: string | null;
  active: boolean;
  createdAt: string;
}

export interface ClientRequest {
  name: string;
  company?: string;
  email?: string;
  phone?: string;
}
