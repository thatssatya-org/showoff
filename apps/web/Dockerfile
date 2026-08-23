FROM node:22.16.0-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

ARG PUBLIC_SITE_URL
ARG PUBLIC_ENABLE_LIVE_CAPABILITIES=false
ARG PUBLIC_ENABLE_OPERATOR_GITHUB_SETUP=false
ENV PUBLIC_SITE_URL=${PUBLIC_SITE_URL}
ENV PUBLIC_ENABLE_LIVE_CAPABILITIES=${PUBLIC_ENABLE_LIVE_CAPABILITIES}
ENV PUBLIC_ENABLE_OPERATOR_GITHUB_SETUP=${PUBLIC_ENABLE_OPERATOR_GITHUB_SETUP}
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080
