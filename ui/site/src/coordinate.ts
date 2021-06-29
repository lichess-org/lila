import * as xhr from 'common/xhr';
import sparkline from '@fnando/sparkline';
import throttle from 'common/throttle';
import resizeHandle from 'common/resize';
import * as cg from 'chessground/types';

lichess.load.then(() => {
  $('#trainer').each(function (this: HTMLElement) {
    const $trainer = $(this);
    const $board = $('.coord-trainer__board .cg-wrap');
    const $coordsSvg = $('.coords-svg');
    const $coords = $coordsSvg.find('.coord');
    let ground;
    const $side = $('.coord-trainer__side');
    const $right = $('.coord-trainer__table');
    const $bar = $trainer.find('.progress_bar');
    const $start = $right.find('.start');
    const $explanation = $right.find('.explanation');
    const $score = $('.coord-trainer__score');
    const $timer = $('.coord-trainer__timer');
    const scoreUrl = $trainer.data('score-url');
    const duration = 30 * 1000;
    const tickDelay = 50;
    const resizePref = $trainer.data('resize-pref');
    let colorPref = $trainer.data('color-pref');
    let color;
    let startAt, score;
    let wrongTimeout;
    let ply = 0;

    const showColor = function () {
      color = colorPref == 'random' ? ['white', 'black'][Math.round(Math.random())] : colorPref;
      if (!ground)
        ground = window.Chessground($board[0], {
          coordinates: false,
          drawable: { enabled: false },
          movable: {
            free: false,
            color: null,
          },
          orientation: color,
          addPieceZIndex: $('#main-wrap').hasClass('is3d'),
          events: {
            insert(elements: cg.Elements) {
              resizeHandle(elements, resizePref, ply);
            },
          },
        });
      else if (color !== ground.state.orientation) ground.toggleOrientation();
      $trainer.removeClass('white black').addClass(color);
    };
    showColor();

    $trainer.find('form.color').each(function (this: HTMLFormElement) {
      const form = this,
        $form = $(this);
      $form.find('input').on('change', function () {
        const selected = $form.find('input:checked').val() as string;
        const c = {
          1: 'white',
          2: 'random',
          3: 'black',
        }[selected];
        if (c !== colorPref) xhr.formToXhr(form);
        colorPref = c;
        showColor();
        return false;
      });
    });

    const setZen = throttle(1000, zen =>
      xhr.text('/pref/zen', {
        method: 'post',
        body: xhr.form({ zen: zen ? 1 : 0 }),
      })
    );

    lichess.pubsub.on('zen', () => {
      const zen = $('body').toggleClass('zen').hasClass('zen');
      window.dispatchEvent(new Event('resize'));
      setZen(zen);
      requestAnimationFrame(showCharts);
    });

    window.Mousetrap.bind('z', () => lichess.pubsub.emit('zen'));

    $('#zentog').on('click', () => lichess.pubsub.emit('zen'));

    function showCharts() {
      $side.find('.user_chart').each(function (this: HTMLElement) {
        const $svg = $('<svg class="sparkline" height="80px" stroke-width="3">')
          .attr('width', $(this).width() + 'px')
          .prependTo($(this).empty());
        sparkline($svg[0] as unknown as SVGSVGElement, $(this).data('points'), {
          interactive: true,
          /* onmousemove(event, datapoint) { */
          /*   var svg = findClosest(event.target, "svg"); */
          /*   var tooltip = svg.nextElementSibling; */
          /*   var date = new Date(datapoint.date).toUTCString().replace(/^.*?, (.*?) \d{2}:\d{2}:\d{2}.*?$/, "$1"); */

          /*   tooltip.hidden = false; */
          /*   tooltip.textContent = `${date}: $${datapoint.value.toFixed(2)} USD`; */
          /*   tooltip.style.top = `${event.offsetY}px`; */
          /*   tooltip.style.left = `${event.offsetX + 20}px`; */
          /* }, */

          /* onmouseout() { */
          /*   var svg = findClosest(event.target, "svg"); */
          /*   var tooltip = svg.nextElementSibling; */

          /*   tooltip.hidden = true; */
          /* } */
          /* }; */
        });
      });
    }
    requestAnimationFrame(showCharts);

    const centerRight = function () {
      $right.css('top', 256 - $right.height() / 2 + 'px');
    };
    centerRight();

    const clearCoords = function () {
      $coords.text('');
    };

    const newCoord = function (prevCoord) {
      // disallow the previous coordinate's row or file from being selected
      let files = 'abcdefgh';
      const fileIndex = files.indexOf(prevCoord[0]);
      files = files.slice(0, fileIndex) + files.slice(fileIndex + 1, 8);

      let rows = '12345678';
      const rowIndex = rows.indexOf(prevCoord[1]);
      rows = rows.slice(0, rowIndex) + rows.slice(rowIndex + 1, 8);

      return (
        files[Math.round(Math.random() * (files.length - 1))] + rows[Math.round(Math.random() * (rows.length - 1))]
      );
    };

    const resolvedCoordEl = () => $coordsSvg.find('.coord--resolved');
    const currentCoordEl = () => $coordsSvg.find('.coord--current');
    const nextCoordEl = () => $coordsSvg.find('.coord--next');
    const newCoordEl = () => $coordsSvg.find('.coord--new');

    const advanceCoords = function () {
      const resolved = resolvedCoordEl().remove();
      currentCoordEl().removeClass('coord--current').addClass('coord--resolved');
      nextCoordEl().removeClass('coord--next').addClass('coord--current');
      newCoordEl().text(newCoord(currentCoordEl().text())).removeClass('coord--new').addClass('coord--next');
      resolved.text('').removeClass('coord--resolved').addClass('coord--new').appendTo($coordsSvg);
    };

    const stop = function () {
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
        xhr
          .text(scoreUrl, {
            method: 'post',
            body: xhr.form({ color, score }),
          })
          .then(charts => {
            $side.find('.scores').html(charts);
            showCharts();
          });
    };

    const tick = function () {
      const spent = Math.min(duration, new Date().getTime() - startAt);
      const left = ((duration - spent) / 1000).toFixed(1);
      if (+left < 10) {
        $timer.addClass('hurry');
      }

      $timer.text(left);
      $bar.css('width', (100 * spent) / duration + '%');
      if (spent < duration) setTimeout(tick, tickDelay);
      else stop();
    };

    $start.on('click', () => {
      $explanation.remove();
      $trainer.addClass('play').removeClass('init');
      $timer.removeClass('hurry');
      ply = 2;
      ground.redrawAll();
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
            select(key) {
              const hit = key == currentCoordEl().text();
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

        const initialCoordValue = newCoord('a1');
        currentCoordEl().text(initialCoordValue);
        nextCoordEl().text(newCoord(initialCoordValue));
        tick();
      }, 1000);
    });
  });
});
