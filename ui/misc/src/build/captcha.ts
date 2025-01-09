import * as compat from 'shogiops/compat';
import * as util from 'shogiops/variant/util';
import { initOneWithState } from 'common/mini-board';
import * as domData from 'common/data';
import { toBW } from 'shogiops';
import { parseSfen } from 'shogiops/sfen';

const readServerValue = (t: string): string => atob(t.split('').reverse().join(''));

window.lishogi.ready.then(() => {
  setTimeout(() => {
    document.querySelectorAll('div.captcha').forEach((captchaEl: HTMLElement) => {
      const board = captchaEl.querySelector<HTMLElement>('.mini-board')!,
        hint = readServerValue(board.dataset.x!),
        orientation = readServerValue(board.dataset.y!) as Color,
        sfen = readServerValue(board.dataset.z!);
      initOneWithState(board, {
        variant: 'minishogi',
        sfen,
        orientation,
        playable: true,
        noHands: true,
      });
      const sg = domData.get(board, 'shogiground');

      const input = captchaEl.querySelector<HTMLInputElement>('input')!;
      input.value = '';

      const fullSfen = `${sfen} ${toBW(orientation)}`,
        pos = parseSfen('minishogi', fullSfen, false),
        dests = pos.isOk ? compat.shogigroundMoveDests(pos.value) : new Map();

      sg.set({
        activeColor: sg.state.orientation,
        turnColor: sg.state.orientation,
        movable: {
          free: pos.isErr,
          dests,
          color: sg.state.orientation,
          events: {
            after: (orig: Key, dest: Key) => {
              captchaEl.classList.remove('success', 'failure');
              submit(`${orig}${dest}`);
            },
          },
        },
        hands: {
          inlined: false,
        },
      });

      const submit = (solution: string) => {
        input.value = solution;
        window.lishogi.xhr
          .text('GET', captchaEl.dataset.checkUrl!, { url: { solution } })
          .then(data => {
            console.log('CAPTCHA:', data);
            const isSuccess = data == '1';
            captchaEl.classList.add(isSuccess ? 'success' : 'failure');
            if (isSuccess) {
              const key = solution.slice(2, 4),
                piece = sg.state.pieces.get(key),
                sfenStr = `${sg.getBoardSfen()} ${piece.color === 'sente' ? ' w' : ' b'}`,
                pos = parseSfen('minishogi', sfenStr, false);
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
                  ]),
                );
              }
              sg.stop();
            } else
              setTimeout(function () {
                sg.set({
                  sfen: {
                    board: sfen,
                  },
                  turnColor: sg.state.orientation,
                  movable: {
                    dests: dests,
                  },
                });
                sg.setSquareHighlights([{ key: hint, className: 'help' }]);
              }, 300);
          });
      };
    });
  });
});
