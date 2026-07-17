FROM node:20-slim

WORKDIR /app

ENV NODE_ENV=production
ENV PORT=9082
ENV RELEASE_DIR=/app/release

COPY server.js .

EXPOSE 9082

CMD ["node", "server.js"]
