import { h, VNode } from 'snabbdom';
import { prop, Prop } from 'common';
import * as xhr from 'common/xhr';
import AnalyseController from '../ctrl';
import { makeConfig as makeCgConfig } from '../ground';
import { Chessground } from 'chessground';
import { Redraw, AnalyseData, MaybeVNodes } from '../interfaces';
import { Player } from 'game';
import {
  renderSan,
  renderPieces,
  renderBoard,
  renderMainline,
  renderComments,
  styleSetting,
  pieceSetting,
  prefixSetting,
  boardSetting,
  positionSetting,
  boardCommandsHandler,
  selectionHandler,
  arrowKeyHandler,
  positionJumpHandler,
  pieceJumpingHandler,
  Style,
  castlingFlavours,
  inputToLegalUci,
  namePiece,
} from 'nvui/chess';
import { renderSetting } from 'nvui/setting';
import { Notify } from 'nvui/notify';
import { commands } from 'nvui/command';
import * as moveView from '../moveView';
import { bind } from '../util';
import throttle from 'common/throttle';
import { Role } from 'chessground/types';

export const throttled = (sound: string) => throttle(100, () => lichess.sound.play(sound));

const selectSound = throttled('select');
const wrapSound = throttled('wrapAround');
const borderSound = throttled('outOfBound');
const errorSound = throttled('error');

