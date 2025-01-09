import * as domData from 'common/data';

window.lishogi.ready.then(() => {
  $('.user-show .note-zone-toggle').each(function (this: HTMLElement) {
    $(this).on('click',function () {
      $('.user-show .note-zone').toggle();
    });
    if (location.search.includes('note')) $(this).trigger('click');
  });

  $('.user-show .claim_title_zone').each(function () {
    const $zone = $(this);
    $zone.find('.actions a').on('click', function (this: HTMLAnchorElement) {
      window.lishogi.xhr.text('POST', this.href);
      $zone.remove();
      return false;
    });
  });

  $('.user-show .angles').each(function () {
    const $angles = $(this),
      $content = $('.angle-content');
    function browseTo(path: string) {
      const node = document.querySelector('.angle-content .infinitescroll');
      if (node) {
        const infScroll = domData.get<any>(node, 'infinite-scroll');
        infScroll?.destroy();
      }

      window.lishogi.xhr.text('GET', path).then(html => {
        $content.html(html);
        window.lishogi.pubsub.emit('content_loaded');
        history.replaceState({}, '', path);
        window.lishogi.loadInfiniteScroll('.angle-content .infinitescroll');
      });
    }
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

$('button.report-block').one('click', function () {
  const $button = $(this);
  $button.find('span').text('Blocking...');
  window.lishogi.xhr.text('POST', $button.data('action')).then(() => {
    $button.find('span').text('Blocked!');
  });
});
