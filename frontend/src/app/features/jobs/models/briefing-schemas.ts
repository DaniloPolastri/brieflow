import type { JobType } from './job.model';

export type BriefingFieldType =
  | 'text'
  | 'textarea'
  | 'number'
  | 'select'
  | 'dynamic-list';

export interface BriefingFieldSchema {
  key: string;
  label: string;
  type: BriefingFieldType;
  required?: boolean;
  options?: string[];
  min?: number;
  max?: number;
}

export const BRIEFING_SCHEMAS: Record<JobType, BriefingFieldSchema[]> = {
  POST_FEED: [
    { key: 'captionText', label: 'Texto da legenda', type: 'textarea', required: true },
    { key: 'format', label: 'Formato', type: 'select', options: ['1:1', '4:5'], required: true },
    { key: 'colorPalette', label: 'Paleta de cores', type: 'text' },
    { key: 'visualReferences', label: 'Referências visuais', type: 'textarea' },
  ],
  STORIES: [
    { key: 'text', label: 'Texto', type: 'textarea', required: true },
    { key: 'format', label: 'Formato', type: 'select', options: ['9:16'], required: true },
    { key: 'cta', label: 'CTA', type: 'text' },
    { key: 'visualReferences', label: 'Referências visuais', type: 'textarea' },
  ],
  CARROSSEL: [
    { key: 'slideCount', label: 'Número de slides', type: 'number', required: true, min: 2, max: 10 },
    { key: 'slideTexts', label: 'Texto por slide', type: 'dynamic-list' },
    { key: 'format', label: 'Formato', type: 'select', options: ['1:1', '4:5'] },
  ],
  REELS_VIDEO: [
    { key: 'duration', label: 'Duração (segundos)', type: 'number', required: true },
    { key: 'script', label: 'Roteiro/Storyboard', type: 'textarea', required: true },
    { key: 'audioReference', label: 'Referência de áudio', type: 'text' },
    { key: 'visualReferences', label: 'Referências visuais', type: 'textarea' },
  ],
  BANNER: [
    { key: 'dimensions', label: 'Dimensões', type: 'text', required: true },
    { key: 'text', label: 'Texto', type: 'textarea', required: true },
    { key: 'cta', label: 'CTA', type: 'text' },
  ],
  LOGO: [
    { key: 'desiredStyle', label: 'Estilo desejado', type: 'textarea', required: true },
    { key: 'colorReferences', label: 'Referências de cor', type: 'text' },
    { key: 'visualReferences', label: 'Referências visuais', type: 'textarea' },
  ],
  OUTROS: [
    { key: 'freeDescription', label: 'Descrição livre', type: 'textarea', required: true },
  ],
};
