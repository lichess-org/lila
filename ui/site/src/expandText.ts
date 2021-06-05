import spinner from './component/spinner';

function toYouTubeEmbedUrl(url: string) {
  if (!url) return;
  const m = url.match(
    /(?:https?:\/\/)?(?:www\.)?(?:youtube\.com|youtu\.be)\/(?:watch)?(?:\?v=)?([^"&?/ ]{11})(?:\?|&|)(\S*)/i
  );
  if (!m) return;
  let start = 1;
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

type LinkType = 'youtube' | 'study' | 'game';

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

interface Group {
  parent: HTMLElement | null;
  index: number;
}

lichess.load.then(() => {
  const domain = window.location.host,
    studyRegex = new RegExp(domain + '/study/(?:embed/)?(\\w{8})/(\\w{8})(#\\d+)?\\b'),
    gameRegex = new RegExp(domain + '/(?:embed/)?(\\w{8})(?:(?:/(white|black))|\\w{4}|)(#\\d+)?\\b'),
    notGames = ['training', 'analysis', 'insights', 'practice', 'features', 'password', 'streamer', 'timeline'];

  function parseLink(a: HTMLAnchorElement): Parsed | undefined {
    const yt = toYouTubeEmbedUrl(a.href);
    if (yt)
      return {
        type: 'youtube',
        src: yt,
      };
    let matches = a.href.match(studyRegex);
    if (matches && matches[2] && a.text.match(studyRegex))
      return {
        type: 'study',
        src: '/study/embed/' + matches[1] + '/' + matches[2] + (matches[3] || ''),
      };
    matches = a.href.match(gameRegex);
    if (matches && matches[1] && !notGames.includes(matches[1]) && a.text.match(gameRegex)) {
      let src = '/embed/' + matches[1];
      if (matches[2]) src += '/' + matches[2]; // orientation
      if (matches[3]) src += matches[3]; // ply hash
      return {
        type: 'game',
        src: src,
      };
    }
  }

  function expandYoutube(a: Candidate) {
    const $iframe = $('<div class="embed"><iframe src="' + a.src + '"></iframe></div>');
    $(a.element).replaceWith($iframe);
    return $iframe;
  }

  function expandYoutubes(as: Candidate[], wait = 100) {
    wait = Math.min(1500, wait);
    const a = as.shift();
    if (a)
      expandYoutube(a)
        .find('iframe')
        .on('load', function () {
          setTimeout(function () {
            expandYoutubes(as, wait + 200);
          }, wait);
        });
  }

  function expand(a: Candidate) {
    const $iframe: any = $('<iframe>')
      .addClass('analyse ' + a.type)
      .attr('src', a.src);
    $(a.element).replaceWith($('<div class="embed">').prepend($iframe));
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
      expand(a).on('load', () => {
        setTimeout(() => expandStudies(as, wait + 200), wait);
      });
  }

  function groupByParent(as: Candidate[]) {
    const groups: Candidate[][] = [];
    let current: Group = {
      parent: null,
      index: -1,
    };
    as.forEach(a => {
      if (a.parent === current.parent) groups[current.index].push(a);
      else {
        current = {
          parent: a.parent,
          index: current.index + 1,
        };
        groups[current.index] = [a];
      }
    });
    return groups;
  }

  function expandGames(as: Candidate[]) {
    groupByParent(as).forEach(group => {
      if (group.length < 3) group.forEach(expand);
      else
        group.forEach(a => {
          a.element.title = 'Click to expand';
          a.element.classList.add('text');
          a.element.setAttribute('data-icon', '');
          a.element.addEventListener('click', function (e) {
            if (e.button === 0) {
              e.preventDefault();
              expand(a);
            }
          });
        });
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
    if (theme) parsed.searchParams.append('theme', theme);
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

  expandStudies(
    as
      .filter(a => a.type === 'study')
      .map(a => {
        a.element.classList.add('embedding_analyse');
        a.element.innerHTML = spinner;
        return a;
      })
  );

  expandGames(as.filter(a => a.type === 'game'));
});
