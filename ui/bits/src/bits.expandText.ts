import { embedYoutubeUrl, parseYoutubeUrl } from './youtubeLinkProcessor';

type LinkType = 'youtube';

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

function toYoutubeEmbedUrl(url: string): string | undefined {
  const result = parseYoutubeUrl(url);
  if (result) {
    return embedYoutubeUrl(result);
  }
}

site.load.then(() => {
  function parseLink(a: HTMLAnchorElement): Parsed | undefined {
    if (a.href.replace(/^https?:\/\//, '') !== a.textContent?.replace(/^https?:\/\//, '')) return;
    const yt = toYoutubeEmbedUrl(a.href);
    if (yt)
      return {
        type: 'youtube',
        src: yt,
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

  const as: Candidate[] = Array.from(document.querySelectorAll('.expand-text a'))
    .map((el: HTMLAnchorElement) => {
      const parsed = parseLink(el);
      if (!parsed) return false;
      return {
        element: el,
        parent: el.parentNode,
        type: parsed.type,
        src: parsed.src,
      };
    })
    .filter(a => a) as Candidate[];

  expandYoutubes(as.filter(a => a.type === 'youtube'));

  if ($('.lpv--autostart').length) site.asset.loadEsm('bits.lpv');
});
