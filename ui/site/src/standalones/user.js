$(function() {

  $(".user-show .note-zone-toggle").each(function() {
    $(this).click(function() {
      $(".user-show .note-zone").toggle();
    });
    if (location.search.includes('note')) $(this).click();
  });

  $(".user-show .claim_title_zone").each(function() {
    var $zone = $(this);
    $zone.find('.actions a').click(function() {
      $.post($(this).attr('href'));
      $zone.remove();
      return false;
    });
  });

  if ($('#perfStat.correspondence .view_games').length &&
    lichess.once('user-correspondence-view-games')) lichess.hopscotch(function() {
      hopscotch.configure({
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

    $('.user-show .angles').each(function() {
      var $angles = $(this),
      $content = $('.angle-content');
      function browseTo(path) {
        $('.angle-content .infinitescroll').infinitescroll('destroy');
        $.get(path).then(function(html) {
          $content.html(html);
          lichess.pubsub.emit('content_loaded');
          history.replaceState({}, '', path);
          lichess.loadInfiniteScroll('.angle-content .infinitescroll');
        });
      }
      $angles.on('click', 'a', function() {
        $angles.find('.active').removeClass('active');
        $(this).addClass('active');
        browseTo($(this).attr('href'));
        return false;
      });
      $('.user-show').on('click', '#games a', function() {
        if ($('#games .to-search').hasClass('active') || $(this).hasClass('to-search')) return true;
        $(this).addClass('active');
        browseTo($(this).attr('href'));
        return false;
      });
    });
});
