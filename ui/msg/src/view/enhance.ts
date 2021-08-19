import { scroller } from './scroller';
import { linkRegex, linkReplace, newLineRegex, userPattern, userLinkReplace } from 'common/richText';

export { isMoreThanText } from 'common/richText';

const imgurRegex = /https?:\/\/(?:i\.)?imgur\.com\/(\w+)(?:\.jpe?g|\.png|\.gif)?/;
const giphyRegex = /https:\/\/(?:media\.giphy\.com\/media\/|giphy\.com\/gifs\/(?:\w+-)*)(\w+)(?:\/giphy\.gif)?/;
const teamMessageRegex =
  /You received this because you are subscribed to messages of the team <a(?:[^>]+)>(?:[^\/]+)(.+)<\/a>\.$/;

const img = (src: string) => `<img src="${src}"/>`;

const aImg = (src: string) => linkReplace(src, img(src));

const expandImgur = (url: string) =>
  imgurRegex.test(url) ? url.replace(imgurRegex, (_, id) => aImg(`https://i.imgur.com/${id}.jpg`)) : undefined;

const expandGiphy = (url: string) =>
  giphyRegex.test(url)
    ? url.replace(giphyRegex, (_, id) => aImg(`https://media.giphy.com/media/${id}/giphy.gif`))
    : undefined;

const expandImage = (url: string) => (/\.(jpg|jpeg|png|gif)$/.test(url) ? aImg(url) : undefined);

const expandLink = (url: string) => linkReplace(url, url.replace(/^https?:\/\//, ''));

const expandUrl = (url: string) => expandImgur(url) || expandGiphy(url) || expandImage(url) || expandLink(url);

const expandUrls = (html: string) =>
  html.replace(linkRegex, (_, space: string, url: string) => `${space}${expandUrl(url)}`);

const expandMentions = (html: string) => html.replace(userPattern, userLinkReplace);

const expandGameIds = (html: string) =>
  html.replace(
    /\s#([\w]{8})($|[^\w-])/g,
    (_: string, id: string, suffix: string) => ' ' + linkReplace('/' + id, '#' + id, 'text') + suffix
  );

const expandTeamMessage = (html: string) =>
  html.replace(
    teamMessageRegex,
    (_: string, url: string) =>
      `${expandLink(
        url
      )} <form action="${url}/subscribe" class="unsub" method="post"><button type="submit" class="button button-empty button-thin button-red">Unsubscribe from these messages</button></form>`
  );

export const enhance = (str: string) =>
  expandTeamMessage(expandGameIds(expandMentions(expandUrls(lichess.escapeHtml(str))))).replace(newLineRegex, '<br>');

/* Enhance with iframe expansion */

interface Expandable {
  element: HTMLElement;
  link: Link;
}
interface Link {
  type: LinkType;
  src: string;
}
type LinkType = 'game';

const domain = window.location.host;
const gameRegex = new RegExp(`(?:https?://)${domain}/(?:embed/)?(\\w{8})(?:(?:/(white|black))|\\w{4}|)(#\\d+)?$`);
const notGames = ['training', 'analysis', 'insights', 'practice', 'features', 'password', 'streamer', 'timeline'];

export function expandIFrames(el: HTMLElement) {
  const expandables: Expandable[] = [];

  el.querySelectorAll('a:not(.text)').forEach((a: HTMLAnchorElement) => {
    const link = parseLink(a);
    if (link)
      expandables.push({
        element: a,
        link: link,
      });
  });

  expandGames(expandables.filter(e => e.link.type == 'game'));
}

function expandGames(games: Expandable[]): void {
  if (games.length < 3) games.forEach(expand);
  else
    games.forEach(game => {
      game.element.title = 'Click to expand';
      game.element.classList.add('text');
      game.element.setAttribute('data-icon', '');
      game.element.addEventListener('click', e => {
        if (e.button === 0) {
          e.preventDefault();
          expand(game);
        }
      });
    });
}

function expand(exp: Expandable): void {
  const $iframe: any = $('<iframe>').attr('src', exp.link.src);
  $(exp.element).parent().parent().addClass('has-embed');
  $(exp.element).replaceWith($('<div class="embed">').prepend($iframe));
  return $iframe
    .on('load', function (this: HTMLIFrameElement) {
      if (this.contentDocument?.title.startsWith('404')) (this.parentNode as HTMLElement).classList.add('not-found');
      scroller.auto();
    })
    .on('mouseenter', function (this: HTMLIFrameElement) {
      this.focus();
    });
}

function parseLink(a: HTMLAnchorElement): Link | undefined {
  const [id, pov, ply] = Array.from(a.href.match(gameRegex) || []).slice(1);
  if (id && !notGames.includes(id))
    return {
      type: 'game',
      src: configureSrc(`/embed/${id}${pov ? `/${pov}` : ''}${ply || ''}`),
    };
  return undefined;
}

const themes = [
  'blue',
  'blue2',
  'blue3',
  'blue-marble',
  'canvas',
  'wood',
  'wood2',
  'wood3',
  'wood4',
  'maple',
  'maple2',
  'brown',
  'leather',
  'green',
  'marble',
  'green-plastic',
  'grey',
  'metal',
  'olive',
  'newspaper',
  'purple',
  'purple-diag',
  'pink',
  'ic',
  'horsey',
];

function configureSrc(url: string): string {
  if (url.includes('://')) return url;
  const parsed = new URL(url, window.location.href);
  parsed.searchParams.append('theme', themes.find(theme => document.body.classList.contains(theme))!);
  parsed.searchParams.append('bg', document.body.getAttribute('data-theme')!);
  return parsed.href;
}
