$(function() {

  $('div.user_show .mod_zone_toggle').each(function() {

    $(this).click(function() {
      var $zone = $('div.user_show .mod_zone');
      if ($zone.is(':visible')) $zone.hide();
      else {
        $zone.html(lichess.spinnerHtml).show();
        lichess.loadCss('/assets/stylesheets/user-mod.css');
        $zone.load($(this).attr('href'));
      }
      return false;
    });
    if (location.search.indexOf('mod') === 1) $(this).click();
  });

  $("div.user_show .note_zone_toggle").each(function() {
    $(this).click(function() {
      $("div.user_show .note_zone").toggle();
    });
    if (location.search.indexOf('note') != -1) $(this).click();
  });

  $("div.user_show .claim_title_zone").each(function() {
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

    $('.content_box_inter.angles').each(function() {
      var $angles = $(this),
      $content = $('.angle_content');
      function browseTo(path) {
        $('.angle_content .infinitescroll').infinitescroll('destroy');
        $.get(path).then(function(html) {
          $content.html(html);
          lichess.pubsub.emit('content_loaded')();
          history.replaceState({}, '', path);
          lichess.loadInfiniteScroll('.angle_content .infinitescroll');
        });
      }
      $angles.on('click', 'a', function() {
        $angles.find('.active').removeClass('active');
        $(this).addClass('active');
        browseTo($(this).attr('href'));
        if ($(this).data('tab') === 'activity') lichess.loadCss('/assets/stylesheets/activity.css');
        return false;
      });
      $('.user_show').on('click', '#games a', function() {
        if ($('#games .to_search').hasClass('active') || $(this).hasClass('to_search')) return true;
        $filters = $(this).parent();
        $(this).addClass('active');
        browseTo($(this).attr('href'));
        return false;
      });
    });
});
