import { expandMentions, linkRegex, linkReplace, newLineRegex } from 'common/rich-text';
import { escapeHtml } from 'common/string';
import { i18n } from 'i18n';

const imgurRegex = /https?:\/\/(?:i\.)?imgur\.com\/(?!gallery\b)(\w{7})(?:\.jpe?g|\.png|\.gif)?/;
const giphyRegex =
  /https:\/\/(?:media\.giphy\.com\/media\/|giphy\.com\/gifs\/(?:\w+-)*)(\w+)(?:\/giphy\.gif)?/;
const teamMessageRegex =
  /You received this because you are subscribed to messages of the team <a(?:[^>]+)>(?:[^\/]+)(.+)<\/a>\.$/;

export const img = (src: string): string => `<img src="${src}"/>`;

export const aImg = (src: string): string => linkReplace(src, img(src));

export const expandImgur = (url: string): string | undefined =>
  imgurRegex.test(url)
    ? url.replace(imgurRegex, (_, id) => aImg(`https://i.imgur.com/${id}.jpg`))
    : undefined;

export const expandGiphy = (url: string): string | undefined =>
  giphyRegex.test(url)
    ? url.replace(giphyRegex, (_, id) => aImg(`https://media.giphy.com/media/${id}/giphy.gif`))
    : undefined;

export const expandImage = (url: string): string | undefined =>
  /\.(jpg|jpeg|png|gif)$/.test(url) ? aImg(url) : undefined;

const expandLink = (url: string) => linkReplace(url, url.replace(/^https?:\/\//, ''));

const expandUrl = (url: string) =>
  expandImgur(url) || expandGiphy(url) || expandImage(url) || expandLink(url);

const expandUrls = (html: string) =>
  html.replace(linkRegex, (_, space: string, url: string) => `${space}${expandUrl(url)}`);

const expandTeamMessage = (html: string) =>
  html.replace(
    teamMessageRegex,
    (_: string, url: string) =>
      `${expandLink(
        url,
      )} <form action="${url}/subscribe" class="unsub" method="post"><button type="submit" class="button button-empty button-thin button-red">${i18n('unsubscribe')} from these messages</button></form>`,
  );

export const msgEnhance = (str: string): string =>
  expandTeamMessage(expandMentions(expandUrls(escapeHtml(str)))).replace(newLineRegex, '<br>');
