import debounce from 'common/debounce';

export class WikiTheory {
  update = debounce(
    async (nodes: Tree.Node[]) => {
      const path = nodes
        .slice(1)
        .map(n => `${this.plyPrefix(n)}${n.san}`)
        .join('/');
      if (!path) this.show('');
      else if (this.cache.has(path)) this.show(this.cache.get(path)!);
      else {
        const title = `Chess_Opening_Theory/${path}`;
        const url = `${wikiBooksUrl}/w/api.php?titles=${title}&${this.urlArgs}`;
        const res = await fetch(url);
        const saveAndShow = (html: string) => {
          this.cache.set(path, html);
          this.show(html);
        };
        if (res.ok) {
          const json = await res.json();
          if (json.query.pages[0].missing) saveAndShow('');
          else saveAndShow(transform(json.query.pages[0].extract, title));
        } else saveAndShow('');
      }
    },
    500,
    true
  );
  private cache = new Map<string, string>();
  private urlArgs = 'origin=*&action=query&prop=extracts&formatversion=2&format=json&exchars=1200';
  private plyPrefix = (node: Tree.Node) => `${Math.floor((node.ply + 1) / 2)}${node.ply % 2 === 1 ? '._' : '...'}`;
  private el = () => $('.analyse__wiki');
  private show = (html: string) => this.el().html(html);
}

const wikiBooksUrl = 'https://en.wikibooks.org';

const transform = (html: string, title: string) =>
  removeEmptyParagraph(removeTableHeader(removeTableExpl(removeContributing(html)))) + readMore(title);

const removeEmptyParagraph = (html: string) => html.replace(/<p>(<br \/>|\s)*<\/p>/g, '');

const removeTableHeader = (html: string) => html.replace('<h2><span id="Theory_table">Theory table</span></h2>', '');
const removeTableExpl = (html: string) =>
  html.replace(/For explanation of theory tables see theory table and for notation see algebraic notation.?/, '');
const removeContributing = (html: string) =>
  html.replace('When contributing to this Wikibook, please follow the Conventions for organization.', '');

const readMore = (title: string) =>
  `<p><a target="_blank" href="${wikiBooksUrl}/wiki/${title}">Read more on WikiBooks</a></p>`;
