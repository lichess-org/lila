import * as xhr from 'common/xhr';

lichess.load.then(() => {
  $('.slist, slist-pad')
    .find('.mark-alt')
    .on('click', function (this: HTMLAnchorElement) {
      if (confirm('Close alt account?')) {
        xhr.text(this.getAttribute('href')!, { method: 'post' });
        $(this).remove();
      }
    });
});
