$(function() {

  var $searchForm = $('form.search');

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
      else $zone.html("Loading...").show();
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

});
lichess = lichess || {};
lichess.startTournamentStatsTour = function() {
  var baseUrl = $('body').data('asset-url');
  $('head').append($('<link rel="stylesheet" type="text/css" />')
    .attr('href', baseUrl + '/assets/vendor/hopscotch/dist/css/hopscotch.min.css'));
  $.getScript(baseUrl + "/assets/vendor/hopscotch/dist/js/hopscotch.min.js").done(function() {
    hopscotch.startTour({
      id: "user-tournaments",
      showPrevButton: true,
      steps: [{
        title: "New feature: tournament stats",
        content: "You can now click your tournament points to review your " +
          "recent and best tournaments.",
        target: "#lichess .tournament_points",
        placement: "bottom"
      }]
    });
  });
}
