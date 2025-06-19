import { type VNode, h, type VNodeChildren } from 'snabbdom';
import { defined } from 'lib';
import { text as xhrText } from 'lib/xhr';
import type AnalyseCtrl from '../ctrl';
import { makeConfig as makeCgConfig } from '../ground';
import type { AnalyseData, NvuiPlugin } from '../interfaces';
import type { Player } from 'lib/game/game';
import { renderIndexAndMove } from '../view/moveView';
import {
  type MoveStyle,
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
  castlingFlavours,
  inputToMove,
  lastCapturedCommandHandler,
  type DropMove,
  possibleMovesHandler,
  renderPockets,
  pocketsStr,
} from 'lib/nvui/chess';
import { renderSetting } from 'lib/nvui/setting';
import { Notify } from 'lib/nvui/notify';
import { commands, boardCommands, addBreaks } from 'lib/nvui/command';
import { type MaybeVNode, bind, noTrans, onInsert } from 'lib/snabbdom';
import { throttle } from 'lib/async';
import explorerView from '../explorer/explorerView';
import { ops, path as treePath } from 'lib/tree/tree';
import { view as cevalView, renderEval, type CevalCtrl } from 'lib/ceval/ceval';
import { next, prev } from '../control';
import { lichessRules } from 'chessops/compat';
import { makeSan } from 'chessops/san';
import { charToRole, opposite, parseUci } from 'chessops/util';
import { parseFen } from 'chessops/fen';
import { setupPosition } from 'chessops/variant';
import { plyToTurn } from 'lib/game/chess';
import { Chessground as makeChessground } from '@lichess-org/chessground';
import { pubsub } from 'lib/pubsub';
import { renderResult, viewContext, type RelayViewContext } from '../view/components';
import { view as chapterNewFormView } from '../study/chapterNewForm';
import { view as chapterEditFormView } from '../study/chapterEditForm';
import renderClocks from '../view/clocks';
import { renderChat } from 'lib/chat/renderChat';

import type * as studyDeps from '../study/studyDeps';
import type RelayCtrl from '../study/relay/relayCtrl';
import type { RetroCtrl } from '../retrospect/retroCtrl';
import { playersView } from '../study/relay/relayPlayers';
import { showInfo as tourOverview } from '../study/relay/relayTourView';

const throttled = (sound: string) => throttle(100, () => site.sound.play(sound));
const selectSound = throttled('select');
const borderSound = throttled('outOfBound');
const errorSound = throttled('error');

