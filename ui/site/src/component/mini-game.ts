import { uciToMove } from 'chessground/util';
import { fenColor } from 'common/mini-game';
import * as domData from 'common/data';
import clockWidget from './clock-widget';
import StrongSocket from './socket';

export const init = (node: HTMLElement) => {
  if (!window.Chessground) setTimeout(() => init(node), 200);
  else {
    const [fen, orientation, lm] = node.getAttribute('data-state')!.split(','),
      config = {
        coordinates: false,
        viewOnly: true,
        fen,
        orientation,
        lastMove: uciToMove(lm),
        drawable: {
          enabled: false,
          visible: false,
        },
      },
      $el = $(node).removeClass('mini-game--init'),
      $cg = $el.find('.cg-wrap'),
      turnColor = fenColor(fen);
    domData.set($cg[0] as HTMLElement, 'chessground', window.Chessground($cg[0], config));
    ['white', 'black'].forEach((color: Color) =>
      $el.find('.mini-game__clock--' + color).each(function (this: HTMLElement) {
        clockWidget(this, {
          time: parseInt(this.getAttribute('data-time')!),
          pause: color != turnColor || !clockIsRunning(fen, color),
        });
      })
    );
  }
  return node.getAttribute('data-live');
};

const clockIsRunning = (fen: string, color: Color) =>
  color == 'white' ? !fen.includes('PPPPPPPP/RNBQKBNR') : !fen.startsWith('rnbqkbnr/pppppppp');

export const initAll = (parent?: HTMLElement) => {
  const nodes = Array.from((parent || document).getElementsByClassName('mini-game--init')),
    ids = nodes.map(init).filter(id => id);
  if (ids.length) StrongSocket.firstConnect.then(send => send('startWatching', ids.join(' ')));
};

export const update = (node: HTMLElement, data: MiniGameUpdateData) => {
  const $el = $(node),
    lm = data.lm,
    cg = domData.get(node.querySelector('.cg-wrap')!, 'chessground');
  if (cg)
    cg.set({
      fen: data.fen,
      lastMove: uciToMove(lm),
    });
  const turnColor = fenColor(data.fen);
  const updateClock = (time: number | undefined, color: Color) => {
    if (!isNaN(time!))
      clockWidget($el[0]?.querySelector('.mini-game__clock--' + color) as HTMLElement, {
        time: time!,
        pause: color != turnColor || !clockIsRunning(data.fen, color),
      });
  };
  updateClock(data.wc, 'white');
  updateClock(data.bc, 'black');
};

export const finish = (node: HTMLElement, win?: 'black' | 'white') =>
  ['white', 'black'].forEach(color => {
    const $clock = $(node).find('.mini-game__clock--' + color);
    // don't interfere with snabbdom clocks
    if (!$clock.data('managed'))
      $clock.replaceWith(`<span class="mini-game__result">${win ? (win === color[0] ? 1 : 0) : '½'}</span>`);
  });
