import { h, VNode } from 'snabbdom';
import { defined, prop, Prop } from 'common';
import * as xhr from 'common/xhr';
import AnalyseController from '../ctrl';
import { makeConfig as makeCgConfig } from '../ground';
import { AnalyseData } from '../interfaces';
import { Player } from 'game';
import viewStatus from 'game/view/status';
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
  lastCapturedCommandHandler,
} from 'nvui/chess';
import { renderSetting } from 'nvui/setting';
import { Notify } from 'nvui/notify';
import { commands } from 'nvui/command';
import { bind, MaybeVNodes } from 'common/snabbdom';
import throttle from 'common/throttle';
import { Role } from 'chessground/types';
import explorerView from '../explorer/explorerView';
import { ops, path as treePath } from 'tree';
import { view as cevalView, renderEval } from 'ceval';
import * as control from '../control';
import { lichessRules } from 'chessops/compat';
import { makeSan } from 'chessops/san';
import { opposite, parseUci } from 'chessops/util';
import { parseFen } from 'chessops/fen';
import { setupPosition } from 'chessops/variant';
import { plyToTurn } from '../util';

const throttled = (sound: string) => throttle(100, () => site.sound.play(sound));
const selectSound = throttled('select');
const borderSound = throttled('outOfBound');
const errorSound = throttled('error');

