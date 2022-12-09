import * as xhr from 'common/xhr';
import * as domData from 'common/data';
import type { Api as ChessgroundApi } from 'chessground/api';
import type { Key } from 'chessground/types';

function init() {
  let failed = false;

  $('div.captcha').each(function (this: HTMLElement) {
    if (this.dataset.initialized) return;

    const $captcha = $(this),
      $board = $captcha.find('.mini-board'),
      $input = $captcha.find('input').val(''),
      cg = domData.get($board[0]!, 'chessground') as ChessgroundApi;

    if (!cg) {
      failed = true;
      return;
    }

    $board.on('touchstart', () => {
      const el = document.activeElement as HTMLElement;
      if (el && 'blur' in el) el.blur();
    });

    const fen = cg.getFen(),
      destsObj = $board.data('moves'),
      dests = new Map();
    for (const k in destsObj) dests.set(k, destsObj[k].match(/.{2}/g));
    cg.set({
      turnColor: cg.state.orientation,
      movable: {
        free: false,
        dests,
        color: cg.state.orientation,
        events: {
          after(orig: Key, dest: Key) {
            const piece = cg.state.pieces.get(dest);
            if (piece?.role === 'pawn' && (dest[1] === '8' || dest[1] === '1')) {
              cg.setPieces(
                new Map([
                  [
                    dest,
                    {
                      role: 'queen',
                      color: piece.color,
                      promoted: true,
                    },
                  ],
                ])
              );
            }
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

    this.dataset.initialized = '1';
  });

  if (failed) setTimeout(init, 1000);
}

lichess.load.then(() => setTimeout(init, 1000));
