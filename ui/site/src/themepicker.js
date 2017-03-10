lichess.themepicker = function(toggle, currentZoom, setZoom, manuallySetZoom) {
  var applyBackground = function(v) {
    var bgData = document.getElementById('bg-data');
    bgData ? bgData.innerHTML = 'body.transp::before{background-image:url(' + v + ');}' :
      $('head').append('<style id="bg-data">body.transp::before{background-image:url(' + v + ');}</style>');
  };
  var $themepicker = $('#themepicker');
  var findInBodyClasses = function(choices) {
    var list = document.body.classList;
    for (var i in list)
    if (lichess.fp.contains(choices, list[i])) return list[i];
  };
  $.ajax({
    url: $(toggle).data('url'),
    success: function(html) {
      $themepicker.append(html);
      var $body = $('body');
      var $content = $body.children('.content');
      var $dropdown = $themepicker.find('.dropdown');
      var $pieceSprite = $('#piece-sprite');
      var themes = $dropdown.data('themes').split(' ');
      var theme = findInBodyClasses(themes);
      var set = $body.data('piece-set');
      var theme3ds = $dropdown.data('theme3ds').split(' ');
      var theme3d = findInBodyClasses(theme3ds);
      var set3ds = $dropdown.data('set3ds').split(' ');
      var set3d = findInBodyClasses(set3ds);
      var background = $body.data('bg');
      var is3d = $content.hasClass('is3d');
      $themepicker.find('.is2d div.theme').hover(function() {
        $body.removeClass(themes.join(' ')).addClass($(this).data("theme"));
      }, function() {
        $body.removeClass(themes.join(' ')).addClass(theme);
      }).click(function() {
        theme = $(this).data("theme");
        $.post($(this).parent().data("href"), {
          theme: theme
        }, lichess.reloadOtherTabs);
      });
      $themepicker.find('.is2d div.no-square').hover(function() {
        var s = $(this).data("set");
        $pieceSprite.attr('href', $pieceSprite.attr('href').replace(/\w+\.css/, s + '.css'));
      }, function() {
        $pieceSprite.attr('href', $pieceSprite.attr('href').replace(/\w+\.css/, set + '.css'));
      }).click(function() {
        set = $(this).data("set");
        $.post($(this).parent().data("href"), {
          set: set
        }, lichess.reloadOtherTabs);
      });
      $themepicker.find('.is3d div.theme').hover(function() {
        $body.removeClass(theme3ds.join(' ')).addClass($(this).data("theme"));
      }, function() {
        $body.removeClass(theme3ds.join(' ')).addClass(theme3d);
      }).click(function() {
        theme3d = $(this).data("theme");
        $.post($(this).parent().data("href"), {
          theme: theme3d
        }, lichess.reloadOtherTabs);
      });
      $themepicker.find('.is3d div.no-square').hover(function() {
        $body.removeClass(set3ds.join(' ')).addClass($(this).data("set"));
      }, function() {
        $body.removeClass(set3ds.join(' ')).addClass(set3d);
      }).click(function() {
        set3d = $(this).data("set");
        $.post($(this).parent().data("href"), {
          set: set3d
        }, lichess.reloadOtherTabs);
      });
      var showBg = function(bg) {
        $body.removeClass('light dark transp')
          .addClass(bg === 'transp' ? 'transp dark' : bg);
          if ((bg === 'dark' || bg === 'transp') && !$('link[href*="dark.css"]').length)
            $('link[href*="common.css"]').clone().each(function() {
              $(this).attr('href', $(this).attr('href').replace(/common\.css/, 'dark.css')).appendTo('head');
            });
            if (bg === 'transp' && !$('link[href*="transp.css"]').length) {
              $('link[href*="common.css"]').clone().each(function() {
                $(this).attr('href', $(this).attr('href').replace(/common\.css/, 'transp.css')).appendTo('head');
              });
              applyBackground($themepicker.find('input.background_image').val());
            }
      };
      var showDimensions = function(is3d) {
        $content.add('#top').removeClass('is2d is3d').addClass(is3d ? 'is3d' : 'is2d');
        if (is3d && !$('link[href*="board-3d.css"]').length)
        $('link[href*="board.css"]').clone().each(function() {
          $(this).attr('href', $(this).attr('href').replace(/board\.css/, 'board-3d.css')).appendTo('head');
        });
        setZoom(currentZoom());
      };
      $themepicker.find('.background a').click(function() {
        background = $(this).data('bg');
        $.post($(this).parent().data('href'), {
          bg: background
        }, function() {
          if (window.Highcharts) lichess.reload();
          lichess.reloadOtherTabs();
        });
        $(this).addClass('active').siblings().removeClass('active');
        return false;
      }).hover(function() {
        showBg($(this).data('bg'));
      }, function() {
        showBg(background);
      }).filter('.' + background).addClass('active');
      $themepicker.find('.dimensions a').click(function() {
        is3d = $(this).data('is3d');
        $.post($(this).parent().data('href'), {
          is3d: is3d
        }, lichess.reloadOtherTabs);
        $(this).addClass('active').siblings().removeClass('active');
        return false;
      }).hover(function() {
        showDimensions($(this).data('is3d'));
      }, function() {
        showDimensions(is3d);
      }).filter('.' + (is3d ? 'd3' : 'd2')).addClass('active');
      lichess.slider().done(function() {
        $themepicker.find('.slider').slider({
          orientation: "horizontal",
          min: 1,
          max: 2,
          range: 'min',
          step: 0.01,
          value: currentZoom(),
          slide: function(e, ui) {
            manuallySetZoom(ui.value);
          }
        });
      });
      $themepicker.find('input.background_image')
        .on('change keyup paste', lichess.fp.debounce(function() {
          var v = $(this).val();
          $.post($(this).data("href"), {
            bgImg: v
          }, lichess.reloadOtherTabs);
          applyBackground(v);
        }, 200));
    }
  });
};
