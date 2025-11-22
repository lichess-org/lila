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
  const m = url?.match(
    /(?:https?:\/\/)?(?:www\.)?(?:youtube\.com|youtu\.be)\/(watch|embed)?(?:\?v=|\/)?([^"&?/ ]{11})(?:\?|&|)(\S*)/i,
  );
  if (!m) return;
  const [, linkType, videoId, linkParams] = m;
  if (linkType === 'embed') return url.startsWith('https://') ? url : 'https://' + url;
  let start = 0;
  linkParams.split('&').forEach(p => {
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
  return 'https://www.youtube-nocookie.com/embed/' + videoId + '?' + params;
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
