lichess.loadInfiniteScroll = el => {
  $(el).each(function() {
    if (!$('.pager a', this).length) return;
    var $scroller = $(this).infinitescroll({
      navSelector: ".pager",
      nextSelector: ".pager a",
      itemSelector: ".infinitescroll .paginated",
      errorCallback: function() {
        $("#infscr-loading").remove();
      },
      loading: {
        msg: $('<div id="infscr-loading">').html(lichess.spinnerHtml)
      }
    }, function() {
      $("#infscr-loading").remove();
      lichess.pubsub.emit('content_loaded');
      var ids = [];
      $(el).find('.paginated[data-dedup]').each(function() {
        var id = $(this).data('dedup');
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