export function initModule(ctrl: AnalyseController) {
  const notify = new Notify(),
    moveStyle = styleSetting(),
    pieceStyle = pieceSetting(),
    prefixStyle = prefixSetting(),
    positionStyle = positionSetting(),
    boardStyle = boardSetting(),
    analysisInProgress = prop(false);

  site.pubsub.on('analysis.server.progress', (data: AnalyseData) => {
    if (data.analysis && !data.analysis.partial) notify.set('Server-side analysis complete');
  });

  site.mousetrap.bind('c', () => notify.set(renderEvalAndDepth(ctrl)));

  return {
    render(): VNode {
      notify.redraw = ctrl.redraw;
      const d = ctrl.data,
        style = moveStyle.get();
      if (!ctrl.chessground)
        ctrl.chessground = site.makeChessground(document.createElement('div'), {
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
            h('p', [color + ' player: ', renderPlayer(ctrl, playerByColor(d, color))]),
          ),
          h('p', `${d.game.rated ? 'Rated' : 'Casual'} ${d.game.perf}`),
          d.clock ? h('p', `Clock: ${d.clock.initial / 60} + ${d.clock.increment}`) : null,
          h('h2', 'Moves'),
          h('p.moves', { attrs: { role: 'log', 'aria-live': 'off' } }, renderCurrentLine(ctrl, style)),
          ...(!ctrl.studyPractice
            ? [
                h(
                  'button',
                  {
                    attrs: { 'aria-pressed': `${ctrl.explorer.enabled()}` },
                    hook: bind('click', _ => ctrl.explorer.toggle(), ctrl.redraw),
                  },
                  ctrl.trans.noarg('openingExplorerAndTablebase'),
                ),
                explorerView(ctrl),
              ]
            : []),
          h('h2', 'Pieces'),
          h('div.pieces', renderPieces(ctrl.chessground.state.pieces, style)),
          ...renderResult(ctrl),
          h('h2', 'Current position'),
          h(
            'p.position.lastMove',
            { attrs: { 'aria-live': 'assertive', 'aria-atomic': 'true' } },
            // make sure consecutive positions are different so that they get re-read
            renderCurrentNode(ctrl, style) + (ctrl.node.ply % 2 === 0 ? '' : ' '),
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
                  attrs: { name: 'move', type: 'text', autocomplete: 'off', autofocus: true },
                }),
              ]),
            ],
          ),
          notify.render(),
          // h('h2', 'Actions'),
          // h('div.actions', tableInner(ctrl)),
          h('h2', 'Computer analysis'),
          ...cevalView.renderCeval(ctrl),
          cevalView.renderPvs(ctrl),
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
                  const steps = () => ctrl.tree.getNodeList(ctrl.path);
                  const fenSteps = () => steps().map(step => step.fen);
                  const opponentColor = () => (ctrl.node.ply % 2 === 0 ? 'black' : 'white');
                  $buttons.on('click', selectionHandler(opponentColor, selectSound));
                  $buttons.on('keydown', arrowKeyHandler(ctrl.data.player.color, borderSound));
                  $buttons.on(
                    'keypress',
                    lastCapturedCommandHandler(fenSteps, pieceStyle.get(), prefixStyle.get()),
                  );
                  $buttons.on('keypress', positionJumpHandler());
                  $buttons.on('keypress', pieceJumpingHandler(selectSound, errorSound));
                },
              },
            },
            renderBoard(
              ctrl.chessground.state.pieces,
              ctrl.data.player.color,
              pieceStyle.get(),
              prefixStyle.get(),
              positionStyle.get(),
              boardStyle.get(),
            ),
          ),
          h(
            'div.boardstatus',
            {
              attrs: { 'aria-live': 'polite', 'aria-atomic': 'true' },
            },
            '',
          ),
          h('div.content', {
            hook: {
              insert: vnode => {
                const root = $(vnode.elm as HTMLElement);
                root.append($('.blind-content').removeClass('none'));
                root.find('.copy-pgn').on('click', function (this: HTMLElement) {
                  navigator.clipboard.writeText(this.dataset.pgn!).then(() => {
                    notify.set('PGN copied into clipboard.');
                  });
                });
              },
            },
          }),
          h('h2', 'Settings'),
          h('label', ['Move notation', renderSetting(moveStyle, ctrl.redraw)]),
          h('h3', 'Board settings'),
          h('label', ['Piece style', renderSetting(pieceStyle, ctrl.redraw)]),
          h('label', ['Piece prefix style', renderSetting(prefixStyle, ctrl.redraw)]),
          h('label', ['Show position', renderSetting(positionStyle, ctrl.redraw)]),
          h('label', ['Board layout', renderSetting(boardStyle, ctrl.redraw)]),
          h('h2', 'Keyboard shortcuts'),
          h('p', [
            'Use arrow keys to navigate in the game.',
            h('br'),
            'l: toggle local computer analysis',
            h('br'),
            'z: toggle all computer analysis',
            h('br'),
            'space: play best computer move',
            h('br'),
            'c: announce computer evaluation',
            h('br'),
            'x: show threat',
            h('br'),
          ]),
          h('h2', 'Board mode commands'),
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
            "eval: announce last move's computer evaluation",
            h('br'),
            'best: announce the top engine move',
            h('br'),
            'prev: return to the previous move',
            h('br'),
            'next: go to the next move',
            h('br'),
            'prev line: switch to the previous variation',
            h('br'),
            'next line: switch to the next variation',
          ]),
        ]),
      ]);
    },
  };
}

const NOT_ALLOWED = 'local evaluation not allowed';
const NOT_POSSIBLE = 'local evaluation not possible';
const NOT_ENABLED = 'local evaluation not enabled';

function renderEvalAndDepth(ctrl: AnalyseController): string {
  let evalStr: string, depthStr: string;
  if (ctrl.threatMode()) {
    evalStr = evalInfo(ctrl.node.threat);
    depthStr = depthInfo(ctrl, ctrl.node.threat, false);
    return `${evalInfo(ctrl.node.threat)} ${depthInfo(ctrl, ctrl.node.threat, false)}`;
  } else {
    const evs = ctrl.currentEvals(),
      bestEv = cevalView.getBestEval(evs);
    evalStr = evalInfo(bestEv);
    depthStr = depthInfo(ctrl, evs.client, !!evs.client?.cloud);
  }
  if (!evalStr) {
    if (!ctrl.ceval.allowed()) return NOT_ALLOWED;
    else if (!ctrl.ceval.possible) return NOT_POSSIBLE;
    else return NOT_ENABLED;
  } else {
    return evalStr + ' ' + depthStr;
  }
}