export function initModule(ctrl: AnalyseCtrl): NvuiPlugin {
  const notify = new Notify(),
    moveStyle = styleSetting(),
    pieceStyle = pieceSetting(),
    prefixStyle = prefixSetting(),
    positionStyle = positionSetting(),
    boardStyle = boardSetting();

  pubsub.on('analysis.server.progress', (data: AnalyseData) => {
    if (data.analysis && !data.analysis.partial) notify.set('Server-side analysis complete');
  });

  site.mousetrap.bind('c', () => notify.set(renderEvalAndDepth(ctrl)));

  return {
    render(deps?: typeof studyDeps): VNode {
      notify.redraw = ctrl.redraw;
      const d = ctrl.data,
        style = moveStyle.get(),
        clocks = renderClocks(ctrl, ctrl.path),
        pockets = ctrl.node.crazy?.pockets;
      ctrl.chessground = makeChessground(document.createElement('div'), {
        ...makeCgConfig(ctrl),
        animation: { enabled: false },
        drawable: { enabled: false },
        coordinates: false,
      });
      return h('main.analyse', [
        h('div.nvui', [
          studyDetails(ctrl),
          h('h1', 'Textual representation'),
          h('h2', 'Game info'),
          ...['white', 'black'].map((color: Color) =>
            h('p', [`${i18n.site[color]}: `, renderPlayer(ctrl, playerByColor(d, color))]),
          ),
          h('p', `${i18n.site[d.game.rated ? 'rated' : 'casual']} ${d.game.perf || d.game.variant.name}`),
          d.clock ? h('p', `Clock: ${d.clock.initial / 60} + ${d.clock.increment}`) : null,
          h('h2', 'Moves'),
          h('p.moves', { attrs: { role: 'log', 'aria-live': 'off' } }, renderCurrentLine(ctrl, style)),
          ...(!ctrl.studyPractice
            ? [
                h(
                  'button',
                  {
                    attrs: { 'aria-pressed': `${ctrl.explorer.enabled()}` },
                    hook: onInsert((el: HTMLButtonElement) => {
                      const toggle = () => {
                        ctrl.explorer.toggle();
                        ctrl.redraw();
                      };
                      onInsertHandler(toggle, el);
                    }),
                  },
                  i18n.site.openingExplorerAndTablebase,
                ),
                explorerView(ctrl),
              ]
            : []),
          h('h2', 'Pieces'),
          h('div.pieces', renderPieces(ctrl.chessground.state.pieces, style)),
          h('div.pockets', pockets && renderPockets(pockets)),
          ...renderAriaResult(ctrl),
          h('h2', 'Current position'),
          h(
            'p.position.lastMove',
            { attrs: { 'aria-live': 'assertive', 'aria-atomic': 'true' } },
            // make sure consecutive positions are different so that they get re-read
            renderCurrentNode(ctrl, style) + (ctrl.node.ply % 2 === 0 ? '' : ' '),
          ),
          clocks &&
            h('div.clocks', [
              h('h2', `${i18n.site.clock}`),
              h('div.clocks', [h('div.topc', clocks[0]), h('div.botc', clocks[1])]),
            ]),
          h('h2', 'Move form'),
          h(
            'form#move-form',
            {
              hook: {
                insert(vnode) {
                  const $form = $(vnode.elm as HTMLFormElement),
                    $input = $form.find('.move').val('');
                  $form.on('submit', onSubmit(ctrl, notify.set, moveStyle.get, $input));
                },
              },
            },
            [
              h('label', [
                'Command input',
                h('input.move.mousetrap', {
                  attrs: { name: 'move', type: 'text', autocomplete: 'off' },
                }),
              ]),
            ],
          ),
          notify.render(),
          h('h2', 'Computer analysis'),
          renderComputerAnalysis(ctrl, notify, moveStyle.get()),
          h('h2', 'Board'),
          h(
            'div.board',
            {
              hook: {
                insert: el => {
                  const $board = $(el.elm as HTMLElement);
                  const $buttons = $board.find('button');
                  const steps = () => ctrl.tree.getNodeList(ctrl.path);
                  const fenSteps = () => steps().map(step => step.fen);
                  const opponentColor = () => (ctrl.node.ply % 2 === 0 ? 'black' : 'white');
                  $buttons.on('click', selectionHandler(opponentColor, selectSound));
                  $buttons.on('keydown', (e: KeyboardEvent) => {
                    if (e.shiftKey && e.key.match(/^[ad]$/i)) jumpMoveOrLine(ctrl)(e);
                    else if (['o', 'l', 't'].includes(e.key)) boardCommandsHandler()(e);
                    else if (e.key.startsWith('Arrow'))
                      arrowKeyHandler(ctrl.data.player.color, borderSound)(e);
                    else if (e.key === 'c')
                      lastCapturedCommandHandler(fenSteps, pieceStyle.get(), prefixStyle.get())();
                    else if (e.code.match(/^Digit([1-8])$/)) positionJumpHandler()(e);
                    else if (e.key.match(/^[kqrbnp]$/i)) pieceJumpingHandler(selectSound, errorSound)(e);
                    else if (e.key.toLowerCase() === 'm')
                      possibleMovesHandler(
                        ctrl.turnColor(),
                        ctrl.chessground,
                        ctrl.data.game.variant.key,
                        ctrl.nodeList,
                      )(e);
                  });
                },
              },
            },
            renderBoard(
              ctrl.chessground.state.pieces,
              ctrl.data.game.variant.key === 'racingKings' ? 'white' : ctrl.data.player.color,
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
                root.find('.copy-fen').on('click', function (this: HTMLElement) {
                  const inputFen = document.querySelector(
                    '.analyse__underboard__fen input',
                  ) as HTMLInputElement;
                  const fen = inputFen.value;
                  navigator.clipboard.writeText(fen).then(() => {
                    notify.set('FEN copied into clipboard.');
                  });
                });
              },
            },
          }),
          h('h2', i18n.site.advancedSettings),
          h('label', ['Move notation', renderSetting(moveStyle, ctrl.redraw)]),
          h('h3', 'Board settings'),
          h('label', ['Piece style', renderSetting(pieceStyle, ctrl.redraw)]),
          h('label', ['Piece prefix style', renderSetting(prefixStyle, ctrl.redraw)]),
          h('label', ['Show position', renderSetting(positionStyle, ctrl.redraw)]),
          h('label', ['Board layout', renderSetting(boardStyle, ctrl.redraw)]),
          h('h2', i18n.site.keyboardShortcuts),
          h(
            'p',
            [
              'Use arrow keys to navigate in the game.',
              `l: ${i18n.site.toggleLocalAnalysis}`,
              `z: ${i18n.site.toggleAllAnalysis}`,
              `space: ${i18n.site.playComputerMove}`,
              'c: announce computer evaluation',
              `x: ${i18n.site.showThreat}`,
            ].reduce(addBreaks, []),
          ),
          ...boardCommands(),
          h('h2', 'Commands'),
          h(
            'p',
            [
              'Type these commands in the command input.',
              ...inputCommands
                .filter(c => !c.invalid?.(ctrl))
                .flatMap(command => [noTrans(`${command.cmd}: `), command.help]),
            ].reduce<VNodeChildren[]>(
              (acc, curr, i) => (i % 2 != 0 ? addBreaks(acc, curr) : acc.concat(curr)),
              [],
            ),
          ),
          h('h2', 'Chat'),
          ctrl.chatCtrl && renderChat(ctrl.chatCtrl),
          ...(deps && ctrl.study?.relay ? tourDetails(ctrl, ctrl.study, ctrl.study.relay, deps) : []),
        ]),
      ]);
    },
  };
}

