import * as xhr from 'common/xhr';
import * as domData from 'common/data';

lichess.load.then(() => {
  setTimeout(() => {
    $('div.captcha').each(function (this: HTMLElement) {
      const $captcha = $(this),
        $board = $captcha.find('.mini-board'),
        $input = $captcha.find('input').val(''),
        cg = domData.get($board[0]!, 'chessground'),
        fen = cg.getFen(),
        destsObj = $board.data('moves'),
        dests = new Map();
      for (let k in destsObj) dests.set(k, destsObj[k].match(/.{2}/g));
      cg.set({
        turnColor: cg.state.orientation,
        movable: {
          free: false,
          dests,
          color: cg.state.orientation,
          events: {
            after(orig: string, dest: string) {
              $captcha.removeClass('success failure');
              submit(orig + ' ' + dest);
            },
          },
        },
      });

      const submit = function (solution: string) {
        $input.val(solution);
        xhr.text(xhr.url($captcha.data('check-url'), { solution })).then(data => {
          $captcha.toggleClass('success', data == '1').toggleClass('failure', data != '1');
          if (data == '1') domData.get($board[0]!, 'chessground').stop();
          else
            setTimeout(
              () =>
                cg.set({
                  fen: fen,
                  turnColor: cg.state.orientation,
                  movable: { dests },
                }),
              300
            );
        });
      };
    });
  }, 1000);
});
