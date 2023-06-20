import * as xhr from 'common/xhr';
import { currentTheme } from 'common/theme';

type LinkType = 'youtube' | 'study' | 'twitter';

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

function toYouTubeEmbedUrl(url: string) {
  const m = url?.match(
    /(?:https?:\/\/)?(?:www\.)?(?:youtube\.com|youtu\.be)\/(?:watch)?(?:\?v=)?([^"&?/ ]{11})(?:\?|&|)(\S*)/i
  );
  if (!m) return;
  let start = 0;
  m[2].split('&').forEach(p => {
    const s = p.split('=');
    if (s[0] === 't' || s[0] === 'start') {
      if (s[1].match(/^\d+$/)) start = parseInt(s[1]);
      else {
        const n = s[1].match(/(?:(\d+)h)?(?:(\d+)m)?(?:(\d+)s)?/);
        start = n ? (parseInt(n[1]) || 0) * 3600 + (parseInt(n[2]) || 0) * 60 + (parseInt(n[3]) || 0) : 0;
      }
    }
  });
  const params = 'modestbranding=1&rel=0&controls=2&iv_load_policy=3&start=' + start;
  return 'https://www.youtube.com/embed/' + m[1] + '?' + params;
}

function toTwitterEmbedUrl(url: string) {
  const m = url?.match(/(?:https?:\/\/)?(?:www\.)?(?:twitter\.com)\/([^/]+\/status\/\d+)/i);
  return m && `https://twitter.com/${m[1]}`;
}

lichess.load.then(() => {
  const domain = window.location.host,
    chapterRegex = new RegExp(domain + '/study/(?:embed/)?(\\w{8})/(\\w{8})(#\\d+)?\\b'),
    studyRegex = new RegExp(domain + '/study/(?:embed/)?(\\w{8})(#\\d+)?\\b');

  function parseLink(a: HTMLAnchorElement): Parsed | undefined {
    if (a.href.replace(/^https?:\/\//, '') !== a.textContent?.replace(/^https?:\/\//, '')) return;
    const tw = toTwitterEmbedUrl(a.href);
    if (tw)
      return {
        type: 'twitter',
        src: tw,
      };
    const yt = toYouTubeEmbedUrl(a.href);
    if (yt)
      return {
        type: 'youtube',
        src: yt,
      };
    let matches = a.href.match(chapterRegex);
    if (matches && matches[2])
      return {
        type: 'study',
        src: `/study/embed/${matches[1]}/${matches[2]}${matches[3] || ''}`,
      };
    matches = a.href.match(studyRegex);
    if (matches && matches[1])
      return {
        type: 'study',
        src: `/study/embed/${matches[1]}/autochap${matches[2] || ''}`,
      };
  }

  function expandYoutube(a: Candidate) {
    const $iframe = $('<div class="embed"><iframe src="' + a.src + '" credentialless></iframe></div>');
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
        }">${a.src}</a></blockquote>`
      )
    );
    if (!twitterLoaded) {
      twitterLoaded = true;

      // polyfill document.createElement so that iframes created by twitter get the `credentialless` attribute
      const originalCreateElement = document.createElement;
      document.createElement = function () {
        const element = originalCreateElement.apply(this, arguments as any);
        if (element instanceof HTMLIFrameElement) {
          (element as any).credentialless = true;
        }
        return element;
      };

      xhr.script('https://platform.twitter.com/widgets.js');
    }
  }

  function expandStudy(a: Candidate) {
    const $iframe: any = $('<iframe>')
      .addClass('analyse ' + a.type)
      .attr('src', a.src);
    $(a.element).replaceWith($('<div class="embed embed--study">').prepend($iframe));
    return $iframe
      .on('load', function (this: HTMLIFrameElement) {
        if (this.contentDocument?.title.startsWith('404')) this.style.height = '100px';
      })
      .on('mouseenter', function (this: HTMLElement) {
        this.focus();
      });
  }

  function expandStudies(as: Candidate[], wait = 100) {
    const a = as.shift();
    wait = Math.min(1500, wait);
    if (a)
      expandStudy(a).on('load', () => {
        setTimeout(() => expandStudies(as, wait + 200), wait);
      });
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

  function configureSrc(url: string) {
    if (url.includes('://')) return url; // youtube, img, etc
    const parsed = new URL(url, window.location.href);
    const theme = themes.find(theme => document.body.classList.contains(theme));
    const pieceSet = document.body.dataset.pieceSet;
    if (theme) parsed.searchParams.append('theme', theme);
    if (pieceSet) parsed.searchParams.append('pieceSet', pieceSet);
    parsed.searchParams.append('bg', document.body.getAttribute('data-theme')!);
    return parsed.href;
  }

  const as: Candidate[] = Array.from(document.querySelectorAll('.expand-text a'))
    .map((el: HTMLAnchorElement) => {
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

  expandStudies(
    as
      .filter(a => a.type === 'study')
      .map(a => {
        a.element.classList.add('embedding_analyse');
        a.element.innerHTML = lichess.spinnerHtml;
        return a;
      })
  );

  expandLpv();
});

const expandLpv = async () => {
  if ($('.lpv--autostart').length) {
    lichess.loadCssPath('lpv');
    await lichess.loadEsm('lpv');
  }
};
