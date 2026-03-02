import type { OpeningPage } from './interfaces';
import { wikiBooksUrl, apiArgs, transformWikiHtml } from 'lib/wikiBooks';

export default function wikiTheory(data: OpeningPage): void {
  $('.opening__wiki__markup__placeholder').each(function (this: HTMLDivElement) {
    const wrap = $(this);
    fetchAndRender(data, html => wrap.html(html));
  });
}

async function fetchAndRender(data: OpeningPage, render: (html: string) => void) {
  const plyPrefix = (ply: number) => `${Math.floor((ply + 1) / 2)}${ply % 2 === 1 ? '._' : '...'}`;
  const pathParts = data.sans.map((san, i) => `${plyPrefix(i + 1)}${san}`);
  const path = pathParts.join('/').replace(/[+!#?]/g, '') ?? '';
  if (pathParts.length > 30 || !path || path.length > 255 - 21) return;
  const title = `Chess_Opening_Theory/${path}`;
  try {
    const res = await fetch(`${wikiBooksUrl}/w/api.php?titles=${title}&${apiArgs}`);
    if (res.ok) {
      const json = await res.json();
      const page = json.query.pages[0];
      if (page.missing) return;
      else if (page.invalid) {
        console.warn('invalid request: ' + page.invalidreason);
        return;
      } else if (!page.extract) {
        console.warn('error: unexpected API response:<br><pre>' + JSON.stringify(page) + '</pre>');
        return;
      } else {
        return render(transformWikiHtml(page.extract, title));
      }
    } else return;
  } catch (err) {
    console.warn(err);
    return;
  }
}
