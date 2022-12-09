import * as xhr from 'common/xhr';
import once from './component/once';
import { hopscotch } from './component/assets';

lichess.load.then(() => {
  const loadNoteZone = () => {
    const $zone = $('.user-show .note-zone');
    $zone.find('textarea')[0]?.focus();
    if ($zone.hasClass('loaded')) return;
    $zone.addClass('loaded');
    $noteToggle.find('strong').text('' + $zone.find('.note').length);
    console.log('load', $zone);
    $zone.find('form').on('submit', function (this: HTMLFormElement) {
      xhr
        .formToXhr(this)
        .then(html => $zone.replaceWith(html))
        .then(() => loadNoteZone())
        .catch(() => alert('Invalid note, is it too short or too long?'));
      return false;
    });
  };

  const $noteToggle = $('.user-show .note-zone-toggle').on('click', () => {
    $('.user-show .note-zone').toggle();
    loadNoteZone();
  });
  if (location.search.includes('note')) $noteToggle.trigger('click');

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
      if ($('#games .to-search').hasClass('active')) return true;
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
