import type { OpeningPage } from './interfaces';

export default function wikiTheory(data: OpeningPage): void {
  $('.opening__wiki__markup__placeholder').each(function (this: HTMLDivElement) {
    const wrap = $(this);
    fetchAndRender(data, html => wrap.html(html));
  });
}

async function fetchAndRender(data: OpeningPage, render: (html: string) => void) {
  const wikiBooksUrl = 'https://en.wikibooks.org';
  const apiArgs = 'redirects&origin=*&action=query&prop=extracts&formatversion=2&format=json&stable=1';

  const removeH1 = (html: string) => html.replace(/<h1.+<\/h1>/g, '');
  const removeEmptyParagraph = (html: string) => html.replace(/<p>(<br \/>|\s)*<\/p>/g, '');
  const removeTheoryTableSection = (html: string) =>
    html.replace(/<h2 data-mw-anchor="Theory_table">Theory table<\/h2>.*?(?=<h[1-6]|$)/gs, '');

  const removeAllBlacksMovesSection = (html: string) =>
    html.replace(
      /<h2 data-mw-anchor="All_possible_Black's_moves" data-mw-fallback-anchor="All_possible_Black\.27s_moves">All possible Black's moves<\/h2>.*?(?=<h[1-6]|$)/gs,
      '',
    );

  const removeAllPossibleRepliesSection = (html: string) =>
    html.replace(
      /<h3 data-mw-anchor="All_possible_replies">All possible replies<\/h3>.*?(?=<h[1-6]|$)/gs,
      '',
    );

  const removeExternalLinksSection = (html: string) =>
    html.replace(/<h2 data-mw-anchor="External_links">External links<\/h2>.*?(?=<h[1-6]|$)/gs, '');
  const removeContributing = (html: string) =>
    html.replace('When contributing to this Wikibook, please follow the Conventions for organization.', '');

  const readMore = (title: string) =>
    `<p><a target="_blank" href="${wikiBooksUrl}/wiki/${title}">Read more on WikiBooks</a></p>`;

  const transform = (html: string, title: string) =>
    removeH1(
      removeEmptyParagraph(
        removeTheoryTableSection(
          removeAllBlacksMovesSection(
            removeAllPossibleRepliesSection(removeExternalLinksSection(removeContributing(html))),
          ),
        ),
      ),
    ) + readMore(title);

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
        return render(transform(page.extract, title));
      }
    } else return;
  } catch (err) {
    console.warn(err);
    return;
  }
}
