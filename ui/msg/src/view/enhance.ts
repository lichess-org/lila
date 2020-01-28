// looks like it has a @mention or a url.tld
export const isMoreThanText = (str: string) => /(@|\.)\w{2,}/.test(str);

export const enhance = (str: string) =>
  expandMentions(
    expandUrls(window.lichess.escapeHtml(str))
  ).replace(/\n/g, '<br>');

const expandMentions = (html: string) =>
  html.replace(/(^|[^\w@#/])@([\w-]{2,})/g, (orig: string, prefix: string, user: string) =>
    user.length > 20 ? orig : `${prefix}${a('/@/' + user, '@' + user)}`
  );

// from https://github.com/bryanwoods/autolink-js/blob/master/autolink.js
const urlRegex = /(^|[\s\n]|<[A-Za-z]*\/?>)((?:https?|ftp):\/\/[\-A-Z0-9+\u0026\u2019@#\/%?=()~_|!:,.;]*[\-A-Z0-9+\u0026@#\/%=~()_|])/gi;
const expandUrls = (html: string) =>
  html.replace(urlRegex, (_, space: string, url: string) => `${space}${expandUrl(url)}`);

const expandUrl = (url: string) =>
  expandImgur(url) || expandGiphy(url) || expandImage(url) || expandLink(url);

const imgurRegex = /https?:\/\/(?:i\.)?imgur\.com\/(\w+)(?:\.jpe?g|\.png|\.gif)?/;
const expandImgur = (url: string) =>
  imgurRegex.test(url) ? url.replace(imgurRegex, (_, id) => img(`https://i.imgur.com/${id}.jpg`)) : undefined;

const giphyRegex = /https:\/\/(?:media\.giphy\.com\/media\/|giphy\.com\/gifs\/(?:\w+-)*)(\w+)(?:\/giphy\.gif)?/;
const expandGiphy = (url: string) =>
  giphyRegex.test(url) ? url.replace(giphyRegex, (_, id) => img(`https://media.giphy.com/media/${id}/giphy.gif`)) : undefined;

const expandImage = (url: string) => /\.(jpg|jpeg|png|gif)$/.test(url) ? a(url, img(url)) : undefined;

const expandLink = (url: string) => a(url, url.replace(/^https?:\/\//, ''));

const a = (href: string, body: string) => `<a target="_blank" href="${href}">${body}</a>`;

const img = (src: string) => `<img src="${src}" class="embed"/>`;
