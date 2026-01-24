FROM nginx:alpine

WORKDIR /usr/share/nginx/html

COPY assets/ .
