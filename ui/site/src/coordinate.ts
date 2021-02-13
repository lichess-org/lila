import * as xhr from "common/xhr";

window.lichess.load.then(() => {
  $('#trainer').each(function(this: HTMLElement) {
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
      if (!ground) ground = window.Chessground($board[0], {
        coordinates: false,
        drawable: { enabled: false },
        movable: {
          free: false,
          color: null
        },
        orientation: color,
        addPieceZIndex: $('#main-wrap').hasClass('is3d')
      });
      else if (color !== ground.state.orientation) ground.toggleOrientation();
      $trainer.removeClass('white black').addClass(color);
    };
    showColor();

    $trainer.find('form.color').each(function(this: HTMLFormElement) {
      const form = this, $form = $(this);
      $form.find('input').on('change', function() {
        var selected = $form.find('input:checked').val();
        var c = {
          1: 'white',
          2: 'random',
          3: 'black'
        }[selected];
        if (c !== colorPref) xhr.formToXhr(form);
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
      $side.find('.user_chart').each(function(this: HTMLElement) {
        $(this).sparkline($(this).data('points'), theme);
      });
    };
    showCharts();

    var centerRight = function() {
      $right.css('top', (256 - $right.height() / 2) + 'px');
    };
    centerRight();

    var clearCoords = function() {
      $.each($coords, function(_, e) {
        e.text('');
      });
    };

    var newCoord = function(prevCoord) {
      // disallow the previous coordinate's row or file from being selected
      var files = 'abcdefgh';
      var fileIndex = files.indexOf(prevCoord[0]);
      files = files.slice(0, fileIndex) + files.slice(fileIndex + 1, 8);

      var rows = '12345678';
      var rowIndex = rows.indexOf(prevCoord[1]);
      rows = rows.slice(0, rowIndex) + rows.slice(rowIndex + 1, 8);

      return files[Math.round(Math.random() * (files.length - 1))] + rows[Math.round(Math.random() * (rows.length - 1))];
    };

    var advanceCoords = function() {
      $('#next_coord0').removeClass('nope');
      var lastElement = $coords.shift()!;
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
      if (scoreUrl) xhr.text(scoreUrl, {
        method: 'post',
        body: xhr.form({ color, score })
      }).then(charts => {
        $side.find('.scores').html(charts);
        showCharts();
      });
    };

    var tick = function() {
      var spent = Math.min(duration, (new Date().getTime() - startAt));
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
          events: {
            select: function(key) {
              var hit = key == $coords[0].text();
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
        $coords[0].text(newCoord('a1'));
        var i;
        for (i = 1; i < $coords.length; i++)
          $coords[i].text(newCoord($coords[i - 1].text()));
        tick();
      }, 1000);
    });
  });

});
