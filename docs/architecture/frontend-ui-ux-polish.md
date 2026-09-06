# DevSphere Frontend UI/UX Architecture & Responsive Guidelines

This document outlines the architectural conventions, design system tokens, responsive layout strategies, state patterns, and accessibility standards applied across the DevSphere frontend application.

---

## 1. Overview & Philosophy

DevSphere's user interface is designed as a dark-themed, modern SaaS control plane for microservices orchestration. The frontend emphasizes clarity, dynamic feedback, fast response times, accessible control mechanics, and visual consistency across all viewports (Desktop, Tablet, and Mobile).

---

## 2. Responsive Breakpoint Strategy

DevSphere enforces standard Tailwind CSS breakpoints across all application modules:

| Viewport Category | Width Range | Layout Strategy |
| :--- | :--- | :--- |
| **Mobile** | `< 640px` (e.g. 375px, 390px, 430px) | Single-column vertical stacking, compact header, collapsible navigation drawer, responsive notification modal (`fixed` viewport bounds), mobile task cards. |
| **Tablet** | `640px – 1023px` (e.g. 768px, 820px) | 2-column metric grid, horizontal tab bars with touch scrolling (`shrink-0`), adaptive form layouts. |
| **Desktop** | `≥ 1024px` (e.g. 1280px, 1440px) | Fixed-width sidebar, multi-column dashboard grid (2:1 col split), comprehensive data tables, anchor-aligned dropdowns and popovers. |

### Global Layout Safety Rules
- Root container enforces `overflow-x-hidden` to eliminate horizontal scrollbars on mobile.
- Page content is contained within `max-w-7xl mx-auto` with consistent gutter padding (`p-4 sm:p-6 lg:p-8`).

---

## 3. Spacing & Visual Hierarchy Tokens

- **Card Radius**: `rounded-2xl` (`1rem`)
- **Card Padding**: `p-4 sm:p-6`
- **Grid Gaps**: `gap-4 sm:gap-6 lg:gap-8`
- **Input Padding & Height**: `px-3.5 py-2.5 min-h-[42px] text-sm`
- **Button Sizing**:
  - `sm`: `px-3 py-2 text-xs min-h-[36px]`
  - `md`: `px-4 py-2.5 text-sm min-h-[40px]`
  - `lg`: `px-6 py-3 text-base min-h-[48px]`

---

## 4. Loading, Empty & Error State Patterns

1. **Skeleton Loading (`Skeleton.tsx`)**:
   - Matches the geometric shape and aspect ratio of content cards, tables, and statistics.
   - Restrained pulse animation (`animate-pulse bg-slate-800/80`).
2. **Empty States (`EmptyState.tsx`)**:
   - Rendered when data sets contain zero items.
   - Provides clear titles, descriptive explanations, and primary call-to-action buttons.
3. **Error Handling (`ErrorState.tsx`)**:
   - Non-intrusive error cards displaying user-friendly error messages with retry buttons.
   - Raw technical exceptions or stack traces are never exposed to the end user.

---

## 5. Animation & Motion Design

- **Modal & Popover Animations**: Fast `150ms` keyframe transitions (`animate-in fade-in zoom-in-95`).
- **Hover & Interaction Effects**: Micro-scaling (`active:scale-[0.98]`) and border/background highlights (`transition-all duration-150`).
- **Reduced Motion Support**: `@media (prefers-reduced-motion: reduce)` automatically sets animation and transition durations to `0.01ms` for user accessibility preferences.

---

## 6. Accessibility & Keyboard Governance

- Semantic HTML tags (`<header>`, `<main>`, `<aside>`, `<nav>`, `<form>`).
- Dialog accessibility attributes (`role="dialog"`, `aria-modal="true"`, `aria-labelledby`, `aria-describedby`, Escape listener).
- Form label associations (`htmlFor`, `id`, `aria-invalid`, `aria-describedby`).
- Outline focus rings (`focus-visible:ring-2 focus-visible:ring-brand-500/50`) for all interactive buttons, inputs, and dropdown triggers.
