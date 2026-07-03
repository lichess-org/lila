import { pgn, opposite } from 'chessops';

import { escapeHtml, frag } from 'lib';

import { env } from './devEnv';
import { type GameCtrl } from './gameCtrl';

export function analyse({ live, clock, white, black }: GameCtrl): void {
  const root = new pgn.Node<pgn.PgnNodeData>();
  let node = root;
  for (const move of live.observe()) {
    const comments = move.clock ? [pgn.makeComment({ clock: move.clock[opposite(move.turn)] })] : [];
    const newNode = new pgn.ChildNode<pgn.PgnNodeData>({ san: move.san, comments });
    node.children.push(newNode);
    node = newNode;
  }
  const game = {
    headers: new Map([
      ['Event', 'Local game'],
      ['Site', 'lichess.org'],
      ['Date', new Date().toISOString().split('T')[0]],
      ['Round', '?'],
      ['White', env.bot.nameOf(white)],
      ['Black', env.bot.nameOf(black)],
      ['Result', live.status.winner ? (live.status.winner === 'white' ? '1-0' : '0-1') : '1/2-1/2'],
      ['TimeControl', clock ? `${clock.initial}+${clock.increment}` : '-'],
    ]),
    moves: root,
  };
  const pgnString = pgn.makePgn(game);
  const formEl = frag<HTMLFormElement>(`<form method="post" action="/import">
    <textarea name="pgn">${escapeHtml(pgnString)}</textarea></form>`);
  document.body.appendChild(formEl);
  formEl.submit();
}
