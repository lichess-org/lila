$(function() {

  var $startButtons = $('#start_buttons');
  if (!$startButtons.length) {
    return;
  }

  if (!$.websocket.available) {
    $('#start_buttons a').attr('href', '#');
    return;
  }

  function prepareForm() {
    var $form = $('div.lichess_overboard');
    $form.find('div.buttons').buttonset().disableSelection();
    $form.find('button.submit').button().disableSelection();
    $form.find('.time_choice input, .increment_choice input').each(function() {
      var $input = $(this), $value = $input.parent().find('span');
      $input.hide().after($('<div>').slider({
        value: $input.val(),
        min: $input.data('min'),
        max: $input.data('max'),
        range: 'min',
        step: 1,
        slide: function( event, ui ) {
          $value.text(ui.value);
          $input.attr('value', ui.value);
          $form.find('.color_submits button').toggle(
            $form.find('.time_choice input').val() > 0 || $form.find('.increment_choice input').val() > 0
            );
        }
      }));
    });
    $form.find('.elo_range').each(function() {
      var $this = $(this);
      var $input = $this.find("input");
      var $span = $this.parent().find("span.range");
      var min = $input.data("min");
      var max = $input.data("max");
      if ($input.val()) {
        var values = $input.val().split("-");
      } else {
        var values = [min, max];
      }
      $span.text(values.join(' - '));
      $this.slider({
        range: true,
        min: min,
        max: max,
        values: values,
        step: 50,
        slide: function( event, ui ) {
          $input.val(ui.values[0] + "-" + ui.values[1]);
          $span.text(ui.values[0] + " - " + ui.values[1]);
        }
      });
      var $eloRangeConfig = $this.parent();
      var $modeChoices = $form.find('.mode_choice input');
      $modeChoices.on('change', function() {
        $eloRangeConfig.toggle($modeChoices.eq(1).attr('checked') == 'checked');
        $.centerOverboard();
      }).trigger('change');
    });
    $form.find('.clock_choice input').on('change', function() {
      $form.find('.time_choice, .increment_choice').toggle($(this).is(':checked'));
      $.centerOverboard();
    }).trigger('change');
    var $eloRangeConfig = $form.find('.elo_range_config');
    $form.prepend($('<a class="close"></a>').click(function() {
      $form.remove();
      $startButtons.find('a.active').removeClass('active');
    }));
  }

  $startButtons.find('a').click(function() {
    $startButtons.find('a.active').removeClass('active');
    $(this).addClass('active');
    $('div.lichess_overboard').remove();
    $.ajax({
      url: $(this).attr('href'),
      success: function(html) {
        $('div.lichess_overboard').remove();
        $('div.lichess_board_wrap').prepend(html);
        prepareForm();
        $.centerOverboard();
      }
    });
    return false;
  });
  $('#lichess').on('submit', 'form', $.lichessOpeningPreventClicks);

  if (window.location.hash) {
    $startButtons.find('a.config_'+window.location.hash.replace(/#/, '')).click();
  }
});

$.lichessOpeningPreventClicks = function() {
  $('div.lichess_overboard, div.hooks_wrap').hide();
};
