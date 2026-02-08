import { text as xhrText } from 'lib/xhr';

interface BookmarkData {
  ply: Ply;
  // ... more
}

export function bindBookmarkButton(getData: () => BookmarkData): void {
  $('#main-wrap').on('click', 'a.bookmark', function (this: HTMLAnchorElement) {
    const data = getData();
    console.log('bookmarking', data);
    const t = $(this).toggleClass('bookmarked');
    xhrText(this.href, { method: 'post' });
    const count = (parseInt(t.text(), 10) || 0) + (t.hasClass('bookmarked') ? 1 : -1);
    t.find('span').html('' + (count > 0 ? count : ''));
    return false;
  });
}
