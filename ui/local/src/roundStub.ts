import type { GameCtrl } from './gameCtrl';
import type { LocalStub } from 'round';
import { replayable } from 'game';
import * as licon from 'common/licon';
import { looseH as h, bind } from 'common/snabbdom';
import { escapeHtml } from 'common';
import * as co from 'chessops';

export function makeStub(gameCtrl: GameCtrl): LocalStub {
  const handlers: SocketHandlers = {
    move: (d: any) => gameCtrl.move(d.u),
    'blindfold-no': () => {},
    'blindfold-yes': () => {},
    'rematch-yes': () => {
      gameCtrl.reset();
    },
    resign: () => gameCtrl.resign(),
    'draw-yes': () => gameCtrl.draw(),
  };
  const send = (t: string, d?: any) => {
    if (handlers[t]) handlers[t]?.(d);
    else console.log('send: no handler for', t, d);
  };
  return {
    send,
    handlers,
    analyseButton: (isIcon: boolean) => {
      return (
        replayable(gameCtrl.roundData) &&
        h(
          isIcon ? 'button.button-none.fbt.analysis' : 'button.fbt',
          {
            attrs: isIcon ? { title: gameCtrl.round.noarg('analysis'), 'data-icon': licon.Microscope } : {},
            hook: bind('click', () => analyse(gameCtrl)),
          },
          !isIcon && gameCtrl.round.noarg('analysis'),
        )
      );
    },
    moreTime: () => {}, //throttle(300, () => send('moretime')),
    outoftime: () => gameCtrl.flag(),
    berserk: () => {},
    sendLoading(typ: string, data?: any) {
      send(typ, data);
    },
    receive: (typ: string, data: any) => {
      if (handlers[typ]) handlers[typ]?.(data);
      else console.log('recv: no handler for', typ, data);
      return true;
    },
    reload: site.reload,
  };
}

function analyse(gameCtrl: GameCtrl) {
  const local = gameCtrl.game;
  const root = new co.pgn.Node<co.pgn.PgnNodeData>();
  const chess = co.Chess.fromSetup(co.fen.parseFen(local.initialFen).unwrap()).unwrap();
  let node = root;
  for (const move of local.moves) {
    const comments = move.clock ? [co.pgn.makeComment({ clock: move.clock[chess.turn] })] : [];
    const san = co.san.makeSanAndPlay(chess, co.parseUci(move.uci)!);
    const newNode = new co.pgn.ChildNode<co.pgn.PgnNodeData>({ san, comments });
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
  const formEl = $as<HTMLFormElement>(`<form method="post" action="/import">
    <textarea name="pgn">${escapeHtml(pgn)}</textarea></form>`);
  document.body.appendChild(formEl);
  formEl.submit();
}
/*
  const handlers: SocketHandlers = {
    takebackOffers(o: { white?: boolean; black?: boolean }) {
      ctrl.data.player.proposingTakeback = o[ctrl.data.player.color];
      const fromOp = (ctrl.data.opponent.proposingTakeback = o[ctrl.data.opponent.color]);
      if (fromOp) ctrl.opponentRequest('takeback', 'yourOpponentProposesATakeback');
      ctrl.redraw();
    },
    move: ctrl.apiMove,
    drop: ctrl.apiMove,
    reload,
    redirect: ctrl.setRedirecting,
    clockInc(o: { color: Color; time: number }) {
      if (ctrl.clock) {
        ctrl.clock.addTime(o.color, o.time);
        ctrl.redraw();
      }
    },
    cclock(o: { white: number; black: number }) {
      if (ctrl.corresClock) {
        ctrl.data.correspondence!.white = o.white;
        ctrl.data.correspondence!.black = o.black;
        ctrl.corresClock.update(o.white, o.black);
        ctrl.redraw();
      }
    },
    crowd(o: { white: boolean; black: boolean }) {
      (['white', 'black'] as const).forEach(c => {
        if (defined(o[c])) game.setOnGame(ctrl.data, c, o[c]);
      });
      ctrl.redraw();
    },
    endData: ctrl.endWithData,
    rematchOffer(by: Color) {
      ctrl.data.player.offeringRematch = by === ctrl.data.player.color;
      if ((ctrl.data.opponent.offeringRematch = by === ctrl.data.opponent.color))
        ctrl.opponentRequest('rematch', 'yourOpponentWantsToPlayANewGameWithYou');
      ctrl.redraw();
    },
    rematchTaken(nextId: string) {
      ctrl.data.game.rematch = nextId;
      if (!ctrl.data.player.spectator) ctrl.setLoading(true);
      else ctrl.redraw();
    },
    drawOffer(by?: Color) {
      if (ctrl.isPlaying()) {
        ctrl.data.player.offeringDraw = by === ctrl.data.player.color;
        const fromOp = (ctrl.data.opponent.offeringDraw = by === ctrl.data.opponent.color);
        if (fromOp) ctrl.opponentRequest('draw', 'yourOpponentOffersADraw');
      }
      if (by) {
        let ply = ctrl.lastPly();
        if ((by == 'white') == (ply % 2 == 0)) ply++;
        ctrl.data.game.drawOffers = (ctrl.data.game.drawOffers || []).concat([ply]);
      }
      ctrl.redraw();
    },
    berserk(color: Color) {
      ctrl.setBerserk(color);
    },
    gone: ctrl.setGone,
    goneIn: ctrl.setGone,
    checkCount(e: { white: number; black: number }) {
      ctrl.data.player.checks = ctrl.data.player.color == 'white' ? e.white : e.black;
      ctrl.data.opponent.checks = ctrl.data.opponent.color == 'white' ? e.white : e.black;
      ctrl.redraw();
    },
    simulPlayerMove(gameId: string) {
      if (
        ctrl.opts.userId &&
        ctrl.data.simul &&
        ctrl.opts.userId == ctrl.data.simul.hostId &&
        gameId !== ctrl.data.game.id &&
        ctrl.moveOn.get() &&
        !game.isPlayerTurn(ctrl.data)
      ) {
        ctrl.setRedirecting();
        site.sound.play('move');
        location.href = '/' + gameId;
      }
    },
    simulEnd(simul: game.Simul) {
      domDialog({
        htmlText:
          '<div><p>Simul complete!</p><br /><br />' +
          `<a class="button" href="/simul/${simul.id}">Back to ${simul.name} simul</a></div>`,
      });
    },
*/
