# BriefFlow — Design Guidelines

**Referências visuais:** Operand, Linear, Resend  
**Estilo:** Clean, moderno, profissional, light mode  
**UI Library:** PrimeNG + Tailwind CSS

---

## Paleta de Cores

### Cores Primárias

| Nome | Hex | Uso |
|------|-----|-----|
| Primary | `#6366F1` | Botões primários, links, elementos de destaque (Indigo-500) |
| Primary Hover | `#4F46E5` | Hover de botões primários (Indigo-600) |
| Primary Light | `#EEF2FF` | Backgrounds de badge, tags, seleção (Indigo-50) |
| Primary Dark | `#3730A3` | Texto sobre fundo claro quando precisa de mais contraste (Indigo-800) |

### Cores Neutras

| Nome | Hex | Uso |
|------|-----|-----|
| White | `#FFFFFF` | Background principal |
| Gray 50 | `#F9FAFB` | Background de seções alternadas, cards |
| Gray 100 | `#F3F4F6` | Background de inputs, sidebar |
| Gray 200 | `#E5E7EB` | Bordas de cards, divisores |
| Gray 300 | `#D1D5DB` | Bordas de inputs |
| Gray 400 | `#9CA3AF` | Placeholder text, ícones inativos |
| Gray 500 | `#6B7280` | Texto secundário, labels |
| Gray 700 | `#374151` | Texto de corpo principal |
| Gray 900 | `#111827` | Títulos, headings |

### Cores Semânticas

| Nome | Hex | Uso |
|------|-----|-----|
| Success | `#10B981` | Aprovado, concluído, online (Emerald-500) |
| Success Light | `#ECFDF5` | Background de badge aprovado (Emerald-50) |
| Warning | `#F59E0B` | Atenção, prazo próximo (Amber-500) |
| Warning Light | `#FFFBEB` | Background de badge warning (Amber-50) |
| Danger | `#EF4444` | Erro, atrasado, urgente (Red-500) |
| Danger Light | `#FEF2F2` | Background de badge erro (Red-50) |
| Info | `#3B82F6` | Informativo, em andamento (Blue-500) |
| Info Light | `#EFF6FF` | Background de badge info (Blue-50) |

### Cores do Kanban (Status)

| Status | Background | Text | Dot |
|--------|-----------|------|-----|
| Novo | `#EFF6FF` | `#1E40AF` | `#3B82F6` |
| Em Criação | `#FEF3C7` | `#92400E` | `#F59E0B` |
| Revisão Interna | `#F3E8FF` | `#6B21A8` | `#A855F7` |
| Aguardando Aprovação | `#FFF7ED` | `#9A3412` | `#F97316` |
| Aprovado | `#ECFDF5` | `#065F46` | `#10B981` |
| Publicado | `#F0FDF4` | `#166534` | `#22C55E` |

---

## Tipografia

### Fontes

| Uso | Font | Fallback |
|-----|------|----------|
| Body / UI | Inter | system-ui, -apple-system, sans-serif |
| Monospace (IDs, códigos) | JetBrains Mono | ui-monospace, monospace |

**Importação (Google Fonts):**
```
Inter: 400, 500, 600, 700
JetBrains Mono: 400, 500
```

### Escala Tipográfica

| Elemento | Size | Weight | Line Height | Tracking |
|----------|------|--------|-------------|----------|
| Display (hero) | 48px / 3rem | 700 | 1.1 | -0.02em |
| H1 | 36px / 2.25rem | 700 | 1.2 | -0.02em |
| H2 | 30px / 1.875rem | 600 | 1.3 | -0.01em |
| H3 | 24px / 1.5rem | 600 | 1.3 | -0.01em |
| H4 | 20px / 1.25rem | 600 | 1.4 | 0 |
| Body Large | 18px / 1.125rem | 400 | 1.6 | 0 |
| Body | 16px / 1rem | 400 | 1.6 | 0 |
| Body Small | 14px / 0.875rem | 400 | 1.5 | 0 |
| Caption | 12px / 0.75rem | 500 | 1.5 | 0.01em |
| Overline | 12px / 0.75rem | 600 | 1.5 | 0.05em |
| Mono (IDs) | 13px / 0.8125rem | 500 | 1.4 | 0 |

