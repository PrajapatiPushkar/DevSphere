# 69. Frontend Foundation & Modern Design System

* Status: Accepted
* Date: 2026-09-04

## Context and Problem Statement

Following the completion of cloud-native backend microservices, Kubernetes scaling, health probing, CI/CD pipelines, and production observability (Lessons 62–70), DevSphere requires a modern, responsive frontend application to provide a full-stack developer experience. The frontend must adhere to SaaS design standards, establish a scalable folder architecture, configure centralized API routing, and build a reusable component design system without over-engineering or deviating from existing backend APIs.

## Decision Drivers

* Need a fast, type-safe, modern web framework setup (React 18 + TypeScript + Vite).
* Requirement for a clean, consistent SaaS design system with reusable atomic UI components (Buttons, Inputs, Cards, Badges, Modals, Dropdowns, Tables, Toast notifications, Loading skeletons).
* Centralized API client foundation with base URL configuration via environment variables (`VITE_API_BASE_URL`).
* Foundation for authentication state (`AuthContext`) handling token persistence and user session state.
* Mobile-responsive layout system with collapsible navigation drawer.

## Considered Options

1. **React + TypeScript + Vite + Tailwind CSS (Selected)**
2. Next.js App Router Single Page Application
3. Vanilla HTML/CSS/JS with standalone scripts

## Decision Outcome

Option 1 was chosen. Vite provides fast HMR and lightweight bundling, TypeScript ensures type safety with backend API DTOs, and Tailwind CSS enables a modern dark-mode SaaS design system using CSS tokens.

### Key Architectural Choices

1. **Folder Architecture**:
   - Organized under `frontend/src/` into `components/` (`ui/`, `layout/`, `common/`), `pages/`, `routes/`, `services/`, `context/`, `hooks/`, `types/`, `utils/`, and `styles/`.

2. **Centralized API Client**:
   - Implemented `apiClient.ts` using Axios with request interceptors for JWT Bearer token attachment and uniform response error normalization.
   - Configurable via `VITE_API_BASE_URL` (defaulting to `http://localhost:8080`).

3. **Design System & Component Library**:
   - Tailored HSL color palette (`brand-500` indigo primary, slate-950 background, glassmorphism panel backdrops).
   - Reusable UI component primitives: `Button`, `Input`, `Textarea`, `Select`, `Checkbox`, `Card`, `Badge`, `Modal`, `Dropdown`, `Table`, `Spinner`, `Skeleton`, `Alert`, `Toast`, `EmptyState`, `ErrorState`.

4. **Routing & Initial Pages**:
   - `react-router-dom` configuration supporting `/` (Landing Page), `/login` (Sign In), `/register` (Sign Up), and `/dashboard` (SaaS Overview Console).

## Security & Environment Considerations

* Base API URLs and environment-specific settings are loaded strictly via `VITE_API_BASE_URL` in `.env`.
* Zero production credentials or hardcoded API keys exist in source code.
* JWT tokens are stored securely in `localStorage` and managed through `AuthContext`.

## Consequences

* Positive: Clean, production-ready frontend foundation capable of rapidly scaling across upcoming feature lessons (Tasks, Resumes, Observability).
* Positive: High visual polish and responsive user experience across desktop, tablet, and mobile devices.
* Negative: Additional frontend dependencies (`node_modules`) to manage during CI/CD build phases.
