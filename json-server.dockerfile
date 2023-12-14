FROM node:20-alpine3.18

RUN npm install -g json-server

EXPOSE 3000

WORKDIR /app

CMD ["json-server", "-H", "0.0.0.0", "db.json"]