import { h, VNode } from 'snabbdom';
import * as domData from './data';
import { lichessClockIsRunning, setClockWidget } from './clock';
import { uciToMove } from 'chessground/util';
import { Chessground as makeChessground } from 'chessground';
import { pubsub } from './pubsub';

export const initMiniBoard = (node: HTMLElement): void => {
  const [fen, orientation, lm] = node.getAttribute('data-state')!.split(',');
  initMiniBoardWith(node, fen, orientation as Color, lm);
};

export const initMiniBoardWith = (node: HTMLElement, fen: string, orientation: Color, lm?: string): void => {
  domData.set(
    node,
    'chessground',
    makeChessground(node, {
      orientation,
      coordinates: false,
      viewOnly: !node.getAttribute('data-playable'),
      fen,
      lastMove: uciToMove(lm),
      drawable: {
        enabled: false,
        visible: false,
      },
    }),
  );
};

export const initMiniBoards = (parent?: HTMLElement): void =>
  Array.from((parent || document).getElementsByClassName('mini-board--init')).forEach((el: HTMLElement) => {
    el.classList.remove('mini-board--init');
    initMiniBoard(el);
  });

export const fenColor = (fen: string): Color => (fen.includes(' w') ? 'white' : 'black');

export const renderClock = (color: Color, time: number): VNode =>
  h(`span.mini-game__clock.mini-game__clock--${color}`, {
    attrs: { 'data-time': time, 'data-managed': 1 },
  });

export const initMiniGame = (node: Element, withCg?: typeof makeChessground): string | null => {
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
    (withCg ?? makeChessground)($cg[0] as HTMLElement, config),
  );

  ['white', 'black'].forEach((color: Color) =>
    $el.find('.mini-game__clock--' + color).each(function(this: HTMLElement) {
      setClockWidget(this, {
        time: parseInt(this.getAttribute('data-time')!),
        pause: color != turnColor || !lichessClockIsRunning(fen, color),
      });
    }),
  );
  return node.getAttribute('data-live');
};

export const initMiniGames = (parent?: HTMLElement): void => {
  const nodes = Array.from((parent || document).getElementsByClassName('mini-game--init')),
    ids = nodes.map(x => initMiniGame(x)).filter(id => id);
  if (ids.length)
    pubsub.after('socket.hasConnected').then(() => site.socket.send('startWatching', ids.join(' ')));
};

export const updateMiniGame = (node: HTMLElement, data: MiniGameUpdateData): void => {
  const lm = data.lm,
    cg = domData.get(node.querySelector('.cg-wrap')!, 'chessground');
  if (cg)
    cg.set({
      fen: data.fen,
      lastMove: uciToMove(lm),
    });
  const turnColor = fenColor(data.fen);
  const updateClock = (time: number | undefined, color: Color) => {
    const clockEl = node?.querySelector('.mini-game__clock--' + color) as HTMLElement;
    if (clockEl && !isNaN(time!))
      setClockWidget(clockEl, {
        time: time!,
        pause: color != turnColor || !lichessClockIsRunning(data.fen, color),
      });
  };
  updateClock(data.wc, 'white');
  updateClock(data.bc, 'black');
};

export const finishMiniGame = (node: HTMLElement, win?: 'black' | 'white'): void =>
  ['white', 'black'].forEach(color => {
    const clock: HTMLElement | null = node.querySelector('.mini-game__clock--' + color);
    // don't interfere with snabbdom clocks
    if (clock && !clock.dataset['managed'])
      $(clock).replaceWith(
        `<span class="mini-game__result">${win ? (win === color[0] ? 1 : 0) : '½'}</span>`,
      );
  });

interface MiniGameUpdateData {
  fen: Cg.FEN;
  lm: Uci;
  wc?: number;
  bc?: number;
}
