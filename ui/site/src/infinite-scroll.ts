import spinnerHtml from './component/spinner';
import * as xhr from 'common/xhr';

export default function InfiniteScroll(selector: string) {
  $(selector).each(function(this: HTMLElement) {
    register(this, selector);
  });
}

function register(el: HTMLElement, selector: string) {

  const nav = el.querySelector('.pager') as HTMLAnchorElement | null,
    next = nav?.querySelector('.pager a') as HTMLAnchorElement | null,
    nextUrl = next?.href;

  if (nav && nextUrl) new Promise(res =>
    window.addEventListener('scroll', function scrollListener() {
      if (isVisible(nav)) {
        window.removeEventListener('scroll', scrollListener);
        res();
      }
    }, { passive: true })
  )
    .then(() => {
      nav.innerHTML = spinnerHtml;
      return xhr.text(nextUrl);
    })
    .then(html => {
      nav.remove();
      $(el).append($(html).find(selector).html());
      window.lichess.contentLoaded(el);
      register(el, selector);
    })
    .catch(e => {
      console.log(e);
      nav.remove();
    });
}

function isVisible(el: HTMLElement) {
  const { top, bottom } = el.getBoundingClientRect();
  return (top > 0 || bottom > 0) && top < window.innerHeight;
}
