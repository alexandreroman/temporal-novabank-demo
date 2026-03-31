FROM node:22-alpine AS build
WORKDIR /app
COPY --link package.json package-lock.json ./
RUN npm ci
COPY --link . .
RUN npm run build

FROM node:22-alpine AS runtime
WORKDIR /app
COPY --link --from=build /app/.output .output
EXPOSE 3001
ENV NITRO_PORT=3001 NITRO_HOST=0.0.0.0
CMD ["node", ".output/server/index.mjs"]