---

## Espaçamento

### Escala Base: 4px

| Token | Valor | Uso |
|-------|-------|-----|
| space-1 | 4px | Espaço mínimo entre ícone e texto |
| space-2 | 8px | Padding interno de badges, gap pequeno |
| space-3 | 12px | Padding de inputs, gap de lista |
| space-4 | 16px | Padding de cards, gap de grid |
| space-5 | 20px | Margem entre elementos relacionados |
| space-6 | 24px | Padding de containers, margem de seção |
| space-8 | 32px | Gap entre seções dentro de uma página |
| space-10 | 40px | Margem entre blocos de conteúdo |
| space-12 | 48px | Padding de seções da landing page |
| space-16 | 64px | Espaço entre seções maiores |
| space-20 | 80px | Espaço entre seções da landing page |
| space-24 | 96px | Espaço entre seções grandes da landing page |

### Layout

| Propriedade | Valor |
|-------------|-------|
| Max width (conteúdo) | 1280px |
| Max width (texto) | 720px |
| Sidebar width | 240px |
| Sidebar collapsed | 64px |
| Container padding (desktop) | 32px |
| Container padding (mobile) | 16px |

---

## Border Radius

| Token | Valor | Uso |
|-------|-------|-----|
| radius-xs | 4px | Badges, tags |
| radius-sm | 6px | Inputs, buttons pequenos |
| radius-md | 8px | Cards, dropdowns, modais |
| radius-lg | 12px | Cards destacados, screenshots |
| radius-xl | 16px | Hero images, containers grandes |
| radius-full | 9999px | Avatares, dots de status |

**Padrão:** `radius-md` (8px) para a maioria dos elementos.

---

## Sombras

| Token | Valor | Uso |
|-------|-------|-----|
| shadow-xs | `0 1px 2px rgba(0,0,0,0.05)` | Cards em repouso, inputs |
| shadow-sm | `0 1px 3px rgba(0,0,0,0.1), 0 1px 2px rgba(0,0,0,0.06)` | Cards hover, dropdowns |
| shadow-md | `0 4px 6px rgba(0,0,0,0.07), 0 2px 4px rgba(0,0,0,0.06)` | Modais, popovers |
| shadow-lg | `0 10px 15px rgba(0,0,0,0.1), 0 4px 6px rgba(0,0,0,0.05)` | Screenshots na landing page |
| shadow-xl | `0 20px 25px rgba(0,0,0,0.1), 0 10px 10px rgba(0,0,0,0.04)` | Hero screenshot |

**Padrão:** `shadow-xs` para cards normais. `shadow-sm` no hover. Sombras são sutis — nunca pesadas.

---

## Componentes — Diretrizes PrimeNG + Tailwind

### Botões

| Tipo | Estilo |
|------|--------|
| Primário | Background `primary`, texto white, `radius-sm`, font-weight 500 |
| Secundário | Background white, borda `gray-200`, texto `gray-700`, hover `gray-50` |
| Ghost | Sem background, sem borda, texto `gray-500`, hover `gray-100` |
| Danger | Background `danger`, texto white |
| Tamanho padrão | Height 40px, padding horizontal 16px |
| Tamanho small | Height 32px, padding horizontal 12px, font 14px |

**PrimeNG mapping:** `p-button` com severity customizado via Tailwind.

### Inputs

| Propriedade | Valor |
|-------------|-------|
| Height | 40px |
| Border | 1px solid `gray-300` |
| Border focus | 2px solid `primary` |
| Border radius | `radius-sm` (6px) |
| Background | `white` |
| Placeholder | `gray-400` |
| Label | `gray-700`, font-weight 500, 14px, acima do input |
| Error | Border `danger`, mensagem abaixo em `danger` 12px |

**PrimeNG mapping:** `p-inputtext`, `p-select`, `p-textarea` com classes Tailwind.

### Cards

