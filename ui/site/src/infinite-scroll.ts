import * as domData from 'common/data';
import { spinnerHtml } from 'common/spinner';

export function loadInfiniteScroll(sel: string): void {
  document.querySelectorAll(sel).forEach((el: HTMLElement) => {
    if (!el.querySelector('.pager a')) return;

    const parent = getOutOfTable(el);

    if (parent) {
      const statusDiv = document.createElement('div');
      statusDiv.className = 'page-load-status';
      statusDiv.innerHTML = `<div id="infscr-loading" class="infinite-scroll-request">${spinnerHtml}</div>`;
      parent.append(statusDiv);
    }

    const infScroll = new window.InfiniteScroll(el, {
      path: '.pager a',
      append: '.infinitescroll .paginated',
      history: false,
      status: '.page-load-status',
      hideNav: '.pager',
    });

    domData.set(el, 'infinite-scroll', infScroll);

    infScroll.on('error', () => {
      document.getElementById('infscr-loading')?.remove();
    });

    infScroll.on('append', () => {
      window.lishogi.pubsub.emit('content_loaded');
      const ids: string[] = [];
      el.querySelectorAll('.paginated[data-dedup]').forEach((dedupEL: HTMLElement) => {
        const id = dedupEL.dataset.dedup;
        if (id) {
          if (ids.includes(id)) dedupEL.remove();
          else ids.push(id);
        }
      });
    });

    if (parent) {
      // Create and append a new button
      const moreButton = document.createElement('button');
      moreButton.className = 'inf-more button button-empty';
      moreButton.textContent = '…';
      moreButton.addEventListener('click', () => {
        infScroll.loadNextPage();
      });
      parent.appendChild(moreButton);
    }
  });
}

function getOutOfTable(el: HTMLElement | null): HTMLElement | null {
  if (el && ['TABLE', 'TBODY'].includes(el.tagName)) return getOutOfTable(el.parentElement);
  else return el;
}
