# Stage 1: Build static assets
FROM node:20-alpine AS builder
WORKDIR /app

# Build-time API base URL configuration for production
ARG VITE_API_BASE_URL=http://api.devsphere.local
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

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

# Support non-root execution for Kubernetes Pod Security Standards (restricted)
RUN sed -i 's|^user  nginx;|#user nginx;|g' /etc/nginx/nginx.conf \
    && sed -i 's|/var/run/nginx.pid|/tmp/nginx.pid|g' /etc/nginx/nginx.conf \
    && chown -R 101:101 /usr/share/nginx/html /var/cache/nginx /var/log/nginx /etc/nginx/conf.d \
    && chmod -R 777 /var/cache/nginx /var/log/nginx /etc/nginx/conf.d \
    && touch /tmp/nginx.pid \
    && chmod 777 /tmp/nginx.pid

# Expose HTTP port
EXPOSE 80

USER 101

CMD ["nginx", "-g", "daemon off;"]

