import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import AnalyseController from '../ctrl';
import { makeConfig as makeCgConfig } from '../ground';
import { Chessground } from 'chessground';
import { Redraw, AnalyseData, MaybeVNodes } from '../interfaces';
import { Player } from 'game';
import { renderSan, renderPieces, renderBoard, styleSetting } from 'nvui/chess';
import { renderSetting } from 'nvui/setting';
import { Notify } from 'nvui/notify';
import { Style } from 'nvui/chess';
import * as moveView from '../moveView';
import { bind } from '../util';

window.lichess.AnalyseNVUI = function(redraw: Redraw) {

  const notify = new Notify(redraw),
    moveStyle = styleSetting();

  return {
    render(ctrl: AnalyseController): VNode {
      const d = ctrl.data, style = moveStyle.get();
      if (!ctrl.chessground) ctrl.chessground = Chessground(document.createElement("div"), {
        ...makeCgConfig(ctrl),
        animation: { enabled: false },
        drawable: { enabled: false },
        coordinates: false
      });
      return h('div.nvui', [
        h('h1', 'Textual representation'),
        h('h2', 'Game info'),
        ...(['white', 'black'].map((color: Color) => h('p', [
          color + ' player: ',
          renderPlayer(ctrl, playerByColor(d, color))
        ]))),
        h('p', `${d.game.rated ? 'Rated' : 'Casual'} ${d.game.perf}`),
        d.clock ? h('p', `Clock: ${d.clock.initial / 60} + ${d.clock.increment}`) : null,
        h('h2', 'Moves'),
        h('p.moves', {
          attrs: {
            role : 'log',
            'aria-live': 'off'
          }
        }, renderMainline(ctrl.mainline, ctrl.path, style)),
        h('h2', 'Pieces'),
        h('div.pieces', renderPieces(ctrl.chessground.state.pieces, style)),
        h('h2', 'Current position'),
        h('p.position', {
          attrs: {
            'aria-live' : 'assertive',
            'aria-atomic' : true
          }
        }, renderCurrentNode(ctrl.node, style)),
        notify.render(),
        // h('h2', 'Actions'),
        // h('div.actions', tableInner(ctrl)),
        h('h2', 'Computer analysis'),
        ...(renderAcpl(ctrl, style) || [requestAnalysisButton(ctrl)]),
        h('h2', 'Board'),
        h('pre.board', renderBoard(ctrl.chessground.state.pieces, ctrl.data.player.color)),
        h('h2', 'Settings'),
        h('label', [
          'Move notation',
          renderSetting(moveStyle, ctrl.redraw)
        ]),
        // h('h2', 'Commands'),
        // h('p', [
        //   'Type these commands in the move input',
        //   h('br'),
        //   '/c: Read clocks',
        //   h('br'),
        //   '/l: Read last move'
        // ])
      ]);
    }
  };
}

const analysisGlyphs = ['?!', '?', '??'];

function renderAcpl(ctrl: AnalyseController, style: Style): MaybeVNodes | undefined {
  const anal = ctrl.data.analysis;
  if (!anal) return undefined;
  const analysisNodes = ctrl.mainline.filter(n => (n.glyphs || []).find(g => analysisGlyphs.indexOf(g.symbol) > -1));
  const res: Array<VNode> = [];
  ['white', 'black'].forEach((color: Color) => {
    const acpl = anal[color].acpl;
    res.push(h('h3', `${color} player: ${acpl} ACPl`));
    res.push(h('select', {
      hook: bind('change', e => {
        ctrl.jumpToMain(parseInt((e.target as HTMLSelectElement).value));
        ctrl.redraw();
      })
    },
      analysisNodes.filter(n => (n.ply % 2 === 1) === (color === 'white')).map(node =>
        h('option', {
          attrs: {
            value: node.ply,
            selected: node.ply === ctrl.node.ply
          }
        }, [
          moveView.plyToTurn(node.ply),
          renderSan(node.san!, node.uci, style),
          renderComments(node, style)
        ].join(' '))
      )));
  });
  return res;
}

function requestAnalysisButton(ctrl: AnalyseController) {
  if (ctrl.ongoing || ctrl.synthetic) return undefined;
  return h('button', {
    hook: bind('click', _ =>  {
    })
  }, 'Request a computer analysis');
}