function skipOrViewSolution(ctrl: RetroCtrl) {
  return h('div.choices', [
    h(
      'button',
      {
        hook: onInsert((el: HTMLButtonElement) => {
          const viewSolution = () => {
            ctrl.viewSolution();
            ctrl.redraw();
          };
          onInsertHandler(viewSolution, el);
        }),
        attrs: { tabindex: '0' },
      },
      i18n.site.viewTheSolution,
    ),
    h(
      'button',
      {
        hook: onInsert((el: HTMLButtonElement) => {
          const skipThisMove = () => {
            ctrl.skip(), ctrl.redraw();
          };
          onInsertHandler(skipThisMove, el);
        }),
        attrs: { tabindex: '0' },
      },
      i18n.site.skipThisMove,
    ),
  ]);
}

function jumpToNext(ctrl: RetroCtrl) {
  return h(
    'button.half.continue',
    {
      hook: onInsert((el: HTMLButtonElement) => {
        const jumpToNext = () => {
          ctrl.jumpToNext(), ctrl.redraw();
        };
        onInsertHandler(jumpToNext, el);
      }),
      attrs: { 'aria-label': 'Jump to next', tabindex: '0' },
    },
    [i18n.site.next],
  );
}

const minDepth = 8;
const maxDepth = 18;

function renderEvalProgress(node: Tree.Node): VNode {
  return h(
    'div.progress',
    h('div', {
      attrs: {
        style: `width: ${
          node.ceval ? (100 * Math.max(0, node.ceval.depth - minDepth)) / (maxDepth - minDepth) + '%' : 0
        }`,
      },
    }),
  );
}

