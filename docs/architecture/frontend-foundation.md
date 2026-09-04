# DevSphere Frontend Foundation & Design System Architecture

This document specifies the technical design, folder architecture, design system tokens, component library contracts, API client structure, and environment configuration for the DevSphere Frontend application.

---

## 1. Stack Overview

- **Framework**: React 18 with TypeScript 5
- **Build Tool**: Vite 5
- **Styling**: Tailwind CSS 3 with custom HSL design tokens & glassmorphism utilities
- **Icons**: Lucide React
- **Routing**: React Router DOM v6
- **HTTP Client**: Axios with request/response interceptors

---

## 2. Directory Architecture

```text
frontend/
├── src/
│   ├── app/              # Application bootstrap & top-level React entry (App.tsx, main.tsx)
│   ├── assets/           # Logos, SVGs, static assets
│   ├── components/       # Reusable component library
│   │   ├── ui/           # Atom & molecule primitives (Button, Input, Card, Modal, Table, etc.)
│   │   ├── layout/       # Layout components (Header, Sidebar, AppLayout, PageHeader)
│   │   └── common/       # Feedback widgets (Toast, Spinner, Skeleton, EmptyState, ErrorState)
│   ├── context/          # Global React context (AuthContext.tsx, ToastContext.tsx)
│   ├── hooks/            # Custom hooks (useAuth, useToast)
│   ├── pages/            # Page components (LandingPage, LoginPage, RegisterPage, DashboardPage)
│   ├── routes/           # Router configuration (index.tsx)
│   ├── services/         # API HTTP Client (apiClient.ts, authService.ts)
│   ├── styles/           # Global CSS directives & Tailwind imports (index.css)
│   ├── types/            # TypeScript domain interfaces (index.ts)
│   └── utils/            # Utilities (cn.ts)
├── .env.example          # Environment variable template
├── package.json          # Dependencies & build scripts
├── tailwind.config.js    # Design system tokens configuration
├── tsconfig.json         # TypeScript compiler configuration
└── vite.config.ts        # Vite build tool configuration
```

---

## 3. Design System & Tokens

### Color Palette
- **Brand Primary**: Indigo scale (`bg-brand-600`, `text-brand-400`, `border-brand-500/30`)
- **Background & Surfaces**: Slate scale (`slate-950` background, `slate-900` cards, `slate-800` borders)
- **Status Accents**:
  - `success`: Emerald (`bg-emerald-500/10`, `text-emerald-400`)
  - `warning`: Amber (`bg-amber-500/10`, `text-amber-400`)
  - `danger`: Rose (`bg-rose-500/10`, `text-rose-400`)
  - `info`: Sky (`bg-sky-500/10`, `text-sky-400`)

### Spacing & Layout
- Grid spacing based on 8px base units (`4`, `8`, `12`, `16`, `24`, `32`, `48`, `64`).
- Rounded radius: `rounded-xl` (buttons/inputs), `rounded-2xl` (cards/tables/modals).

---

## 4. Reusable UI Component Library

| Component | Category | Key Props & Features |
|---|---|---|
| `Button` | Primitive | Variants (`primary`, `secondary`, `outline`, `ghost`, `danger`), sizes (`sm`, `md`, `lg`), `isLoading` spinner state |
| `Input` | Form | `label`, `error`, `helperText`, `leftIcon`, `rightIcon`, focus rings |
| `Textarea` | Form | Multi-line text input with label and error state |
| `Select` | Form | Option array, label, error state |
| `Checkbox` | Form | Checkbox input with label and optional description |
| `Card` | Surface | Subcomponents: `CardHeader`, `CardTitle`, `CardDescription`, `CardContent`, `CardFooter` |
| `Badge` | Feedback | Variants (`success`, `warning`, `danger`, `info`, `neutral`, `brand`), optional pulsing `dot` |
| `Modal` | Overlay | Backdrop blur, ESC key dismiss, `maxWidth` selection, smooth animation |
| `Dropdown` | Overlay | Action menu items, icon support, danger styling, backdrop dismiss |
| `Table` | Data | Generic TypeScript table with header columns, empty message, and loading skeletons |
| `Spinner` | Feedback | Animated loader in multiple sizes (`sm`, `md`, `lg`, `xl`) |
| `Skeleton` | Feedback | Pulse loader with pre-built `CardSkeleton` and `TableSkeleton` helpers |
| `Alert` | Feedback | Warning/Error/Success/Info notification banners |
| `Toast` | Feedback | Transient notification container with auto-dismiss and stack management |
| `EmptyState` | State | Clean empty state with icon, title, description, and CTA button |
| `ErrorState` | State | User-friendly error message container with retry callback |

---

## 5. API Client & Authentication State

- **Centralized Client**: `src/services/apiClient.ts` initializes Axios with `baseURL` read from `import.meta.env.VITE_API_BASE_URL`.
- **Request Interceptor**: Automatically injects `Authorization: Bearer <token>` from `localStorage`.
- **Response Interceptor**: Catches HTTP 401 unauthenticated errors and clears invalid tokens automatically.
- **Auth Context**: `src/context/AuthContext.tsx` exposes `user`, `token`, `isAuthenticated`, `isLoading`, `login()`, and `logout()`.

---

## 6. Development & Build Scripts

```bash
# Install dependencies
npm install

# Start local dev server (port 3000)
npm run dev

# Run TypeScript type checks
npm run lint

# Build production bundle to dist/
npm run build
```