// function onSubmit(ctrl: RoundController, notify: (txt: string) => void, $input: JQuery) {
//   return function() {
//     const input = castlingFlavours($input.val());
//     if (input == '/c') notify($('.nvui .botc').text() + ', ' + $('.nvui .topc').text());
//     else if (input == '/l') notify($('.lastMove').text());
//     else {
//       const d = ctrl.data,
//         legalUcis = destsToUcis(ctrl.chessground.state.movable.dests!),
//         sans: Sans = sanWriter(plyStep(d, ctrl.ply).fen, legalUcis) as Sans,
//         uci = sanToUci(input, sans) || input;
//       if (legalUcis.indexOf(uci.toLowerCase()) >= 0) ctrl.socket.send("move", {
//         from: uci.substr(0, 2),
//         to: uci.substr(2, 2),
//         promotion: uci.substr(4, 1)
//       }, { ackable: true });
//       else notify(d.player.color === d.game.player ? `Invalid move: ${input}` : 'Not your turn');
//     }
//     $input.val('');
//     return false;
//   };
// }

// function castlingFlavours(input: string): string {
//   switch(input.toLowerCase().replace(/[-\s]+/g, '')) {
//     case 'oo': case '00': return 'o-o';
//     case 'ooo': case '000': return 'o-o-o';
//   }
//   return input;
// }

// function anyClock(ctrl: RoundController, position: Position) {
//   const d = ctrl.data, player = ctrl.playerAt(position);
//   return (ctrl.clock && renderClock(ctrl, player, position)) || (
//     d.correspondence && renderCorresClock(ctrl.corresClock!, ctrl.trans, player.color, position, d.game.player)
//   ) || undefined;
// }

// function destsToUcis(dests: DecodedDests) {
//   const ucis: string[] = [];
//   Object.keys(dests).forEach(function(orig) {
//     dests[orig].forEach(function(dest) {
//       ucis.push(orig + dest);
//     });
//   });
//   return ucis;
// }

// function sanToUci(san: string, sans: Sans): Uci | undefined {
//   if (san in sans) return sans[san];
//   const lowered = san.toLowerCase();
//   for (let i in sans)
//     if (i.toLowerCase() === lowered) return sans[i];
//   return;
// }

function renderMainline(nodes: Tree.Node[], currentPath: Tree.Path, style: Style) {
  const res: Array<string | VNode> = [];
  let path: Tree.Path = '';
  nodes.forEach(node => {
    if (!node.san || !node.uci) return;
    path += node.id;
    const content: MaybeVNodes = [
      node.ply & 1 ? '' + moveView.plyToTurn(node.ply) : null,
      renderSan(node.san, node.uci, style)
    ];
    res.push(h('move', {
      attrs: { p: path },
      class: { active: path === currentPath }
    }, content));
    res.push(renderComments(node, style));
    res.push(', ');
    if (node.ply % 2 === 0) res.push(h('br'));
  });
  return res;
}

function renderCurrentNode(node: Tree.Node, style: Style): string {
  if (!node.san || !node.uci) return 'Initial position';
  return [
    moveView.plyToTurn(node.ply),
    renderSan(node.san, node.uci, style),
    renderComments(node, style)
  ].join(' ');
}

function renderComments(node: Tree.Node, style: Style): string {
  if (!node.comments) return '';
  return (node.comments || []).map(c => renderComment(c, style)).join('. ');
}

function renderComment(comment: Tree.Comment, style: Style): string {
  return comment.by === 'lichess' ?
    comment.text.replace(/Best move was (.+)\./, (_, san) =>
      'Best move was ' + renderSan(san, undefined, style)) :
      comment.text;
}

function renderPlayer(ctrl: AnalyseController, player: Player) {
  return player.ai ? ctrl.trans('aiNameLevelAiLevel', 'Stockfish', player.ai) : userHtml(ctrl, player);
}

function userHtml(ctrl: AnalyseController, player: Player) {
  const d = ctrl.data,
    user = player.user,
    perf = user ? user.perfs[d.game.perf] : null,
    rating = player.rating ? player.rating : (perf && perf.rating),
    rd = player.ratingDiff,
    ratingDiff = rd ? (rd > 0 ? '+' + rd : ( rd < 0 ? '−' + (-rd) : '')) : '';
  return user ? h('span', [
    h('a', {
      attrs: { href: '/@/' + user.username }
    }, user.title ? `${user.title} ${user.username}` : user.username),
    rating ? ` ${rating}` : ``,
    ' ' + ratingDiff,
  ]) : 'Anonymous';
}

function playerByColor(d: AnalyseData, color: Color) {
  return color === d.player.color ? d.player : d.opponent;
}
