import * as xhr from 'common/xhr';
import once from "./component/once";
import { hopscotch } from "./component/assets";
import loadInfiniteScroll from "./component/infinite-scroll";

window.lichess.load.then(() => {

  $(".user-show .note-zone-toggle").each(function(this: HTMLElement) {
    $(this).click(function(this: HTMLElement) {
      $(".user-show .note-zone").toggle();
    });
    if (location.search.includes('note')) $(this).click();
  });

  $(".user-show .claim_title_zone").each(function(this: HTMLElement) {
    var $zone = $(this);
    $zone.find('.actions a').click(function(this: HTMLAnchorElement) {
      xhr.text(this.href, { method: 'post' });
      $zone.remove();
      return false;
    });
  });

  if ($('#perfStat.correspondence .view_games').length &&
    once('user-correspondence-view-games')) hopscotch().then(() => {
      window.hopscotch.configure({
        i18n: {
          nextBtn: 'OK, got it'
        }
      }).startTour({
        id: 'correspondence-games',
        showPrevButton: true,
        isTourBubble: false,
        steps: [{
          title: "Recently finished games",
          content: "Would you like to display the list of your correspondence games, sorted by completion date?",
          target: $('#perfStat.correspondence .view_games')[0],
          placement: "bottom"
        }]
      });
    });

  $('.user-show .angles').each(function(this: HTMLElement) {
    const $angles = $(this),
      $content = $('.angle-content'),
      browseTo = (path: string) => {
        $('.angle-content .infinitescroll').infinitescroll('destroy');
        xhr.text(path).then(html => {
          $content.html(html);
          window.lichess.pubsub.emit('content_loaded');
          history.replaceState({}, '', path);
          loadInfiniteScroll('.angle-content .infinitescroll');
        });
      };
    $angles.on('click', 'a', function(this: HTMLElement) {
      $angles.find('.active').removeClass('active');
      $(this).addClass('active');
      browseTo($(this).attr('href'));
      return false;
    });
    $('.user-show').on('click', '#games a', function(this: HTMLAnchorElement) {
      if ($('#games .to-search').hasClass('active') || $(this).hasClass('to-search')) return true;
      $(this).addClass('active');
      browseTo(this.href);
      return false;
    });
  });
});