const feedback = {
  find(ctrl: RetroCtrl): VNode[] {
    return [
      h('div.player', [
        h('div.no-square', h('piece.king.' + ctrl.color)),
        h('div.instruction', [
          h(
            'strong',
            i18n.site.xWasPlayed.asArray(
              h(
                'move',
                { attrs: { tabindex: '0', 'aria-live': 'assertive' } },
                renderIndexAndMove(
                  { withDots: true, showGlyphs: true, showEval: false },
                  ctrl.current()!.fault.node,
                ),
              ),
            ),
          ),
          h(
            'em',
            { attrs: { 'aria-live': 'polite' } },
            i18n.site[ctrl.color === 'white' ? 'findBetterMoveForWhite' : 'findBetterMoveForBlack'],
          ),
          skipOrViewSolution(ctrl),
        ]),
      ]),
    ];
  },
  // user has browsed away from the move to solve
  offTrack(ctrl: RetroCtrl): VNode[] {
    return [
      h('div.player', [
        h('div.icon.off', { attrs: { 'aria-label': i18n.site.resumeLearning } }, '!'),

        h('div.instruction', [
          h('strong', { 'aria-live': 'assertive' }, i18n.site.youBrowsedAway),
          h('div.choices.off', [
            h(
              'button',
              {
                tabindex: '0',
                hook: onInsert((el: HTMLButtonElement) => {
                  const jumpToNext = () => {
                    ctrl.jumpToNext();
                  };
                  onInsertHandler(jumpToNext, el);
                }),
              },
              i18n.site.resumeLearning,
            ),
          ]),
        ]),
      ]),
    ];
  },
  fail(ctrl: RetroCtrl): VNode[] {
    return [
      h('div.player', [
        h('div.icon', { attrs: { 'aria-label': i18n.site.youCanDoBetter } }, '✗'),
        h('div.instruction', [
          h('strong', { attrs: { 'aria-live': 'assertive' } }, i18n.site.youCanDoBetter),
          h(
            'em',
            { attrs: { 'aria-live': 'assertive' } },
            i18n.site[ctrl.color === 'white' ? 'tryAnotherMoveForWhite' : 'tryAnotherMoveForBlack'],
          ),
          skipOrViewSolution(ctrl),
        ]),
      ]),
    ];
  },
  win(ctrl: RetroCtrl): VNode[] {
    return [
      h(
        'div.half.top',
        h('div.player', [
          h('div.icon', { attrs: { 'aria-label': i18n.study.goodMove } }, '✓'),
          h('div.instruction', h('strong', { attrs: { 'aria-live': 'assertive' } }, i18n.study.goodMove)),
        ]),
      ),
      jumpToNext(ctrl),
    ];
  },
  view(ctrl: RetroCtrl): VNode[] {
    return [
      h(
        'div.half.top',
        h('div.player', [
          h('div.icon', { attrs: { 'aria-label': i18n.site.solution } }, '✓'),
          h('div.instruction', { attrs: { 'tab-index': '0' } }, [
            h('strong', { attrs: { 'aria-live': 'assertive' } }, i18n.site.solution),
            h(
              'em',
              i18n.site.bestWasX.asArray(
                h(
                  'strong',
                  { attrs: { 'aria-live': 'assertive' } },
                  renderIndexAndMove({ withDots: true, showEval: false }, ctrl.current()!.solution.node),
                ),
              ),
            ),
          ]),
        ]),
      ),
      jumpToNext(ctrl),
    ];
  },
  eval(ctrl: RetroCtrl): VNode[] {
    return [
      h(
        'div.half.top',
        h('div.player.center', [
          h('div.instruction', [
            h('strong', { attrs: { 'aria-live': 'assertive' } }, i18n.site.evaluatingYourMove),
            renderEvalProgress(ctrl.node()),
          ]),
        ]),
      ),
    ];
  },
  end(ctrl: RetroCtrl, hasFullComputerAnalysis: () => boolean): VNode[] {
    if (!hasFullComputerAnalysis())
      return [
        h(
          'div.half.top',
          h('div.player', [
            h('div.instruction', { attrs: { 'aria-live': 'polite' } }, i18n.site.waitingForAnalysis),
          ]),
        ),
      ];
    const nothing = !ctrl.completion()[1];
    return [
      h('div.player', [
        h('div.no-square', h('piece.king.' + ctrl.color)),
        h('div.instruction', [
          h(
            'em',
            { attrs: { 'aria-live': 'polite' } },
            i18n.site[
              nothing
                ? ctrl.color === 'white'
                  ? 'noMistakesFoundForWhite'
                  : 'noMistakesFoundForBlack'
                : ctrl.color === 'white'
                  ? 'doneReviewingWhiteMistakes'
                  : 'doneReviewingBlackMistakes'
            ],
          ),
          h('div.choices.end', [
            nothing
              ? null
              : h(
                  'button',
                  {
                    attrs: {
                      'tab-index': '0',
                    },
                    key: 'reset',
                    hook: onInsert((el: HTMLButtonElement) => {
                      const doItAgain = () => {
                        ctrl.reset();
                      };
                      onInsertHandler(doItAgain, el);
                    }),
                  },
                  i18n.site.doItAgain,
                ),
            h(
              'button',
              {
                attrs: {
                  'tab-index': '0',
                },
                key: 'flip',
                hook: onInsert((el: HTMLButtonElement) => {
                  const flipBoard = () => {
                    ctrl.flip();
                  };
                  onInsertHandler(flipBoard, el);
                }),
              },
              i18n.site[ctrl.color === 'white' ? 'reviewBlackMistakes' : 'reviewWhiteMistakes'],
            ),
          ]),
        ]),
      ]),
    ];
  },
};

type Command = 'p' | 's' | 'eval' | 'best' | 'prev' | 'next' | 'prev line' | 'next line' | 'pocket';
type InputCommand = {
  cmd: Command;
  help: VNode | string;
  cb: (ctrl: AnalyseCtrl, notify: (txt: string) => void, style: MoveStyle, input: string) => void;
  invalid?: (ctrl: AnalyseCtrl) => boolean;
};

