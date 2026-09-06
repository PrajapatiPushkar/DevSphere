# Stage 1: Build static assets
FROM node:20-alpine AS builder
WORKDIR /app

# Copy dependency specifications
COPY package*.json ./

# Install dependencies cleanly
RUN npm ci

# Copy application source code
COPY . .

# Run type-check and production build
RUN npm run build

# Stage 2: Production Web Server
FROM nginx:1.25-alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx static assets
RUN rm -rf ./*

# Copy built production static files from builder stage
COPY --from=builder /app/dist .

# Copy NGINX configuration with SPA fallback support
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose HTTP port
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
