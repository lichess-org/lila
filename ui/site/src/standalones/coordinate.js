$(function () {
  $('#trainer').each(function () {
    var $trainer = $(this);
    var $board = $('.coord-trainer__board .sg-wrap');
    var ground;
    var $side = $('.coord-trainer__side');
    var $right = $('.coord-trainer__table');
    var $button = $('.coord-trainer__button');
    var $bar = $trainer.find('.progress_bar');
    var $coords = [$('#next_coord0'), $('#next_coord1')];
    var $start = $button.find('.start');
    var $explanation = $right.find('.explanation');
    var $score = $('.coord-trainer__score');
    var $timer = $('.coord-trainer__timer');
    var scoreUrl = $trainer.data('score-url');
    var duration = 30 * 1000;
    var tickDelay = 50;
    var colorPref = $trainer.data('color-pref');
    var color;
    var startAt, score;
    var wrongTimeout;
    var notationPref = parseInt(document.body.dataset.notation || '0');

    $board.removeClass('preload');
    var showColor = function () {
      color = colorPref == 'random' ? ['sente', 'gote'][Math.round(Math.random())] : colorPref;
      if (!ground)
        ground = Shogiground(
          {
            activeColor: null,
            coordinates: { enabled: false },
            blockTouchScroll: true,
            drawable: { enabled: false },
            movable: {
              free: false,
            },
            orientation: color,
          },
          {
            board: $board[0],
          }
        );
      else if (color !== ground.state.orientation) ground.toggleOrientation();
      $trainer.removeClass('sente gote').addClass(color);
    };
    showColor();

    $trainer.find('form.color').each(function () {
      var $form = $(this);
      $form.find('input').on('change', function () {
        var selected = $form.find('input:checked').val();
        var c = {
          1: 'sente',
          2: 'random',
          3: 'gote',
        }[selected];
        if (c !== colorPref) $.ajax(window.lishogi.formAjax($form));
        colorPref = c;
        showColor();
        return false;
      });
    });

    var showCharts = function () {
      var dark = $('body').hasClass('dark');
      var theme = {
        type: 'line',
        width: '100%',
        height: '80px',
        lineColor: dark ? '#3692e7' : '#1b78d0',
        fillColor: dark ? '#293a49' : '#d1e4f6',
      };
      $side.find('.user_chart').each(function () {
        $(this).sparkline($(this).data('points'), theme);
      });
    };
    showCharts();

    var centerRight = function () {
      $right.css('top', 256 - $right.height() / 2 + 'px');
    };
    centerRight();

    var clearCoords = function () {
      $.each($coords, function (i, e) {
        e.text('');
      });
    };

    var newCoord = function (prevCoord) {
      // disallow the previous coordinate's row or file from being selected
      var files = '123456789';
      var fileIndex = files.indexOf(prevCoord[0]);
      files = files.slice(0, fileIndex) + files.slice(fileIndex + 1, 9);

      var rows = '123456789';
      var rowIndex = rows.indexOf(prevCoord[1]);
      rows = rows.slice(0, rowIndex) + rows.slice(rowIndex + 1, 9);

      return codeCoords(
        files[Math.round(Math.random() * (files.length - 1))] + rows[Math.round(Math.random() * (rows.length - 1))]
      );
    };

    var advanceCoords = function () {
      $('#next_coord0').removeClass('nope');
      var lastElement = $coords.shift();
      $.each($coords, function (i, e) {
        e.attr('id', 'next_coord' + i);
      });
      lastElement.attr('id', 'next_coord' + $coords.length);
      lastElement.text(newCoord($coords[$coords.length - 1].text()));
      $coords.push(lastElement);
    };

    var stop = function () {
      clearCoords();
      $trainer.removeClass('play');
      centerRight();
      $trainer.removeClass('wrong');
      ground.set({
        events: {
          select: false,
        },
      });
      if (scoreUrl)
        $.ajax({
          url: scoreUrl,
          method: 'post',
          data: {
            color: color,
            score: score,
          },
          success: function (charts) {
            $side.find('.scores').html(charts);
            showCharts();
          },
        });
    };

    var tick = function () {
      var spent = Math.min(duration, new Date() - startAt);
      var left = ((duration - spent) / 1000).toFixed(1);
      if (+left < 10) {
        $timer.addClass('hurry');
      }
      $timer.text(left);
      $bar.css('width', (100 * spent) / duration + '%');
      if (spent < duration) setTimeout(tick, tickDelay);
      else stop();
    };

    function codeCoords(key) {
      const rankMap1 = {
        1: '一',
        2: '二',
        3: '三',
        4: '四',
        5: '五',
        6: '六',
        7: '七',
        8: '八',
        9: '九',
      };
      const rankMap2 = {
        1: 'a',
        2: 'b',
        3: 'c',
        4: 'd',
        5: 'e',
        6: 'f',
        7: 'g',
        8: 'h',
        9: 'i',
      };
      switch (notationPref) {
        // 11
        case 0:
        case 1:
          return key;
        // 1一
        case 2:
          return key[0] + rankMap1[key[1]];
        default:
          return key[0] + rankMap2[key[1]];
      }
    }

    $start.click(function () {
      $explanation.remove();
      $trainer.addClass('play').removeClass('init');
      $timer.removeClass('hurry');
      showColor();
      clearCoords();
      centerRight();
      score = 0;
      $score.text(score);
      $bar.css('width', 0);
      setTimeout(function () {
        startAt = new Date();
        ground.set({
          events: {
            select: function (key) {
              var hit = codeCoords(key[0] + (key.charCodeAt(1) - 96).toString()) == $coords[0].text();
              if (hit) {
                score++;
                $score.text(score);
                advanceCoords();
              } else {
                clearTimeout(wrongTimeout);
                $trainer.addClass('wrong');

                wrongTimeout = setTimeout(function () {
                  $trainer.removeClass('wrong');
                }, 500);
              }
            },
          },
        });
        $coords[0].text(newCoord('a1'));
        var i;
        for (i = 1; i < $coords.length; i++) $coords[i].text(newCoord($coords[i - 1].text()));
        tick();
      }, 1000);
    });
  });
});
