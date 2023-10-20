import { uciToMove } from 'chessground/util';
import { fenColor } from 'common/mini-board';
import { type Chessground } from 'chessground';
import * as domData from 'common/data';
import clockWidget from './clock-widget';
import StrongSocket from './socket';

export const init = (node: Element, withCg?: typeof Chessground) => {
  const [fen, color, lm] = node.getAttribute('data-state')!.split(','),
    config = {
      coordinates: false,
      viewOnly: true,
      fen,
      orientation: color as Color,
      lastMove: uciToMove(lm),
      drawable: {
        enabled: false,
        visible: false,
      },
    },
    $el = $(node).removeClass('mini-game--init'),
    $cg = $el.find('.cg-wrap'),
    turnColor = fenColor(fen);

  domData.set(
    $cg[0] as Element,
    'chessground',
    (withCg ?? lichess.makeChessground)($cg[0] as HTMLElement, config),
  );

  ['white', 'black'].forEach((color: Color) =>
    $el.find('.mini-game__clock--' + color).each(function (this: HTMLElement) {
      clockWidget(this, {
        time: parseInt(this.getAttribute('data-time')!),
        pause: color != turnColor || !clockIsRunning(fen, color),
      });
    }),
  );
  return node.getAttribute('data-live');
};

const clockIsRunning = (fen: string, color: Color) =>
  color == 'white' ? !fen.includes('PPPPPPPP/RNBQKBNR') : !fen.startsWith('rnbqkbnr/pppppppp');

export const initAll = (parent?: HTMLElement) => {
  const nodes = Array.from((parent || document).getElementsByClassName('mini-game--init')),
    ids = nodes.map(x => init(x)).filter(id => id);
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
    const clock: HTMLElement | null = node.querySelector('.mini-game__clock--' + color);
    // don't interfere with snabbdom clocks
    if (clock && !clock.dataset['managed'])
      $(clock).replaceWith(
        `<span class="mini-game__result">${win ? (win === color[0] ? 1 : 0) : '½'}</span>`,
      );
  });