const inputCommands: InputCommand[] = [
  {
    cmd: 'p',
    // help: commands().piece.help, couses normal ui to not render peices
    help: noTrans('Announce a pieces location. ie: p n or p N'),
    cb: (ctrl, notify, style, input) =>
      notify(
        commands().piece.apply(input, ctrl.chessground.state.pieces, style) ||
          `Bad input: ${input}. Exptected format: ${commands().piece.help}`,
      ),
  },
  {
    cmd: 's',
    // help: commands().scan.help, causes normal ui to not render peices
    help: noTrans('Scan pieces on a row or file. ie: s 1, s d or s D'),
    cb: (ctrl, notify, style, input) =>
      notify(
        commands().scan.apply(input, ctrl.chessground.state.pieces, style) ||
          `Bad input: ${input}. Exptected format: ${commands().scan.help}`,
      ),
  },
  {
    cmd: 'eval',
    help: noTrans("announce last move's computer evaluation"),
    cb: (ctrl, notify) => notify(renderEvalAndDepth(ctrl)),
  },
  {
    cmd: 'best',
    help: noTrans('announce the top engine move'),
    cb: (ctrl, notify, style) => notify(renderBestMove(ctrl, style)),
  },
  {
    cmd: 'prev',
    help: noTrans('return to the previous move'),
    cb: ctrl => doAndRedraw(ctrl, prev),
  },
  { cmd: 'next', help: noTrans('go to the next move'), cb: ctrl => doAndRedraw(ctrl, next) },
  {
    cmd: 'prev line',
    help: noTrans('switch to the previous variation'),
    cb: ctrl => doAndRedraw(ctrl, jumpPrevLine),
  },
  {
    cmd: 'next line',
    help: noTrans('switch to the next variation'),
    cb: ctrl => doAndRedraw(ctrl, jumpNextLine),
  },
  {
    cmd: 'pocket',
    help: noTrans('Read out pockets for white or black. Example: "pocket black"'),
    cb: (ctrl, notify, _, input) => {
      const pockets = ctrl.node.crazy?.pockets;
      const color = input.split(' ')?.[1]?.trim();
      return notify(
        pockets
          ? color
            ? pocketsStr(color === 'white' ? pockets[0] : pockets[1]) || i18n.site.none
            : 'Expected format: pocket [white|black]'
          : 'Command only available in crazyhouse',
      );
    },
    invalid: ctrl => ctrl.data.game.variant.key !== 'crazyhouse',
  },
];

const getCommand = (input: string) => {
  const split = input.split(' ');
  const firstWordLowerCase = split[0].toLowerCase();
  return (
    inputCommands.find(c => c.cmd === input) ||
    inputCommands.find(c => split.length !== 1 && c.cmd === firstWordLowerCase)
  ); // 'next line' should not be interpreted as 'next'
};

const doAndRedraw = (ctrl: AnalyseCtrl, fn: (ctrl: AnalyseCtrl) => void): void => {
  fn(ctrl);
  ctrl.redraw();
};

const playerByColor = (d: AnalyseData, color: Color): Player =>
  color === d.player.color ? d.player : d.opponent;

const jumpNextLine = (ctrl: AnalyseCtrl) => jumpLine(ctrl, 1);
const jumpPrevLine = (ctrl: AnalyseCtrl) => jumpLine(ctrl, -1);

const focus = (el: HTMLElement) => el.focus();

const onInsertHandler = (callback: () => void, el: HTMLElement) => {
  el.addEventListener('click', callback);
  el.addEventListener('keydown', ev => ev.key === 'Enter' && callback());

  el.addEventListener('click', _ => focus(el));
  el.addEventListener('keydown', ev => ev.key === 'Enter' && focus(el));
};

const redirectToSelectedHook = bind('change', (e: InputEvent) => {
  const target = e.target as HTMLSelectElement;
  const selectedOption = target.options[target.selectedIndex];
  const url = selectedOption.getAttribute('url');
  if (url) window.location.href = url;
});

const renderPlayer = (ctrl: AnalyseCtrl, player: Player): VNodeChildren =>
  player.ai ? i18n.site.aiNameLevelAiLevel('Stockfish', player.ai) : userHtml(ctrl, player);

const evalInfo = (bestEv: EvalScore | undefined): string =>
  defined(bestEv?.cp)
    ? renderEval(bestEv.cp).replace('-', '−')
    : defined(bestEv?.mate)
      ? `mate in ${Math.abs(bestEv.mate)} for ${bestEv.mate > 0 ? 'white' : 'black'}`
      : '';

const depthInfo = (clientEv: Tree.ClientEval | undefined, isCloud: boolean): string =>
  clientEv ? `${i18n.site.depthX(clientEv.depth || 0)} ${isCloud ? 'Cloud' : ''}` : '';

const noEvalStr = (ctrl: CevalCtrl) =>
  !ctrl.allowed()
    ? 'local evaluation not allowed'
    : !ctrl.possible
      ? 'local evaluation not possible'
      : !ctrl.enabled()
        ? 'local evaluation not enabled'
        : '';

