import { type GameCtrl } from './gameCtrl';
import * as co from 'chessops';
import { escapeHtml, frag } from 'common';

export function analyse(gameCtrl: GameCtrl): void {
  const local = gameCtrl.live;
  const root = new co.pgn.Node<co.pgn.PgnNodeData>();
  let node = root;
  for (const move of gameCtrl.live.observe()) {
    const comments = move.clock ? [co.pgn.makeComment({ clock: move.clock[move.turn] })] : [];
    const newNode = new co.pgn.ChildNode<co.pgn.PgnNodeData>({ san: move.san, comments });
    node.children.push(newNode);
    node = newNode;
  }
  const game = {
    headers: new Map<string, string>([
      ['Event', 'Local game'],
      ['Site', 'lichess.org'],
      ['Date', new Date().toISOString().split('T')[0]],
      ['Round', '1'],
      ['White', 'Player'],
      ['Black', 'Opponent'],
      ['Result', local.status.winner ? (local.status.winner === 'white' ? '1-0' : '0-1') : '1/2-1/2'],
      ['TimeControl', gameCtrl.clock ? `${gameCtrl.clock.initial}+${gameCtrl.clock.increment}` : 'Unlimited'],
    ]),
    moves: root,
  };
  const pgn = co.pgn.makePgn(game);
  console.log(pgn);
  const formEl = frag<HTMLFormElement>(`<form method="post" action="/import">
    <textarea name="pgn">${escapeHtml(pgn)}</textarea></form>`);
  document.body.appendChild(formEl);
  formEl.submit();
}
