import { text as xhrText } from 'lib/xhr';
import { domDialog, type Dialog } from '@/view';

interface BookmarkData {
  ply: Ply;
  fen: FEN;
  color: Color;
  uci?: Uci;
}

async function bookmarkOptions(gameURL: string, positionURL: string, t: HTMLAnchorElement): Promise<void> {
  function getListener(url: string) {
    const listener = (e: Event, dialog: Dialog) => {
      e.preventDefault();
      xhrText(url, { method: 'post' });
      finishBookmarking(t);
      dialog.close();
    };
    return listener;
  }

  await domDialog({
    class: 'continue-with',
    show: true,
    htmlText: `
      <a class="button" rel="nofollow">${i18n.site.bookmarkThisGame}</a>
      <a class="button" rel="nofollow">${i18n.site.bookmarkThisPosition}</a>
    `,
    actions: [
      {
        selector: 'a:first-child',
        listener: getListener(gameURL),
      },
      {
        selector: 'a:last-child',
        listener: getListener(positionURL),
      },
    ],
  });
}

function finishBookmarking(el: HTMLAnchorElement): void {
  const t = $(el).toggleClass('bookmarked');
  const count = (parseInt(t.text(), 10) || 0) + (t.hasClass('bookmarked') ? 1 : -1);
  t.find('span').html('' + (count > 0 ? count : ''));
}

export function bindBookmarkButton(getData: () => BookmarkData): void {
  $('#main-wrap').on('click', 'a.bookmark', function (this: HTMLAnchorElement) {
    const isBookmarked = $(this).hasClass('bookmarked');
    const data = getData();
    console.log('bookmarking', data);

    const gameURL = this.href;
    console.log(`GameURL: ${gameURL}`);

    if (!isBookmarked) {
      const positionURL = `${gameURL}?ply=${data.ply}&fen=${data.fen}&col=${data.color}${data.uci ? '&uci=' + data.uci : ''}`;
      console.log(`PositionURL: ${positionURL}`);
      bookmarkOptions(gameURL, positionURL, this);
    } else {
      xhrText(gameURL, { method: 'post' });
      finishBookmarking(this);
    }

    return false;
  });
}