function renderEvalAndDepth(ctrl: AnalyseCtrl): string {
  if (ctrl.threatMode()) return `${evalInfo(ctrl.node.threat)} ${depthInfo(ctrl.node.threat, false)}`;
  const evs = ctrl.currentEvals(),
    bestEv = cevalView.getBestEval(evs);
  const evalStr = evalInfo(bestEv);
  return !evalStr ? noEvalStr(ctrl.ceval) : `${evalStr} ${depthInfo(evs.client, !!evs.client?.cloud)}`;
}
function renderBestMove(ctrl: AnalyseCtrl, style: MoveStyle): string {
  const noEvalMsg = noEvalStr(ctrl.ceval);
  if (noEvalMsg) return noEvalMsg;
  const node = ctrl.node,
    setup = parseFen(node.fen).unwrap();
  let pvs: Tree.PvData[] = [];
  if (ctrl.threatMode() && node.threat) {
    pvs = node.threat.pvs;
    setup.turn = opposite(setup.turn);
    if (setup.turn === 'white') setup.fullmoves += 1;
  } else if (node.ceval) pvs = node.ceval.pvs;
  const pos = setupPosition(lichessRules(ctrl.ceval.opts.variant.key), setup);
  if (pos.isOk && pvs.length > 0 && pvs[0].moves.length > 0) {
    const uci = pvs[0].moves[0];
    const san = makeSan(pos.unwrap(), parseUci(uci)!);
    return renderSan(san, uci, style);
  }
  return '';
}

function renderFeedback(root: AnalyseCtrl, fb: Exclude<keyof typeof feedback, 'end'>) {
  const ctrl: RetroCtrl = root.retro!;
  const current = ctrl.current();
  if (ctrl.isSolving() && current && root.path !== current.prev.path) return feedback.offTrack(ctrl);
  if (fb === 'find') return current ? feedback.find(ctrl) : feedback.end(ctrl, root.hasFullComputerAnalysis);
  return feedback[fb](ctrl);
}

function renderRetro(root: AnalyseCtrl): VNode | undefined {
  const ctrl = root.retro;
  if (!ctrl) return;

  const fb = ctrl.feedback(),
    completion = ctrl.completion();

  return h('div.retro-box.training-box.sub-box', [
    h('div.title', [
      h('h3', { attrs: { 'aria-live': 'assertive' } }, i18n.site.learnFromYourMistakes),
      h(
        'p',
        { attrs: { 'aria-label': 'mistake number' } },
        `${Math.min(completion[0] + 1, completion[1])} / ${completion[1]}`,
      ),
      h('button.fbt', {
        hook: onInsert((el: HTMLButtonElement) => {
          const toggleLFYM = () => {
            root.toggleRetro();
            root.redraw();
          };
          onInsertHandler(toggleLFYM, el);
        }),
        attrs: { 'aria-label': 'toggle learn from your mistakes' },
      }),
    ]),
    h('div.feedback.' + fb, { attrs: { 'aria-live': 'assertive' } }, renderFeedback(root, fb)),
  ]);
}

function renderAriaResult(ctrl: AnalyseCtrl): VNode[] {
  const result = renderResult(ctrl);
  const res = result.length ? result : 'No result';
  return [
    h('h2', 'Game status'),
    h('div.status', { attrs: { role: 'status', 'aria-live': 'assertive', 'aria-atomic': 'true' } }, res),
  ];
}

function renderCurrentLine(ctrl: AnalyseCtrl, style: MoveStyle): VNodeChildren {
  if (ctrl.path.length === 0) return renderMainline(ctrl.mainline, ctrl.path, style);
  else {
    const futureNodes = ctrl.node.children.length > 0 ? ops.mainlineNodeList(ctrl.node.children[0]) : [];
    return renderMainline(ctrl.nodeList.concat(futureNodes), ctrl.path, style);
  }
}

function renderLFYMButton(ctrl: AnalyseCtrl, notify: Notify): VNode {
  return h(
    'button',
    {
      hook: onInsert((el: HTMLButtonElement) => {
        const toggleLFYM = () => {
          ctrl.toggleRetro();
          notify.set('Learn from your mistakes');
          ctrl.nvuiLearning = !ctrl.nvuiLearning;
          ctrl.redraw();
        };
        onInsertHandler(toggleLFYM, el);
      }),
    },
    'Learn from your mistakes',
  );
}

function onSubmit(ctrl: AnalyseCtrl, notify: (txt: string) => void, style: () => MoveStyle, $input: Cash) {
  return (e: SubmitEvent) => {
    e.preventDefault();
    const input = castlingFlavours(($input.val() as string).trim());
    // Allow commands with/without a leading '/'
    const command = getCommand(input) || getCommand(input.slice(1));
    if (command && !command.invalid?.(ctrl)) command.cb(ctrl, notify, style(), input);
    else {
      const move = inputToMove(input, ctrl.node.fen, ctrl.chessground);
      const isDrop = (u: undefined | string | DropMove) => !!(u && typeof u !== 'string');
      const isInvalidDrop = (d: DropMove) =>
        !ctrl.crazyValid(d.role, d.key) || ctrl.chessground.state.pieces.has(d.key);
      const isInvalidCrazy = isDrop(move) && isInvalidDrop(move);

      if (!move || isInvalidCrazy) notify(`Invalid move: ${input}`);
      else sendMove(move, ctrl);
    }
    $input.val('');
  };
}

