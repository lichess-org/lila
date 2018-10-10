lichess.loadInfiniteScroll = function(el) {
  $(el).each(function() {
    if (!$('.pager a', this).length) return;
    var $scroller = $(this).infinitescroll({
      navSelector: ".pager",
      nextSelector: ".pager a",
      itemSelector: ".infinitescroll .paginated_element",
      errorCallback: function() {
        $("#infscr-loading").remove();
      },
      loading: {
        msg: $('<div id="infscr-loading">').html(lichess.spinnerHtml)
      }
    }, function() {
      $("#infscr-loading").remove();
      lichess.pubsub.emit('content_loaded')();
      var ids = [];
      $(el).find('.paginated_element[data-dedup]').each(function() {
        var id = $(this).data('dedup');
        if (id) {
          if (lichess.fp.contains(ids, id)) $(this).remove();
          else ids.push(id);
        }
      });
    }).find('div.pager').hide().end();

    const $moreButton = $('<button class="button inf-more">More</button>').on('click', function() {
      $scroller.infinitescroll('retrieve');
    });

    var $moreButtonParent = $scroller.parent();
    // prevent adding buttons as child of elements where buttons are not allowed
    if ($moreButton.is('table, ul, ol, dl')) {
      $moreButtonParent = $moreButtonParent.parent();
    }
    $moreButtonParent.after($moreButton);
  });
};
