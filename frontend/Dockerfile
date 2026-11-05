# ---------- Stage 1: build ----------
# Node compiles the TypeScript/React app into plain static files (dist/).
FROM node:22-alpine AS build
WORKDIR /app

# Install dependencies first so Docker can cache this layer while only source files change.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# VITE_API_URL is left empty on purpose: the browser calls /api on the same address,
# and nginx (below) forwards those calls to the backend inside the cluster.
RUN npm run build

# ---------- Stage 2: serve ----------
# A tiny nginx image that runs as a non-root user and listens on 8080.
FROM nginxinc/nginx-unprivileged:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080
