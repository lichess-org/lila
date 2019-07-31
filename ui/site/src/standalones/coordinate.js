$(function() {
  $('#trainer').each(function() {
    var $trainer = $(this);
    var $board = $('.coord-trainer__board .cg-wrap');
    var ground;
    var $side = $('.coord-trainer__side');
    var $right = $('.coord-trainer__table');
    var $bar = $trainer.find('.progress_bar');
    var $coords = [
      $('#next_coord0'),
      $('#next_coord1'),
      $('#next_coord2')
    ];
    var $start = $right.find('.start');
    var $explanation = $right.find('.explanation');
    var $score = $('.coord-trainer__score');
    var scoreUrl = $trainer.data('score-url');
    var duration = 30 * 1000;
    var tickDelay = 50;
    var colorPref = $trainer.data('color-pref');
    var color;
    var startAt, score;

    var showColor = function() {
      color = colorPref == 'random' ? ['white', 'black'][Math.round(Math.random())] : colorPref;
      if (!ground) ground = Draughtsground($board[0], {
        fen: 'W:WG31,G32,G33,G34,G35,G36,G37,G38,G39,G40,G41,G42,G43,G44,G45,G46,G47,G48,G49,G50:BG1,G2,G3,G4,G5,G6,G7,G8,G9,G10,G11,G12,G13,G14,G15,G16,G17,G18,G19,G20',
        coordinates: 1,
        drawable: { enabled: false },
        movable: {
          free: false,
          color: null
        },
        orientation: color
      });
      else if (color !== ground.state.orientation) ground.toggleOrientation();
      $trainer.removeClass('white black').addClass(color);
    };
    showColor();

    $trainer.find('form.color').each(function() {
      var $form = $(this);
      $form.find('input').on('change', function() {
        var selected = $form.find('input:checked').val();
        var c = {
          1: 'white',
          2: 'random',
          3: 'black'
        }[selected];
        if (c !== colorPref) $.ajax(window.lidraughts.formAjax($form));
        colorPref = c;
        showColor();
        return false;
      });
    });

    var showCharts = function() {
      var dark = $('body').hasClass('dark');
      var theme = {
        type: 'line',
        width: '100%',
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

    var clearCoords = function() {
      $.each($coords, function(i, e) {
        e.text('');
      });
    };

    var newCoord = function(prevCoord) {
      // disallow the previous coordinate from being selected
      var coord = 1 + Math.floor(Math.random() * 50)
      while (coord == prevCoord)
        coord = 1 + Math.floor(Math.random() * 50)
      return coord;
    };

    var advanceCoords = function() {
      $('#next_coord0').removeClass('nope');
      var lastElement = $coords.shift();
      $.each($coords, function(i, e) {
        e.attr('id', 'next_coord' + i);
      });
      lastElement.attr('id', 'next_coord' + ($coords.length));
      lastElement.text(newCoord($coords[$coords.length - 1].text()));
      $coords.push(lastElement);
    };

    var stop = function() {
      clearCoords();
      $trainer.removeClass('play');
      centerRight();
      $trainer.removeClass('wrong');
      ground.set({
        events: {
          select: false
        }
      });
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
      $explanation.remove();
      $trainer.addClass('play').removeClass('init');
      showColor();
      clearCoords();
      centerRight();
      score = 0;
      $score.text(score);
      $bar.css('width', 0);
      setTimeout(function() {

        startAt = new Date();
        ground.set({
          fen: 'W:W31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50:B1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20:H0:F1',
          coordinates: 0,
          events: {
            select: function(key) {
              var hit = (key == $coords[0].text() || key == "0" + $coords[0].text());
              if (hit) {
                score++;
                $score.text(score);
                advanceCoords();
              } else {
                $('#next_coord0').addClass('nope');
                setTimeout(function() {
                  $('#next_coord0').removeClass('nope');
                }, 500);
              }
              $trainer.toggleClass('wrong', !hit);
            }
          }
        });
        ground.redrawAll();
        $coords[0].text(newCoord('1'));
        var i;
        for (i = 1; i < $coords.length; i++)
          $coords[i].text(newCoord($coords[i - 1].text()));
        tick();
      }, 1000);
    });
  });

});
