import { OpeningPage } from './interfaces';

export default async function wikiTheory(data: OpeningPage) {
  $('.opening__wiki__markup__placeholder').each(function (this: HTMLDivElement) {
    const wrap = $(this);
    fetchAndRender(data, html => wrap.html(html));
  });
}

async function fetchAndRender(data: OpeningPage, render: (html: string) => void) {
  const wikiBooksUrl = 'https://en.wikibooks.org';
  const apiArgs = 'redirects&origin=*&action=query&prop=extracts&formatversion=2&format=json&exchars=1200';

  const removeH1 = (html: string) => html.replace(/<h1>.+<\/h1>/g, '');
  const removeEmptyParagraph = (html: string) => html.replace(/<p>(<br \/>|\s)*<\/p>/g, '');
  const removeTableHeader = (html: string) =>
    html.replace('<h2><span id="Theory_table">Theory table</span></h2>', '');
  const removeTableExpl = (html: string) =>
    html.replace(
      /For explanation of theory tables see theory table and for notation see algebraic notation.?/,
      '',
    );
  const removeContributing = (html: string) =>
    html.replace('When contributing to this Wikibook, please follow the Conventions for organization.', '');

  const readMore = (title: string) =>
    `<p><a target="_blank" href="${wikiBooksUrl}/wiki/${title}">Read more on WikiBooks</a></p>`;

  const transform = (html: string, title: string) =>
    removeH1(removeEmptyParagraph(removeTableHeader(removeTableExpl(removeContributing(html))))) +
    readMore(title);

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
