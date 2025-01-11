import { loadScript } from 'common/assets';
import { currentTheme } from 'common/theme';

type LinkType = 'youtube' | 'twitter' | 'game' | 'study';

interface Parsed {
  type: LinkType;
  src: string;
}
interface Candidate {
  element: HTMLAnchorElement;
  parent: HTMLElement;
  type: LinkType;
  src: string;
}

function toYouTubeEmbedUrl(url: string | undefined): Parsed | undefined {
  const m = url?.match(
    /(?:https?:\/\/)?(?:www\.)?(?:youtube\.com|youtu\.be)\/(?:watch)?(?:\?v=)?([^"&?/ ]{11})(?:\?|&|)(\S*)/i,
  );
  if (!m) return;
  let start = 0;
  m[2].split('&').forEach(p => {
    const s = p.split('=');
    if (s[0] === 't' || s[0] === 'start') {
      if (s[1].match(/^\d+$/)) start = Number.parseInt(s[1]);
      else {
        const n = s[1].match(/(?:(\d+)h)?(?:(\d+)m)?(?:(\d+)s)?/);
        start = n
          ? (Number.parseInt(n[1]) || 0) * 3600 +
            (Number.parseInt(n[2]) || 0) * 60 +
            (Number.parseInt(n[3]) || 0)
          : 0;
      }
    }
  });
  const params = `modestbranding=1&rel=0&controls=2&iv_load_policy=3&start=${start}`;
  return {
    type: 'youtube',
    src: `https://www.youtube.com/embed/${m[1]}?${params}`,
  };
}

function toTwitterEmbedUrl(url: string | undefined): Parsed | undefined {
  const m = url?.match(/(?:https?:\/\/)?(?:www\.)?(?:twitter\.com|x\.com)\/([^\/]+\/status\/\d+)/i);
  return m
    ? {
        type: 'twitter',
        src: `https://twitter.com/${m[1]}`,
      }
    : undefined;
}

const domain = window.location.host;

function toStudyEmbedUrl(url: string | undefined): Parsed | undefined {
  const studyRegex = new RegExp(`${domain}/study/(?:embed/)?(\\w{8})/(\\w{8})(#\\d+)?\\b`, 'i'),
    m = url?.match(studyRegex);
  return m ? { type: 'study', src: `/study/embed/${m[1]}/${m[2]}${m[3] || ''}` } : undefined;
}

function toGameEmbedUrl(url: string | undefined): Parsed | undefined {
  const gameRegex = new RegExp(
      `${domain}/(?:embed/)?(\\w{8})(?:(?:/(sente|gote))|\\w{4}|)(#\\d+)?\\b`,
      'i',
    ),
    m = url?.match(gameRegex);
  if (!m) return;

  let src = `/embed/${m[1]}`;
  if (m[2]) src += `/${m[2]}`; // orientation
  if (m[3]) src += m[3]; // ply hash

  return {
    type: 'game',
    src,
  };
}

function parseLink(a: HTMLAnchorElement): Parsed | undefined {
  if (a.href.replace(/^https?:\/\//, '') !== a.textContent?.replace(/^https?:\/\//, '')) return;
  return (
    toTwitterEmbedUrl(a.href) ||
    toYouTubeEmbedUrl(a.href) ||
    toStudyEmbedUrl(a.href) ||
    toGameEmbedUrl(a.href)
  );
}

function expandYoutube(a: Candidate) {
  const $iframe = $(
    `<div class="embed-wrap"><div class="embed"><iframe src="${a.src}" credentialless></iframe></div></div>`,
  );
  $(a.element).replaceWith($iframe);
  return $iframe;
}

function expandYoutubes(as: Candidate[], wait = 100) {
  wait = Math.min(1500, wait);
  const a = as.shift();
  if (a)
    expandYoutube(a)
      .find('iframe')
      .on('load', () => setTimeout(() => expandYoutubes(as, wait + 200), wait));
}

let twitterLoaded = false;
function expandTwitter(a: Candidate) {
  $(a.element).replaceWith(
    $(
      `<blockquote class="twitter-tweet" data-dnt="true" data-theme="${currentTheme()}"><a href="${
        a.src
      }">${a.src}</a></blockquote>`,
    ),
  );
  if (!twitterLoaded) {
    twitterLoaded = true;

    // polyfill document.createElement so that iframes created by twitter get the `credentialless` attribute
    const originalCreateElement = document.createElement;
    document.createElement = function (this, ...args) {
      const element = originalCreateElement.apply(this, args);
      if (element instanceof HTMLIFrameElement) {
        (element as any).credentialless = true;
      }
      return element;
    };

    loadScript('https://platform.twitter.com/widgets.js');
  }
}

const expandAnalysis = (a: Candidate): HTMLIFrameElement => {
  const iframe = document.createElement('iframe');
  iframe.className = `analyse ${a.type}`;
  iframe.src = a.src;

  const embedDiv = document.createElement('div');
  embedDiv.className = 'embed embed--game';
  embedDiv.appendChild(iframe);

  const embedWrap = document.createElement('div');
  embedWrap.className = 'embed-wrap';
  embedWrap.appendChild(embedDiv);

  a.element.replaceWith(embedWrap);

  iframe.addEventListener('load', function () {
    window.lishogi.pubsub.emit('embed_loaded');
    if (this.contentDocument?.title.startsWith('404')) {
      this.style.height = '100px';
    }
  });

  iframe.addEventListener('mouseenter', function () {
    this.focus();
  });

  return iframe;
};

function expandStudies(studies: Candidate[], delay = 100): void {
  const currentStudy = studies.shift();
  const waitTime = Math.min(1500, delay);

  if (currentStudy) {
    const iframe = expandAnalysis(currentStudy);
    iframe.addEventListener('load', () => {
      setTimeout(() => {
        expandStudies(studies, waitTime + 200);
      }, waitTime);
    });
  }
}

const groupByParent = (games: Candidate[]): Candidate[][] => {
  const groups = new Map<HTMLElement | null, Candidate[]>();

  games.forEach(game => {
    const parent = game.parent;
    if (!groups.has(parent)) {
      groups.set(parent, []);
    }
    groups.get(parent)!.push(game);
  });

  return Array.from(groups.values());
};

const expandGames = (games: Candidate[]): void => {
  groupByParent(games).forEach(group => {
    if (group.length < 3) {
      group.forEach(expandAnalysis);
    } else {
      group.forEach(game => {
        game.element.title = 'Click to expand';
        game.element.classList.add('text');
        game.element.setAttribute('data-icon', '=');

        game.element.addEventListener('click', e => {
          if (e.button === 0) {
            e.preventDefault();
            expandAnalysis(game);
          }
        });
      });
    }
  });
};

function configureSrc(url: string) {
  if (url.includes('://')) return url; // youtube, img, etc
  const parsed = new URL(url, window.location.href),
    theme =
      document.body.dataset.boardTheme === 'custom' ? undefined : document.body.dataset.boardTheme,
    pieceSet = document.body.dataset.pieceSet;
  if (theme) parsed.searchParams.append('theme', theme);
  if (pieceSet) parsed.searchParams.append('pieceSet', pieceSet);
  parsed.searchParams.append('bg', document.body.getAttribute('data-theme')!);
  return parsed.href;
}

// YouTube,  Twitter, studies, games
function main() {
  const as: Candidate[] = Array.from(document.querySelectorAll('.expand-text a'))
    .map((el: HTMLAnchorElement) => {
      if (el.classList.contains('parsed')) return;
      else el.classList.add('parsed');

      const parsed = parseLink(el);
      if (!parsed) return false;
      return {
        element: el,
        parent: el.parentNode,
        type: parsed.type,
        src: configureSrc(parsed.src),
      };
    })
    .filter(a => a) as Candidate[];

  expandYoutubes(as.filter(a => a.type === 'youtube'));

  as.filter(a => a.type === 'twitter').forEach(expandTwitter);

  expandStudies(as.filter(a => a.type === 'study'));

  expandGames(as.filter(a => a.type === 'game'));
}

window.lishogi.registerModule(__bundlename__, main);
window.lishogi.ready.then(() => main());
