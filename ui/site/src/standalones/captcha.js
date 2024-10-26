var sfen = require('shogiops/sfen');
var compat = require('shogiops/compat');
var util = require('shogiops/variant/util');

$(function () {
  lishogi.requestIdleCallback(function () {
    $('div.captcha').each(function () {
      var $captcha = $(this);
      var $board = $captcha.find('.mini-board');
      var $input = $captcha.find('input').val('');
      var sg = $board.data('shogiground');
      var hint = lishogi.readServerSfen($board.data('x'));
      var sfenString = sg.getBoardSfen() + (sg.state.orientation === 'sente' ? ' b' : ' w');
      var pos = sfen.parseSfen('minishogi', sfenString);
      var dests = pos.isOk ? compat.shogigroundMoveDests(pos.value) : new Map();
      sg.set({
        activeColor: sg.state.orientation,
        turnColor: sg.state.orientation,
        movable: {
          free: pos.isErr,
          dests,
          color: sg.state.orientation,
          events: {
            after: function (orig, dest) {
              $captcha.removeClass('success failure');
              submit(orig + dest);
            },
          },
        },
        hands: {
          inlined: false,
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
              const key = solution.slice(2, 4);
              const piece = sg.state.pieces.get(key);
              const sfenStr = sg.getBoardSfen() + (piece.color === 'sente' ? ' w' : ' b');
              const pos = sfen.parseSfen('minishogi', sfenStr, false);
              if (pos.isOk && !pos.value.isCheckmate()) {
                sg.setPieces(
                  new Map([
                    [
                      key,
                      {
                        color: piece.color,
                        role: util.promote('minishogi')(piece.role),
                        promoted: true,
                      },
                    ],
                  ])
                );
              }
              sg.stop();
            } else
              setTimeout(function () {
                lishogi.parseSfen($board);
                sg.set({
                  turnColor: sg.state.orientation,
                  movable: {
                    dests: dests,
                  },
                });
                sg.setSquareHighlights([{ key: hint, className: 'help' }]);
              }, 300);
          },
        });
      };
    });
  });
});
