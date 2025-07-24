import { scroller } from './scroller';
import * as licon from 'lib/licon';
import { linkRegex, linkReplace, newLineRegex, expandMentions } from 'lib/richText';
import { escapeHtml } from 'lib';
export { isMoreThanText } from 'lib/richText';

export const imgurRegex = /https?:\/\/(?:i\.)?imgur\.com\/(?!gallery\b)(\w{7})(?:\.jpe?g|\.png|\.gif)?/;
const giphyRegex =
  /https:\/\/(?:media\.giphy\.com\/media\/|giphy\.com\/gifs\/(?:\w+-)*)(\w+)(?:\/giphy\.gif)?/;
const teamMessageRegex =
  /You received this because you are subscribed to messages of the team <a(?:[^>]+)>(?:[^\/]+)(.+)<\/a>\.$/;

const img = (src: string) => `<img src="${src}"/>`;

const aImg = (src: string) => linkReplace(src, img(src));

const expandImgur = (url: string) =>
  imgurRegex.test(url)
    ? url.replace(imgurRegex, (_, id) => aImg(`https://i.imgur.com/${id}.jpg`))
    : undefined;

const expandGiphy = (url: string) =>
  giphyRegex.test(url)
    ? url.replace(giphyRegex, (_, id) => aImg(`https://media.giphy.com/media/${id}/giphy.gif`))
    : undefined;

const expandImage = (url: string) => (/\.(jpg|jpeg|png|gif)$/.test(url) ? aImg(url) : undefined);

const expandLink = (url: string) => linkReplace(url, url.replace(/^https?:\/\//, ''));

const expandUrl = (url: string) =>
  expandImgur(url) || expandGiphy(url) || expandImage(url) || expandLink(url);

const expandUrls = (html: string) =>
  html.replace(linkRegex, (_, space: string, url: string) => `${space}${expandUrl(url)}`);

const expandGameIds = (html: string) =>
  html.replace(
    /(\s#)([\w]{8})($|[^\w-])/g,
    (_: string, bulkStart: string, id: string, suffix: string) =>
      ' ' + linkReplace('/' + id, '#' + id, !bulkStart) + suffix,
  );

const expandTeamMessage = (html: string) =>
  html.replace(
    teamMessageRegex,
    (_: string, url: string) =>
      `${expandLink(
        url,
      )} <form action="${url}/subscribe" class="unsub" method="post"><button type="submit" class="button button-empty button-thin button-red">Unsubscribe from these messages</button></form>`,
  );

export const enhance = (str: string) =>
  expandTeamMessage(expandGameIds(expandMentions(expandUrls(escapeHtml(str))))).replace(newLineRegex, '<br>');

interface Expandable {
  element: HTMLElement;
  link: Link;
}
interface Link {
  type: LinkType;
  src: string;
  opts: any;
}
type LinkType = 'game';

const domain = window.location.host;
const gameRegex = new RegExp(
  `(?:https?://)${domain}/(?:embed/)?(?:game/)?(\\w{8})(?:(?:/(white|black))|\\w{4}|)(?:(?:#)(\\d+))?$`,
);
const notGames = [
  'training',
  'analysis',
  'insights',
  'practice',
  'features',
  'password',
  'streamer',
  'timeline',
];

export function expandLpvs(el: HTMLElement) {
  const expandables: Expandable[] = [];

  el.querySelectorAll('a:not(.text)').forEach((a: HTMLAnchorElement) => {
    const link = parseLink(a);
    if (link)
      expandables.push({
        element: a,
        link: link,
      });
  });

  expandGames(expandables);
}

function expandGames(games: Expandable[]): void {
  if (games.length < 3) games.forEach(expandGame);
  else
    games.forEach(game => {
      game.element.title = 'Click to expand';
      game.element.classList.add('text');
      game.element.setAttribute('data-icon', licon.Expand);
      game.element.addEventListener('click', e => {
        if (e.button === 0) {
          e.preventDefault();
          expandGame(game);
        }
      });
    });
}

const expandGame = async (exp: Expandable) => {
  const $lpv = $('<div>');
  $(exp.element).parent().parent().addClass('has-embed');
  $(exp.element).replaceWith($('<div>').prepend($lpv));
  await site.asset.loadEsm('bits.lpv', {
    init: { el: $lpv[0] as HTMLElement, url: exp.link.src, lpvOpts: exp.link.opts },
  });
  scroller.auto();
};

function parseLink(a: HTMLAnchorElement): Link | undefined {
  const [id, orientation, initialPly] = Array.from(a.href.match(gameRegex) || []).slice(1);
  if (id && !notGames.includes(id))
    return {
      type: 'game',
      src: `/embed/game/${id}`,
      opts: {
        orientation,
        initialPly,
      },
    };
  return undefined;
}
