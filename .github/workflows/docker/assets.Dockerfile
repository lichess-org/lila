FROM nginx:alpine

WORKDIR /usr/share/nginx/html

COPY assets/ /usr/share/nginx/html
COPY public/ /usr/share/nginx/html/public
