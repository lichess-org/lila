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
    class: 'dialog-content',
    show: true,
    htmlText: `
      <div class="continue-with">
      <button class="button bookmark-game">${i18n.site.bookmarkThisGame}</button>
      <button class="button bookmark-position">${i18n.site.bookmarkThisPosition}</button>
      </div>
    `,
    actions: [
      {
        selector: 'button.bookmark-game',
        listener: getListener(gameURL),
      },
      {
        selector: 'button.bookmark-position',
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

    const gameURL = this.href;

    if (!isBookmarked) {
      const positionURL = `${gameURL}?ply=${data.ply}&fen=${data.fen}&color=${data.color}${data.uci ? '&uci=' + data.uci : ''}`;
      bookmarkOptions(gameURL, positionURL, this);
    } else {
      xhrText(gameURL, { method: 'post' });
      finishBookmarking(this);
    }

    return false;
  });
}