lichess.AnalyseNVUI = function (redraw: Redraw) {
  const notify = new Notify(redraw),
    moveStyle = styleSetting(),
    pieceStyle = pieceSetting(),
    prefixStyle = prefixSetting(),
    positionStyle = positionSetting(),
    boardStyle = boardSetting(),
    analysisInProgress = prop(false);

  lichess.pubsub.on('analysis.server.progress', (data: AnalyseData) => {
    if (data.analysis && !data.analysis.partial) notify.set('Server-side analysis complete');
  });

  return {
    render(ctrl: AnalyseController): VNode {
      const d = ctrl.data,
        style = moveStyle.get();
      if (!ctrl.chessground)
        ctrl.chessground = Chessground(document.createElement('div'), {
          ...makeCgConfig(ctrl),
          animation: { enabled: false },
          drawable: { enabled: false },
          coordinates: false,
        });
      return h('main.analyse', [
        h('div.nvui', [
          h('h1', 'Textual representation'),
          h('h2', 'Game info'),
          ...['white', 'black'].map((color: Color) =>
            h('p', [color + ' player: ', renderPlayer(ctrl, playerByColor(d, color))])
          ),
          h('p', `${d.game.rated ? 'Rated' : 'Casual'} ${d.game.perf}`),
          d.clock ? h('p', `Clock: ${d.clock.initial / 60} + ${d.clock.increment}`) : null,
          h('h2', 'Moves'),
          h(
            'p.moves',
            {
              attrs: {
                role: 'log',
                'aria-live': 'off',
              },
            },
            renderMainline(ctrl.mainline, ctrl.path, style)
          ),
          h('h2', 'Pieces'),
          h('div.pieces', renderPieces(ctrl.chessground.state.pieces, style)),
          h('h2', 'Current position'),
          h(
            'p.position',
            {
              attrs: {
                'aria-live': 'assertive',
                'aria-atomic': 'true',
              },
            },
            // make sure consecutive positions are different so that they get re-read
            renderCurrentNode(ctrl.node, style) + (ctrl.node.ply % 2 === 0 ? '' : ' ')
          ),
          h('h2', 'Move form'),
          h(
            'form',
            {
              hook: {
                insert(vnode) {
                  const $form = $(vnode.elm as HTMLFormElement),
                    $input = $form.find('.move').val('');
                  $input[0]!.focus();
                  $form.on('submit', onSubmit(ctrl, notify.set, moveStyle.get, $input));
                },
              },
            },
            [
              h('label', [
                'Command input',
                h('input.move.mousetrap', {
                  attrs: {
                    name: 'move',
                    type: 'text',
                    autocomplete: 'off',
                    autofocus: true,
                  },
                }),
              ]),
            ]
          ),
          notify.render(),
          // h('h2', 'Actions'),
          // h('div.actions', tableInner(ctrl)),
          h('h2', 'Computer analysis'),
          ...(renderAcpl(ctrl, style) || [requestAnalysisButton(ctrl, analysisInProgress, notify.set)]),
          h('h2', 'Board'),
          h(
            'div.board',
            {
              hook: {
                insert: el => {
                  const $board = $(el.elm as HTMLElement);
                  $board.on('keypress', boardCommandsHandler());
                  const $buttons = $board.find('button');
                  $buttons.on('click', selectionHandler(ctrl.data.opponent.color, selectSound));
                  $buttons.on('keydown', arrowKeyHandler(ctrl.data.player.color, borderSound));
                  $buttons.on('keypress', positionJumpHandler());
                  $buttons.on('keypress', pieceJumpingHandler(wrapSound, errorSound));
                },
              },
            },
            renderBoard(
              ctrl.chessground.state.pieces,
              ctrl.data.player.color,
              pieceStyle.get(),
              prefixStyle.get(),
              positionStyle.get(),
              boardStyle.get()
            )
          ),
          h('div.content', {
            hook: {
              insert: vnode => {
                const root = $(vnode.elm as HTMLElement);
                root.append($('.blind-content').removeClass('none'));
                root.find('.copy-pgn').on('click', () => {
                  (root.find('.game-pgn').attr('type', 'text')[0] as HTMLInputElement).select();
                  document.execCommand('copy');
                  root.find('.game-pgn').attr('type', 'hidden');
                  notify.set('PGN copied into clipboard.');
                });
              },
            },
          }),
          h('h2', 'Settings'),
          h('label', ['Move notation', renderSetting(moveStyle, ctrl.redraw)]),
          h('h3', 'Board Settings'),
          h('label', ['Piece style', renderSetting(pieceStyle, ctrl.redraw)]),
          h('label', ['Piece prefix style', renderSetting(prefixStyle, ctrl.redraw)]),
          h('label', ['Show position', renderSetting(positionStyle, ctrl.redraw)]),
          h('label', ['Board layout', renderSetting(boardStyle, ctrl.redraw)]),
          h('h2', 'Keyboard shortcuts'),
          h('p', ['Use arrow keys to navigate in the game.']),
          h('h2', 'Board Mode commands'),
          h('p', [
            'Use these commands when focused on the board itself.',
            h('br'),
            'o: announce current position.',
            h('br'),
            "c: announce last move's captured piece.",
            h('br'),
            'l: display last move.',
            h('br'),
            't: display clocks.',
            h('br'),
            'arrow keys: move left, right, up or down.',
            h('br'),
            'kqrbnp/KQRBNP: move forward/backward to a piece.',
            h('br'),
            '1-8: move to rank 1-8.',
            h('br'),
            'Shift+1-8: move to file a-h.',
            h('br'),
            '',
            h('br'),
          ]),
          h('h2', 'Commands'),
          h('p', [
            'Type these commands in the command input.',
            h('br'),
            commands.piece.help,
            h('br'),
            commands.scan.help,
            h('br'),
          ]),
        ]),
      ]);
    },
  };
};

function onSubmit(ctrl: AnalyseController, notify: (txt: string) => void, style: () => Style, $input: Cash) {
  return function () {
    let input = castlingFlavours(($input.val() as string).trim());
    if (isShortCommand(input)) input = '/' + input;
    if (input[0] === '/') onCommand(ctrl, notify, input.slice(1), style());
    else {
      const uci = inputToLegalUci(input, ctrl.node.fen, ctrl.chessground);
      if (uci)
        ctrl.sendMove(
          uci.slice(0, 2) as Key,
          uci.slice(2, 4) as Key,
          undefined,
          namePiece[uci.slice(4)] as Role | undefined
        );
      else notify('Invalid command');
    }
    $input.val('');
    return false;
  };
}