function sendMove(uciOrDrop: string | DropMove, ctrl: AnalyseCtrl) {
  if (typeof uciOrDrop === 'string')
    ctrl.sendMove(
      uciOrDrop.slice(0, 2) as Key,
      uciOrDrop.slice(2, 4) as Key,
      undefined,
      charToRole(uciOrDrop.slice(4)),
    );
  else if (ctrl.crazyValid(uciOrDrop.role, uciOrDrop.key)) ctrl.sendNewPiece(uciOrDrop.role, uciOrDrop.key);
}

function requestAnalBtn(ctrl: AnalyseCtrl): VNode {
  return h(
    'button',
    {
      hook: onInsert((el: HTMLButtonElement) => {
        const reqAnal = () => {
          xhrText(`/${ctrl.data.game.id}/request-analysis`, { method: 'post' });
          ctrl.redraw();
        };
        onInsertHandler(reqAnal, el);
      }),
    },
    i18n.site.requestAComputerAnalysis,
  );
}

function renderAcpl(ctrl: AnalyseCtrl, style: MoveStyle): VNode {
  const anal = ctrl.data.analysis; // heh
  if (!anal) return requestAnalBtn(ctrl);
  const analysisGlyphs = ['?!', '?', '??'];
  const analysisNodes = ctrl.mainline.filter(n => n.glyphs?.find(g => analysisGlyphs.includes(g.symbol)));
  const res: Array<VNode> = [];
  ['white', 'black'].forEach((color: Color) => {
    res.push(h('h3', `${color} player: ${anal[color].acpl} ${i18n.site.averageCentipawnLoss}`));
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
  return h('section', res);
}

function renderComputerAnalysis(ctrl: AnalyseCtrl, notify: Notify, moveStyle: MoveStyle): VNode {
  if (ctrl.hasFullComputerAnalysis()) {
    if (ctrl.ongoing || ctrl.synthetic) {
      notify.set('Server-side analysis in progress');
      return h('h2', 'Server-side analysis in progress');
    }
    if (ctrl.nvuiLearning) {
      const LFYM = renderRetro(ctrl);
      if (LFYM) {
        return LFYM;
      }
      notify.set('Problem rendering learn from your mistakes');
    }
    return h('section', [renderLFYMButton(ctrl, notify), renderAcpl(ctrl, moveStyle)]);
  }
  // catch all analysis issues
  return requestAnalBtn(ctrl);
}

function currentLineIndex(ctrl: AnalyseCtrl): { i: number; of: number } {
  if (ctrl.path === treePath.root) return { i: 1, of: 1 };
  const prevNode = ctrl.tree.nodeAtPath(treePath.init(ctrl.path));
  return {
    i: prevNode.children.findIndex(node => node.id === ctrl.node.id),
    of: prevNode.children.length,
  };
}

function renderLineIndex(ctrl: AnalyseCtrl): string {
  const { i, of } = currentLineIndex(ctrl);
  return of > 1 ? `, line ${i + 1} of ${of} ,` : '';
}

function renderCurrentNode(ctrl: AnalyseCtrl, style: MoveStyle): string {
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

function userHtml(ctrl: AnalyseCtrl, player: Player) {
  const d = ctrl.data,
    user = player.user,
    perf = user ? user.perfs[d.game.perf] : null,
    rating = player.rating ? player.rating : perf && perf.rating,
    rd = player.ratingDiff,
    ratingDiff = rd ? (rd > 0 ? '+' + rd : rd < 0 ? '−' + -rd : '') : '';
  const studyPlayers = ctrl.study && renderStudyPlayer(ctrl, player.color);
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
    : studyPlayers || h('span', i18n.site.anonymous);
}

function renderStudyPlayer(ctrl: AnalyseCtrl, color: Color): VNode | undefined {
  const player = ctrl.study?.currentChapter().players?.[color];
  const keys = [
    ['name', i18n.site.name],
    ['title', 'title'],
    ['rating', i18n.site.rating],
    ['fed', 'fed'],
    ['team', 'team'],
  ] as const;
  return (
    player &&
    h(
      'span',
      keys
        .reduce<
          string[]
        >((strs, [key, i18n]) => (player[key] ? strs.concat(`${i18n}: ${key === 'fed' ? player[key].name : player[key]}`) : strs), [])
        .join(' '),
    )
  );
}

function jumpLine(ctrl: AnalyseCtrl, delta: number) {
  const { i, of } = currentLineIndex(ctrl);
  if (of === 1) return;
  const newI = (i + delta + of) % of;
  const prevPath = treePath.init(ctrl.path);
  const prevNode = ctrl.tree.nodeAtPath(prevPath);
  const newPath = prevPath + prevNode.children[newI].id;
  ctrl.userJumpIfCan(newPath);
}

function tourDetails(
  ctrl: AnalyseCtrl,
  study: studyDeps.StudyCtrl,
  relay: RelayCtrl,
  deps: typeof studyDeps,
): VNode[] {
  const ctx: RelayViewContext = { ...viewContext(ctrl, deps), study, deps, relay, allowVideo: false };
  const tour = ctx.relay.data.tour;
  ctx.relay.redraw = ctrl.redraw;

  return [
    h('h1', 'Tour details'),
    h('h2', 'Overview'),
    h('div', tourOverview(tour.info, tour.dates)),
    h('h2', 'Players'),
    h(
      'button',
      {
        hook: onInsert((el: HTMLButtonElement) => {
          const toggle = () => {
            ctx.relay.tab('players');
            ctrl.redraw();
          };
          onInsertHandler(toggle, el);
        }),
      },
      'Load player list',
    ),
    ctx.relay.tab() === 'players' ? h('div', playersView(ctx.relay.players, ctx.relay.data.tour)) : h('div'),
  ];
}

function studyDetails(ctrl: AnalyseCtrl): MaybeVNode {
  const study = ctrl.study;
  const relayGroups = study?.relay?.data.group;
  const relayRounds = study?.relay?.data.rounds;
  const tour = study?.relay?.data.tour;
  const hash = window.location.hash;
  return (
    study &&
    h('div.study-details', [
      h('h2', 'Study details'),
      h('h3', `Title: ${study.data.name}. By: ${study.data.ownerId}`),
      h('br'),
      relayGroups &&
        h(
          'div.relay-groups',
          h('label', [
            'Current group:',
            h(
              'select',
              {
                attrs: { autofocus: hash === '#group-select' },
                hook: redirectToSelectedHook,
              },
              relayGroups.tours.map(t =>
                h(
                  'option',
                  { attrs: { selected: t.id == tour?.id, url: `/broadcast/-/${t.id}#group-select` } },
                  t.name,
                ),
              ),
            ),
          ]),
        ),
      tour &&
        relayRounds &&
        h(
          'div.relay-rounds',
          h('label', [
            'Current round:',
            h(
              'select',
              {
                attrs: { autofocus: hash === '#round-select' },
                hook: redirectToSelectedHook,
              },
              relayRounds.map(r =>
                h(
                  'option',
                  {
                    attrs: {
                      selected: r.id == study.data.id,
                      url: `/broadcast/${tour.slug}/${r.slug}/${r.id}#round-select`,
                    },
                  },
                  r.name,
                ),
              ),
            ),
          ]),
        ),
      h('div.chapters', [
        h('label', [
          'Current chapter:',
          h(
            'select',
            {
              attrs: { id: 'chapter-select' },
              hook: bind('change', (e: InputEvent) => {
                const target = e.target as HTMLSelectElement;
                const selectedOption = target.options[target.selectedIndex];
                const chapterId = selectedOption.getAttribute('chapterId');
                study.setChapter(chapterId!);
              }),
            },
            study.chapters.list.all().map((ch, i) =>
              h(
                'option',
                {
                  attrs: {
                    selected: ch.id === study.currentChapter().id,
                    chapterId: ch.id,
                  },
                },
                `${i + 1}. ${ch.name}`,
              ),
            ),
          ),
        ]),
        study.members.canContribute()
          ? h('div.buttons', [
              h(
                'button',
                {
                  hook: onInsert((el: HTMLButtonElement) => {
                    const toggle = () => {
                      study.chapters.editForm.toggle(study.currentChapter());
                      ctrl.redraw();
                    };
                    onInsertHandler(toggle, el);
                  }),
                },
                [
                  'Edit current chapter',
                  study.chapters.editForm.current() && chapterEditFormView(study.chapters.editForm),
                ],
              ),
              h(
                'button',
                {
                  hook: onInsert((el: HTMLButtonElement) => {
                    const toggle = () => {
                      study.chapters.newForm.toggle();
                      ctrl.redraw();
                    };
                    onInsertHandler(toggle, el);
                  }),
                },
                [
                  'Add new chapter',
                  study.chapters.newForm.isOpen() ? chapterNewFormView(study.chapters.newForm) : undefined,
                ],
              ),
            ])
          : undefined,
      ]),
    ])
  );
}

function jumpMoveOrLine(ctrl: AnalyseCtrl) {
  return (e: KeyboardEvent) => {
    if (e.key === 'A') doAndRedraw(ctrl, e.altKey ? jumpPrevLine : prev);
    else if (e.key === 'D') doAndRedraw(ctrl, e.altKey ? jumpNextLine : next);
  };
}
