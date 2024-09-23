import * as xhr from 'common/xhr';
import { spinnerHtml } from 'common/spinner';
import { pubsub } from 'common/pubsub';

export function initModule(selector: string = '.infinite-scroll'): void {
  $(selector).each(function(this: HTMLElement) {
    register(this, selector);
  });
}

function register(el: HTMLElement, selector: string, backoff = 500) {
  const nav = el.querySelector('.pager') as HTMLAnchorElement | null,
    next = nav?.querySelector('.pager a') as HTMLAnchorElement | null,
    nextUrl = next?.href;

  if (nav && nextUrl)
    new Promise<void>(res => {
      if (isVisible(nav)) res();
      else
        window.addEventListener(
          'scroll',
          function scrollListener() {
            if (isVisible(nav)) {
              window.removeEventListener('scroll', scrollListener);
              res();
            }
          },
          { passive: true },
        );
    })
      .then(() => {
        nav.innerHTML = spinnerHtml;
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

function isVisible(el: HTMLElement) {
  const { top, bottom } = el.getBoundingClientRect();
  return (top > 0 || bottom > 0) && top < window.innerHeight;
}

function dedupEntries(el: HTMLElement) {
  const ids = new Set<string>();
  $(el)
    .find('[data-dedup]')
    .each(function(this: HTMLElement) {
      const id = this.dataset.dedup;
      if (id) {
        if (ids.has(id)) $(this).remove();
        else ids.add(id);
      }
    });
}
