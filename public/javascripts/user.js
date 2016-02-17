$(function() {

  var $searchForm = $('form.search.public');
  var loadingSpinner = '<div class="spinner"><svg viewBox="0 0 40 40"><circle cx="20" cy="20" r="18" fill="none"></circle></svg></div>';

  if ($searchForm.length) {
    $searchInput = $searchForm.find('input.search_user');
    $searchInput.on('autocompleteselect', function(e, ui) {
      setTimeout(function() {
        $searchForm.submit();
      }, 10);
    });
    $searchForm.submit(function() {
      location.href = $searchForm.attr('action') + $searchInput.val();
      return false;
    });
  }

  $("div.user_show .mod_zone_toggle").each(function() {
    $(this).click(function() {
      var $zone = $("div.user_show .mod_zone");
      if ($zone.is(':visible')) $zone.hide();
      else $zone.html(loadingSpinner).show();
      $zone.load($(this).attr("href"), function() {
        $zone.find('form.fide_title select').on('change', function() {
          $(this).parent('form').submit();
        });
        $('body').trigger('lichess.content_loaded');
        var relatedUsers = +$('.reportCard thead th:last').text();
        if (relatedUsers > 100) {
          $zone.find('.others').css('display', 'none').before('<a class="others-show">Show ' + relatedUsers + ' related users</a>');
          $zone.find('.others-show').click(function() {
            $(this).next().css('display', '');
            $(this).remove();
          });
        }
        $zone.find('li.ip').slice(0, 3).each(function() {
          var $li = $(this);
          $.ajax({
            url: '/mod/ip-intel?ip=' + $(this).find('.address').text(),
            success: function(res) {
              var p = Math.round(parseFloat(res) * 100);
              $li.append($('<span class="intel">' + p + '% proxy</span>'));
            }
          });
        });
      });
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

  $('.buttonset').buttonset().disableSelection();

  $('form.autosubmit').each(function() {
    var $form = $(this);
    $form.find('input').change(function() {
      $.ajax({
        url: $form.attr('action'),
        method: $form.attr('method'),
        data: $form.serialize(),
        success: function() {
          $form.find('.saved').fadeIn();
        }
      });
    });
  });

  $("div.user_show .claim_title_zone").each(function() {
    var $zone = $(this);
    $zone.find('.actions a').click(function() {
      $.post($(this).attr('href'));
      $zone.remove();
      return false;
    });
  });

  if ($('div.user_show.myself').length &&
    $('div.sub_ratings .relevant').length &&
    lichess.once('user-perf-stats-tour')) lichess.hopscotch(function() {
    hopscotch.configure({
      i18n: {
        doneBtn: 'OK, got it'
      }
    }).startTour({
      id: 'perf-stats',
      showPrevButton: true,
      steps: [{
        title: "New: performance stats",
        content: "You can now click your ratings to display stats about your play!",
        target: $('div.sub_ratings .relevant')[0],
        placement: "right",
        xOffset: -40
      }]
    });
  });
  else if ($('#perfStat.correspondence .view_games').length &&
    lichess.once('user-correspondence-view-games')) lichess.hopscotch(function() {
    hopscotch.configure({
      i18n: {
        doneBtn: 'OK, got it'
      }
    }).startTour({
      id: 'correspondence-games',
      showPrevButton: true,
      steps: [{
        title: "Recently finished games",
        content: "Would you like to display the list of your correspondence games, sorted by completion date?",
        target: $('#perfStat.correspondence .view_games')[0],
        placement: "bottom"
      }]
    });
  });
});
