FROM node:20.9-alpine3.18 AS builder
WORKDIR /app

COPY package*.json ./

# install project dependencies
RUN npm install

# copy project files and folders to the current working directory
COPY . .

# build app for production with minification
RUN npm run build


FROM node:20.9-alpine3.18
WORKDIR /app

# install simple http server for serving static content
RUN npm install -g http-server

COPY --from=builder /app/dist/ /app/

EXPOSE 8080
CMD ["http-server"]