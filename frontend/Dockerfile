# Etapa de construccion
FROM node:20-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci --legacy-peer-deps

COPY . .

# Construir la aplicacion
RUN npm run build -- --configuration production

# Verificar la estructura de dist
RUN echo "=== Contenido de dist ===" && ls -la /app/dist/
RUN echo "=== Contenido de dist/frontend ===" && ls -la /app/dist/frontend/ || echo "No existe dist/frontend"
RUN echo "=== Contenido de dist/frontend/browser ===" && ls -la /app/dist/frontend/browser/ || echo "No existe dist/frontend/browser"

# Etapa de ejecucion
FROM nginx:alpine

# Copiar la configuracion de nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copiar los archivos construidos (Angular 17+ usa /dist/frontend/browser)
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