function evalInfo(bestEv: EvalScore | undefined): string {
  if (bestEv) {
    if (defined(bestEv.cp)) return renderEval(bestEv.cp).replace('-', '−');
    else if (defined(bestEv.mate))
      return `mate in ${Math.abs(bestEv.mate)} for ${bestEv.mate > 0 ? 'white' : 'black'}`;
  }
  return '';
}

function depthInfo(ctrl: AnalyseController, clientEv: Tree.ClientEval | undefined, isCloud: boolean): string {
  if (!clientEv) return '';
  const depth = clientEv.depth || 0;
  return ctrl.trans('depthX', depth) + isCloud ? ' Cloud' : '';
}

function renderBestMove(ctrl: AnalyseController, style: Style): string {
  const instance = ctrl.getCeval();
  if (!instance.allowed()) return NOT_ALLOWED;
  if (!instance.possible) return NOT_POSSIBLE;
  if (!instance.enabled()) return NOT_ENABLED;
  const node = ctrl.node,
    setup = parseFen(node.fen).unwrap();
  let pvs: Tree.PvData[] = [];
  if (ctrl.threatMode() && node.threat) {
    pvs = node.threat.pvs;
    setup.turn = opposite(setup.turn);
    if (setup.turn === 'white') setup.fullmoves += 1;
  } else if (node.ceval) {
    pvs = node.ceval.pvs;
  }
  const pos = setupPosition(lichessRules(instance.opts.variant.key), setup);
  if (pos.isOk && pvs.length > 0 && pvs[0].moves.length > 0) {
    const uci = pvs[0].moves[0];
    const san = makeSan(pos.unwrap(), parseUci(uci)!);
    return renderSan(san, uci, style);
  } else {
    return '';
  }
}

function renderResult(ctrl: AnalyseController): VNode[] {
  if (ctrl.data.game.status.id >= 30) {
    let result;
    switch (ctrl.data.game.winner) {
      case 'white':
        result = '1-0';
        break;
      case 'black':
        result = '0-1';
        break;
      default:
        result = '½-½';
    }
    return [
      h('h2', 'Game status'),
      h('div.status', { attrs: { role: 'status', 'aria-live': 'assertive', 'aria-atomic': 'true' } }, [
        h('div.result', result),
        h('div.status', viewStatus(ctrl)),
      ]),
    ];
  }
  return [];
}

function renderCurrentLine(ctrl: AnalyseController, style: Style): (string | VNode)[] {
  if (ctrl.path.length === 0) {
    return renderMainline(ctrl.mainline, ctrl.path, style);
  } else {
    const futureNodes = ctrl.node.children.length > 0 ? ops.mainlineNodeList(ctrl.node.children[0]) : [];
    return renderMainline(ctrl.nodeList.concat(futureNodes), ctrl.path, style);
  }
}

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
          namePiece[uci.slice(4)] as Role | undefined,
        );
      else notify('Invalid command');
    }
    $input.val('');
    return false;
  };
}

const shortCommands = ['p', 's', 'next', 'prev', 'eval', 'best'];

function isShortCommand(input: string): boolean {
  return shortCommands.includes(input.split(' ')[0].toLowerCase());
}

function onCommand(ctrl: AnalyseController, notify: (txt: string) => void, c: string, style: Style) {
  const lowered = c.toLowerCase();
  if (lowered === 'next') {
    control.next(ctrl);
    ctrl.redraw();
  } else if (lowered === 'prev') {
    control.prev(ctrl);
    ctrl.redraw();
  } else if (lowered === 'next line') {
    jumpNextLine(ctrl);
    ctrl.redraw();
  } else if (lowered === 'prev line') {
    jumpPrevLine(ctrl);
    ctrl.redraw();
  } else if (lowered === 'eval') notify(renderEvalAndDepth(ctrl));
  else if (lowered === 'best') notify(renderBestMove(ctrl, style));
  else {
    const pieces = ctrl.chessground.state.pieces;
    notify(
      commands.piece.apply(c, pieces, style) ||
        commands.scan.apply(c, pieces, style) ||
        `Invalid command: ${c}`,
    );
  }
}

