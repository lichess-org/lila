import { pubsub } from 'lib/pubsub';
import { spinnerHtml } from 'lib/view';
import * as xhr from 'lib/xhr';

export function initModule(selector = '.infinite-scroll'): void {
  $(selector).each(function (this: HTMLElement) {
    register(this, selector);
  });
}

function register(el: HTMLElement, selector: string, backoff = 500) {
  const nav = el.querySelector<HTMLElement>('.pager');
  const next = nav?.querySelector<HTMLAnchorElement>('a');
  const nextUrl = next?.href;
  const scrollSelector = el.dataset.scrollSelector;
  const scrollEl: HTMLElement | Window = (scrollSelector && document.querySelector(scrollSelector)) || window;

  if (nav && nextUrl)
    new Promise<void>(res => {
      if (isVisible(nav)) res();
      else
        scrollEl.addEventListener(
          'scroll',
          function scrollListener() {
            if (isVisible(nav)) {
              scrollEl.removeEventListener('scroll', scrollListener);
              res();
            }
          },
          { passive: true },
        );
    })
      .then(() => {
        // in tables .pager is the <tr> - writing into it drops the cell and the spinner lands in an
        // anonymous single-column cell
        const cell = nav.querySelector<HTMLTableCellElement>(':scope > th, :scope > td');
        if (cell) cell.colSpan = renderedColumns(nav);
        (cell ?? nav).innerHTML = spinnerHtml;
        return xhr.text(nextUrl);
      })
      .then(
        html => {
          nav.remove();
          $(el).append(($(html).is(selector) ? $(html) : $(html).find(selector)).html());
          dedupEntries(el);
          pubsub.emit('content-loaded', el);
          setTimeout(() => register(el, selector, backoff * 1.05), backoff); // recursion with backoff
        },
        e => {
          console.log(e);
          nav.remove();
        },
      );
}

// count the cells css actually renders - several tables drop columns at narrow widths and spanning
// the dropped ones adds empty phantom columns that squeeze the visible ones
function renderedColumns(pagerRow: HTMLElement) {
  const cells = (pagerRow.previousElementSibling as HTMLTableRowElement | null)?.cells;
  if (!cells) return 1;
  return [...cells].reduce((n, cell) => n + (cell.getClientRects().length ? cell.colSpan : 0), 0) || 1;
}

function isVisible(el: HTMLElement) {
  const { top, bottom } = el.getBoundingClientRect();
  return (top > 0 || bottom > 0) && top < window.innerHeight;
}

function dedupEntries(el: HTMLElement) {
  const ids = new Set<string>();
  $(el)
    .find('[data-dedup]')
    .each(function (this: HTMLElement) {
      const id = this.dataset.dedup;
      if (id) {
        if (ids.has(id)) $(this).remove();
        else ids.add(id);
      }
    });
}
