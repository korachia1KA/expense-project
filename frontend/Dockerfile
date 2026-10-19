# =====================================================================================
# Frontend image: compiles the Angular app, then serves it with nginx.
# Built by docker-compose.yml, or by hand:  docker build -t expense-tracker-web ./frontend
# =====================================================================================

# ---------- Stage 1: compile the Angular app to static files ----------
FROM node:18-alpine AS build
WORKDIR /app
# Install dependencies first (cached layer; only re-runs when package*.json change).
# `npm ci` installs EXACTLY what package-lock.json says: reproducible builds.
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# Production build: minified, tree-shaken, hashed file names. Output: dist/frontend/browser
RUN npx ng build --configuration production

# ---------- Stage 2: serve the static files with nginx ----------
# Node is not needed at runtime: the result is just HTML/JS/CSS, so a tiny nginx image is enough.
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