| Propriedade | Valor |
|-------------|-------|
| Background | `white` |
| Border | 1px solid `gray-200` |
| Border radius | `radius-md` (8px) |
| Padding | 16-24px |
| Shadow | `shadow-xs` (repouso), `shadow-sm` (hover) |
| Transition | `transition-shadow duration-200` |

### Kanban Cards (Jobs)

| Propriedade | Valor |
|-------------|-------|
| Background | `white` |
| Border | 1px solid `gray-200` |
| Border-left | 3px solid (cor do status) |
| Border radius | `radius-md` |
| Padding | 12px 16px |
| Hover | `shadow-sm`, cursor pointer |
| Conteúdo | Título (font-weight 600), cliente (gray-500 14px), prazo, avatar do criativo, badge de prioridade |

### Badges / Tags

| Propriedade | Valor |
|-------------|-------|
| Padding | 2px 8px |
| Border radius | `radius-xs` (4px) |
| Font | 12px, font-weight 500 |
| Background + Text | Seguir cores semânticas (success-light + success, etc.) |

### Sidebar

| Propriedade | Valor |
|-------------|-------|
| Width | 240px |
| Background | `white` |
| Border-right | 1px solid `gray-200` |
| Item padding | 8px 12px |
| Item hover | Background `gray-100` |
| Item active | Background `primary-light`, texto `primary` |
| Ícones | 20px, `gray-400` (inativo), `primary` (ativo) |

### Modais

| Propriedade | Valor |
|-------------|-------|
| Overlay | `rgba(0,0,0,0.4)` |
| Background | `white` |
| Border radius | `radius-lg` (12px) |
| Shadow | `shadow-md` |
| Max width | 560px (padrão), 800px (wide) |
| Padding | 24px |

---

## Ícones

**Biblioteca:** Lucide Icons (via `lucide-angular` ou SVG inline)

**Tamanhos:**
- 16px: inline com texto small
- 20px: inline com texto body, sidebar, botões
- 24px: destaque em cards, headers

**Cor padrão:** `gray-400` (inativo), `gray-700` (ativo), `primary` (selecionado)

**Stroke width:** 1.5px (padrão Lucide)

---

## Breakpoints (Tailwind)

| Breakpoint | Largura | Uso |
|------------|---------|-----|
| sm | 640px | Mobile landscape |
| md | 768px | Tablet portrait |
| lg | 1024px | Tablet landscape / Desktop pequeno |
| xl | 1280px | Desktop |
| 2xl | 1536px | Desktop grande |

**Abordagem:** Desktop-first para o app, mobile-first para a landing page.

---

## Animações e Transições

| Tipo | Duração | Easing |
|------|---------|--------|
| Hover (cor, sombra) | 150ms | ease-in-out |
| Expand/Collapse | 200ms | ease-out |
| Modal enter | 200ms | ease-out |
| Modal exit | 150ms | ease-in |
| Page transition | 200ms | ease-in-out |
| Drag (kanban) | real-time | — |

**Regra:** Animações sutis, nunca chamar atenção. Se não melhora a UX, não anima.

---

## Referências Visuais

| Produto | URL | O que estudar |
|---------|-----|---------------|
| Operand | operand.com.br | Layout geral, design de dashboard, cards |
| Linear | linear.app | Sidebar, kanban, transições, tipografia |
| Resend | resend.com | Clean design, espaçamento generoso, landing page |
| Vercel | vercel.com | Gradientes sutis, hero section |
| Clerk | clerk.com | Light mode, forms, onboarding |

---

## Do's and Don'ts

**DO:**
- Usar espaçamento generoso — deixar o conteúdo respirar
- Manter hierarquia visual clara — títulos > corpo > secundário
- Usar cores semânticas consistentemente (verde = sucesso, vermelho = erro)
- Cards com bordas sutis e sombras leves
- Feedback visual em todas as interações (hover, focus, loading)

**DON'T:**
- Usar mais de 2 cores primárias na mesma tela
- Sombras pesadas ou bordas grossas
- Texto em cinza claro demais (acessibilidade)
- Animações lentas ou chamativas
- Ícones coloridos demais — manter monocromático
- Encher a tela de informação — priorizar e esconder o secundário