const shortCommands = ['p', 'scan'];

function isShortCommand(input: string): boolean {
  return shortCommands.includes(input.split(' ')[0]);
}

function onCommand(ctrl: AnalyseController, notify: (txt: string) => void, c: string, style: Style) {
  const pieces = ctrl.chessground.state.pieces;
  notify(commands.piece.apply(c, pieces, style) || commands.scan.apply(c, pieces, style) || `Invalid command: ${c}`);
}

const analysisGlyphs = ['?!', '?', '??'];

function renderAcpl(ctrl: AnalyseController, style: Style): MaybeVNodes | undefined {
  const anal = ctrl.data.analysis;
  if (!anal) return undefined;
  const analysisNodes = ctrl.mainline.filter(n => (n.glyphs || []).find(g => analysisGlyphs.includes(g.symbol)));
  const res: Array<VNode> = [];
  ['white', 'black'].forEach((color: Color) => {
    const acpl = anal[color].acpl;
    res.push(h('h3', `${color} player: ${acpl} ACPL`));
    res.push(
      h(
        'select',
        {
          hook: bind('change', e => {
            ctrl.jumpToMain(parseInt((e.target as HTMLSelectElement).value));
            ctrl.redraw();
          }),
        },
        analysisNodes
          .filter(n => (n.ply % 2 === 1) === (color === 'white'))
          .map(node =>
            h(
              'option',
              {
                attrs: {
                  value: node.ply,
                  selected: node.ply === ctrl.node.ply,
                },
              },
              [moveView.plyToTurn(node.ply), renderSan(node.san!, node.uci, style), renderComments(node, style)].join(
                ' '
              )
            )
          )
      )
    );
  });
  return res;
}

function requestAnalysisButton(ctrl: AnalyseController, inProgress: Prop<boolean>, notify: (msg: string) => void) {
  if (inProgress()) return h('p', 'Server-side analysis in progress');
  if (ctrl.ongoing || ctrl.synthetic) return undefined;
  return h(
    'button',
    {
      hook: bind('click', _ =>
        xhr
          .text(`/${ctrl.data.game.id}/request-analysis`, {
            method: 'post',
          })
          .then(
            () => {
              inProgress(true);
              notify('Server-side analysis in progress');
            },
            _ => notify('Cannot run server-side analysis')
          )
      ),
    },
    'Request a computer analysis'
  );
}

function renderCurrentNode(node: Tree.Node, style: Style): string {
  if (!node.san || !node.uci) return 'Initial position';
  return [moveView.plyToTurn(node.ply), renderSan(node.san, node.uci, style), renderComments(node, style)]
    .join(' ')
    .trim();
}

function renderPlayer(ctrl: AnalyseController, player: Player) {
  return player.ai ? ctrl.trans('aiNameLevelAiLevel', 'Stockfish', player.ai) : userHtml(ctrl, player);
}

function userHtml(ctrl: AnalyseController, player: Player) {
  const d = ctrl.data,
    user = player.user,
    perf = user ? user.perfs[d.game.perf] : null,
    rating = player.rating ? player.rating : perf && perf.rating,
    rd = player.ratingDiff,
    ratingDiff = rd ? (rd > 0 ? '+' + rd : rd < 0 ? '−' + -rd : '') : '';
  return user
    ? h('span', [
        h(
          'a',
          {
            attrs: { href: '/@/' + user.username },
          },
          user.title ? `${user.title} ${user.username}` : user.username
        ),
        rating ? ` ${rating}` : ``,
        ' ' + ratingDiff,
      ])
    : 'Anonymous';
}

function playerByColor(d: AnalyseData, color: Color) {
  return color === d.player.color ? d.player : d.opponent;
}
