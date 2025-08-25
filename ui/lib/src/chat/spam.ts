import * as xhr from '../xhr';
import { storage } from '../storage';

export const skip = (txt: string): boolean => (suspLink(txt) || followMe(txt)) && !isKnownSpammer();

export const selfReport = (txt: string): void => {
  if (isKnownSpammer()) return;
  const hasSuspLink = suspLink(txt);
  if (hasSuspLink) xhr.text(`/jslog/${window.location.href.slice(-12)}?n=spam`, { method: 'post' });
  if (hasSuspLink || followMe(txt)) storage.set('chat-spam', '1');
};

const isKnownSpammer = () => storage.get('chat-spam') === '1';

const spamRegex = new RegExp(
  [
    'xcamweb.com',
    '(^|[^i])chess-bot',
    'chess-cheat',
    'coolteenbitch',
    'letcafa.webcam',
    'tinyurl.com/',
    'wooga.info/',
    'bit.ly/',
    'wbt.link/',
    'eb.by/',
    '001.rs/',
    'shr.name/',
    'u.to/',
    '.3-a.net',
    '.ssl443.org',
    '.ns02.us',
    '.myftp.info',
    '.flinkup.com',
    '.serveusers.com',
    'badoogirls.com',
    'hide.su',
    'wyon.de',
    'sexdatingcz.club',
    'qps.ru',
    'tiny.cc/',
    'trasderk.blogspot.com',
    't.ly/',
    'shorturl.at/',
    'lichess77',
    '77Casino.cfd',
    'Betspin.life',
  ]
    .map(url => url.replace(/\./g, '\\.').replace(/\//g, '\\/'))
    .join('|'),
);

const suspLink = (txt: string) => !!txt.match(spamRegex);

const followMeRegex = /follow me|join my team/i;
const followMe = (txt: string) => !!txt.match(followMeRegex);

const teamUrlRegex = /lichess\.org\/team\//i;
export const hasTeamUrl = (txt: string): boolean => !!txt.match(teamUrlRegex);
