FROM node:22-alpine AS build

ARG NG_APP_API_URL=http://localhost:8080/api
ENV NG_APP_API_URL=${NG_APP_API_URL}

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/ticketflow-frontend/browser /usr/share/nginx/html

EXPOSE 80