const analysisGlyphs = ['?!', '?', '??'];

function renderAcpl(ctrl: AnalyseController, style: Style): MaybeVNodes | undefined {
  const anal = ctrl.data.analysis;
  if (!anal) return undefined;
  const analysisNodes = ctrl.mainline.filter(n =>
    (n.glyphs || []).find(g => analysisGlyphs.includes(g.symbol)),
  );
  const res: Array<VNode> = [];
  ['white', 'black'].forEach((color: Color) => {
    const acpl = anal[color].acpl;
    res.push(h('h3', `${color} player: ${acpl} ACPL`));
    res.push(
      h(
        'select',
        {
          hook: bind(
            'change',
            e => ctrl.jumpToMain(parseInt((e.target as HTMLSelectElement).value)),
            ctrl.redraw,
          ),
        },
        analysisNodes
          .filter(n => (n.ply % 2 === 1) === (color === 'white'))
          .map(node =>
            h(
              'option',
              { attrs: { value: node.ply, selected: node.ply === ctrl.node.ply } },
              [plyToTurn(node.ply), renderSan(node.san!, node.uci, style), renderComments(node, style)].join(
                ' ',
              ),
            ),
          ),
      ),
    );
  });
  return res;
}

function requestAnalysisButton(
  ctrl: AnalyseController,
  inProgress: Prop<boolean>,
  notify: (msg: string) => void,
) {
  if (inProgress()) return h('p', 'Server-side analysis in progress');
  if (ctrl.ongoing || ctrl.synthetic) return undefined;
  return h(
    'button',
    {
      hook: bind('click', _ =>
        xhr.text(`/${ctrl.data.game.id}/request-analysis`, { method: 'post' }).then(
          () => {
            inProgress(true);
            notify('Server-side analysis in progress');
          },
          () => notify('Cannot run server-side analysis'),
        ),
      ),
    },
    'Request a computer analysis',
  );
}

function currentLineIndex(ctrl: AnalyseController): { i: number; of: number } {
  if (ctrl.path === treePath.root) return { i: 1, of: 1 };
  const prevNode = ctrl.tree.nodeAtPath(treePath.init(ctrl.path));
  return {
    i: prevNode.children.findIndex(node => node.id === ctrl.node.id),
    of: prevNode.children.length,
  };
}

function renderLineIndex(ctrl: AnalyseController): string {
  const { i, of } = currentLineIndex(ctrl);
  return of > 1 ? `, line ${i + 1} of ${of} ,` : '';
}

function renderCurrentNode(ctrl: AnalyseController, style: Style): string {
  const node = ctrl.node;
  if (!node.san || !node.uci) return 'Initial position';
  return [
    plyToTurn(node.ply),
    renderSan(node.san, node.uci, style),
    renderLineIndex(ctrl),
    renderComments(node, style),
  ]
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
          { attrs: { href: '/@/' + user.username } },
          user.title ? `${user.title} ${user.username}` : user.username,
        ),
        rating ? ` ${rating}` : ``,
        ' ' + ratingDiff,
      ])
    : 'Anonymous';
}

function playerByColor(d: AnalyseData, color: Color) {
  return color === d.player.color ? d.player : d.opponent;
}

const jumpNextLine = (ctrl: AnalyseController) => jumpLine(ctrl, 1);
const jumpPrevLine = (ctrl: AnalyseController) => jumpLine(ctrl, -1);

function jumpLine(ctrl: AnalyseController, delta: number) {
  const { i, of } = currentLineIndex(ctrl);
  if (of === 1) return;
  const newI = (i + delta + of) % of;
  const prevPath = treePath.init(ctrl.path);
  const prevNode = ctrl.tree.nodeAtPath(prevPath);
  const newPath = prevPath + prevNode.children[newI].id;
  ctrl.userJumpIfCan(newPath);
}
