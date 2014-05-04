$(function() {
  $('#trainer').each(function() {
    var $trainer = $(this);
    var $board = $trainer.find('.lichess_board');
    var $side = $trainer.find('> .side');
    var $right = $trainer.find('> .right');
    var $bar = $trainer.find('.progress_bar');
    var $coord = $right.find('.coord');
    var $start = $right.find('.start');
    var $scoreCont = $trainer.find('.score_container');
    var $score = $scoreCont.find('strong');
    var $colorForm = $trainer.find('form.color');
    var scoreUrl = $trainer.data('score-url');
    var colorUrl = $trainer.data('color-url');
    var duration = 30 * 1000;
    var tickDelay = 50;
    var colorPref = $trainer.data('color-pref');
    var color;
    var coordToGuess, startAt, score;

    var showColor = function() {
      color = colorPref == 'random' ? ['white', 'black'][_.random(0, 1)] : colorPref;
      $trainer.removeClass('white black').addClass(color);
    };
    showColor();

    $colorForm.buttonset().disableSelection().on('click', 'button', function() {
      colorPref = {
        1: 'white',
        2: 'random',
        3: 'black'
      }[$(this).val()];
      if (colorUrl) $.ajax({
        url: colorUrl,
        method: 'post',
        data: {
          color: $(this).val()
        }
      });
      $(this).siblings()
        .removeClass('ui-state-active').attr('disabled', false).end()
        .addClass('ui-state-active').attr('disabled', true)
        .parent().buttonset();
      showColor();
      return false;
    });

    var showCharts = function() {
      var dark = $('body').hasClass('dark');
      var theme = {
        type: 'line',
        width: '213px',
        height: '80px',
        lineColor: dark ? '#4444ff' : '#0000ff',
        fillColor: dark ? '#222255' : '#ccccff'
      };
      $side.find('.user_chart').each(function() {
        $(this).sparkline($(this).data('points'), theme);
      });
    };
    showCharts();

    var centerRight = function() {
      $right.css('top', (256 - $right.height() / 2) + 'px');
    };
    centerRight();

    var newCoord = function() {
      var c;
      do {
        c = 'abcdefgh' [_.random(0, 7)] + _.random(1, 8);
      } while (c == coordToGuess);
      coordToGuess = c;
      $coord.text(coordToGuess);
    };

    var stop = function() {
      $trainer.removeClass('play');
      centerRight();
      $trainer.removeClass('wrong');
      $board.off('click', '.lcs');
      if (scoreUrl) $.ajax({
        url: scoreUrl,
        method: 'post',
        data: {
          color: color,
          score: score
        },
        success: function(charts) {
          $side.find('.scores').html(charts);
          showCharts();
        }
      });
    };

    var tick = function() {
      var spent = Math.min(duration, (new Date() - startAt));
      $bar.css('width', (100 * spent / duration) + '%');
      if (spent < duration) setTimeout(tick, tickDelay);
      else stop();
    };

    $start.click(function() {
      $trainer.addClass('play');
      showColor();
      $coord.text('--');
      centerRight();
      score = 0;
      $score.text(score);
      $scoreCont.show();
      $bar.css('width', 0);
      setTimeout(function() {
        startAt = new Date();
        $board.on('click', '.lcs', function() {
          var hit = this.id == coordToGuess;
          if (hit) {
            score++;
            $score.text(score);
          }
          $trainer.toggleClass('wrong', !hit);
          newCoord();
        });
        newCoord();
        tick();
      }, 1000);
    });
  });
});
