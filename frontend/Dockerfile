# ---------- Compilación ----------
FROM node:22-alpine AS build
WORKDIR /app

COPY package*.json ./
# npm ci si hay package-lock.json (instalación reproducible); si no, npm install
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi

COPY . .
RUN npx ng build --configuration production

# ---------- Servidor estático ----------
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80
