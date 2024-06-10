var sfen = require('shogiops/sfen');
var util = require('shogiops/variant/util');

$(function () {
  lishogi.requestIdleCallback(function () {
    $('div.captcha').each(function () {
      var $captcha = $(this);
      var $board = $captcha.find('.mini-board');
      var $input = $captcha.find('input').val('');
      var sg = $board.data('shogiground');
      var destsJson = JSON.parse(lishogi.readServerSfen($board.data('x')));
      var dests = new Map();
      for (var k in destsJson) dests.set(k, destsJson[k].match(/.{2}/g));
      sg.set({
        activeColor: sg.state.orientation,
        turnColor: sg.state.orientation,
        movable: {
          free: false,
          dests: dests,
          color: sg.state.orientation,
          events: {
            after: function (orig, dest) {
              $captcha.removeClass('success failure');
              submit(orig + ' ' + dest);
            },
          },
        },
      });

      var submit = function (solution) {
        $input.val(solution);
        $.ajax({
          url: $captcha.data('check-url'),
          data: {
            solution: solution,
          },
          success: function (data) {
            $captcha.toggleClass('success', data == 1);
            $captcha.toggleClass('failure', data != 1);
            if (data == 1) {
              const key = solution.slice(3, 5);
              const piece = sg.state.pieces.get(key);
              const sfenStr = sg.getBoardSfen() + (piece.color === 'sente' ? ' w' : ' b');
              const pos = sfen.parseSfen('standard', sfenStr, false);
              if (pos.isOk && !pos.value.isCheckmate()) {
                sg.setPieces(
                  new Map([
                    [
                      key,
                      {
                        color: piece.color,
                        role: util.promote('standard')(piece.role),
                        promoted: true,
                      },
                    ],
                  ])
                );
              }
              $board.data('shogiground').stop();
            } else
              setTimeout(function () {
                lishogi.parseSfen($board);
                $board.data('shogiground').set({
                  turnColor: sg.state.orientation,
                  movable: {
                    dests: dests,
                  },
                });
              }, 300);
          },
        });
      };
    });
  });
});
