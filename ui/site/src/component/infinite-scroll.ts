import spinnerHtml from './spinner';

export default function loadInfiniteScroll(selector: string) {
  $(selector).each(function(this: HTMLElement) {
    const self = this;
    if (!$('.pager a', this).length) return;
    var $scroller = $(this).infinitescroll({
      navSelector: ".pager",
      nextSelector: ".pager a",
      itemSelector: ".infinitescroll .paginated",
      errorCallback: function() {
        $("#infscr-loading").remove();
      },
      loading: {
        msg: $('<div id="infscr-loading">').html(spinnerHtml)
      }
    }, function() {
      $("#infscr-loading").remove();
      window.lichess.pubsub.emit('content_loaded');
      const ids: string[] = [];
      $(self).find('.paginated[data-dedup]').each(function(this: HTMLElement) {
        const id = $(this).data('dedup');
        if (id) {
          if (ids.includes(id)) $(this).remove();
          else ids.push(id);
        }
      });
    }).find('div.pager').hide().end();
    $scroller.parent().append($('<button class="inf-more button button-empty">&hellip;</button>').on('click', function() {
      $scroller.infinitescroll('retrieve');
    }));
  });
}
