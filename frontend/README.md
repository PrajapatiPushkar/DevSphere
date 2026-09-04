# DevSphere Frontend

Production-ready modern SaaS application foundation for DevSphere microservices platform built with React, TypeScript, Vite, and Tailwind CSS.

## Getting Started

### Prerequisites
- Node.js >= 18.x
- npm >= 9.x

### Installation
```bash
cd frontend
npm install
```

### Environment Configuration
Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```
Default configuration:
```text
VITE_API_BASE_URL=http://localhost:8080
```

### Development Server
```bash
npm run dev
```

### Production Build
```bash
npm run build
```

## Architecture Overview

```text
frontend/src/
├── app/          # Main App wrapper & entry
├── components/   # Reusable UI component library
│   ├── ui/       # Atom & molecule UI components (Button, Input, Card, Modal, Table, etc.)
│   ├── layout/   # Layout elements (Header, Sidebar, Container, PageHeader)
│   └── common/   # Common feedback widgets (Toast, Spinner, Skeleton, EmptyState, ErrorState)
├── context/      # AuthContext & ToastContext providers
├── hooks/        # Custom hooks (useAuth, useToast)
├── pages/        # Landing, Login, Register, Dashboard pages
├── routes/       # React Router configuration
├── services/     # Centralized API client (apiClient.ts, authService.ts)
├── styles/       # Global CSS & Tailwind imports (index.css)
├── types/        # TypeScript interfaces & API models
└── utils/        # Class merge utility (cn.ts)
```
