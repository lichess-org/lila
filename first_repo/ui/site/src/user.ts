import * as xhr from 'common/xhr';
import once from './component/once';
import { hopscotch } from './component/assets';

lichess.load.then(() => {
  $('.user-show .note-zone-toggle').each(function (this: HTMLElement) {
    $(this).on('click', () => $('.user-show .note-zone').toggle());
    if (location.search.includes('note')) $(this).trigger('click');
  });

  $('.user-show .claim_title_zone').each(function (this: HTMLElement) {
    const $zone = $(this);
    $zone.find('.actions a').on('click', function (this: HTMLAnchorElement) {
      xhr.text(this.href, { method: 'post' });
      $zone.remove();
      return false;
    });
  });

  if ($('#perfStat.correspondence .view_games').length && once('user-correspondence-view-games'))
    hopscotch().then(() => {
      window.hopscotch
        .configure({
          i18n: {
            nextBtn: 'OK, got it',
          },
        })
        .startTour({
          id: 'correspondence-games',
          showPrevButton: true,
          isTourBubble: false,
          steps: [
            {
              title: 'Recently finished games',
              content: 'Would you like to display the list of your correspondence games, sorted by completion date?',
              target: $('#perfStat.correspondence .view_games')[0],
              placement: 'bottom',
            },
          ],
        });
    });

  $('.user-show .angles').each(function (this: HTMLElement) {
    const $angles = $(this),
      $content = $('.angle-content'),
      browseTo = (path: string) =>
        xhr.text(path).then(html => {
          $content.html(html);
          lichess.contentLoaded($content[0]);
          history.replaceState({}, '', path);
          window.InfiniteScroll('.infinite-scroll');
        });
    $angles.on('click', 'a', function (this: HTMLAnchorElement) {
      $angles.find('.active').removeClass('active');
      $(this).addClass('active');
      browseTo(this.href);
      return false;
    });
    $('.user-show').on('click', '#games a', function (this: HTMLAnchorElement) {
      if ($('#games .to-search').hasClass('active') || $(this).hasClass('to-search')) return true;
      $(this).addClass('active');
      browseTo(this.href);
      return false;
    });
  });
});
