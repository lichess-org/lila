import { type VNode, type LooseVNodes, type VNodeChildren, hl, bind, noTrans } from 'lib/view';
import { defined } from 'lib';
import { text as xhrText } from 'lib/xhr';
import type AnalyseCtrl from '../ctrl';
import { makeConfig as makeCgConfig } from '../ground';
import type { AnalyseData } from '../interfaces';
import type { Player } from 'lib/game';
import {
  renderSan,
  renderPieces,
  renderBoard,
  renderMainline,
  renderComments,
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
  leaveSquareHandler,
} from 'lib/nvui/chess';
import { liveText } from 'lib/nvui/notify';
import { renderSetting } from 'lib/nvui/setting';
import { commands, boardCommands, addBreaks } from 'lib/nvui/command';
import explorerView from '../explorer/explorerView';
import { ops, path as treePath } from 'lib/tree/tree';
import { view as cevalView, renderEval } from 'lib/ceval';
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
import { throttle } from 'lib/async';
import { renderRetro } from '../retrospect/nvuiRetroView';
import { playersView } from '../study/relay/relayPlayers';
import { showInfo as tourOverview } from '../study/relay/relayTourView';
import type { AnalyseNvuiContext } from '../analyse.nvui';
import { scanDirectionsHandler } from 'lib/nvui/directionScan';
import type { ClientEval, PvData } from 'lib/tree/types';
import { COLORS } from 'chessops';

const throttled = (sound: string) => throttle(100, () => site.sound.play(sound));
const selectSound = throttled('select');
const borderSound = throttled('outOfBound');
const errorSound = throttled('error');

export function initNvui(ctx: AnalyseNvuiContext): void {
  const { ctrl, notify } = ctx;
  pubsub.on('analysis.server.progress', (data: AnalyseData) => {
    if (data.analysis && !data.analysis.partial) notify.set('Server-side analysis complete');
  });
  site.mousetrap.unbind('c');
  site.mousetrap.bind('c', () => notify.set(renderEvalAndDepth(ctrl)));
}

export function renderNvui(ctx: AnalyseNvuiContext): VNode {
  const { ctrl, deps, notify, moveStyle, pieceStyle, prefixStyle, positionStyle, boardStyle } = ctx;
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
  return hl('main.analyse', [
    hl('div.nvui', [
      studyDetails(ctrl),
      hl('h2', i18n.nvui.gameInfo),
      ...COLORS.map(color => hl('p', [`${i18n.site[color]}: `, renderPlayer(ctrl, playerByColor(d, color))])),
      hl('p', `${i18n.site[d.game.rated ? 'rated' : 'casual']} ${d.game.perf || d.game.variant.name}`),
      d.clock ? hl('p', `Clock: ${d.clock.initial / 60} + ${d.clock.increment}`) : null,
      hl('h2', i18n.nvui.moveList),
      hl('p.moves', { attrs: { role: 'log', 'aria-live': 'off' } }, renderCurrentLine(ctx)),
      !ctrl.study?.practice && [
        hl(
          'button',
          {
            attrs: { 'aria-pressed': `${ctrl.explorer.enabled()}` },
            hook: bind('click', _ => ctrl.explorer.toggle(), ctrl.redraw),
          },
          i18n.site.openingExplorerAndTablebase,
        ),
        explorerView(ctrl),
      ],
      hl('h2', i18n.nvui.pieces),
      renderPieces(ctrl.chessground.state.pieces, style),
      pockets && hl('h2', i18n.nvui.pockets),
      pockets && renderPockets(pockets),
      renderAriaResult(ctrl),
      hl('h2', i18n.nvui.lastMove),
      !ctrl.retro && liveText(renderCurrentNode(ctx), 'polite', 'p.position.lastMove'),
      clocks &&
        hl('div.clocks', [
          hl('h2', `${i18n.site.clock}`),
          hl('div.clocks', [hl('div.topc', clocks[0]), hl('div.botc', clocks[1])]),
        ]),
      hl('h2', i18n.nvui.inputForm),
      hl(
        'form#move-form',
        {
          hook: {
            insert(vnode) {
              const $form = $(vnode.elm as HTMLFormElement),
                $input = $form.find('.move').val('');
              $form.on('submit', onSubmit(ctx, $input));
            },
          },
        },
        [
          hl('label', [
            i18n.nvui.inputForm,
            hl('input.move.mousetrap', {
              attrs: { name: 'move', type: 'text', autocomplete: 'off' },
            }),
          ]),
        ],
      ),
      notify.render(),
      renderRetro(ctx),
      !ctrl.retro && [
        hl('h2', i18n.site.computerAnalysis),
        cevalView.renderCeval(ctrl), // beware unsolicted redraws hosing the screen reader
        cevalView.renderPvs(ctrl),
        renderAcpl(ctx) || requestAnalysisBtn(ctx),
      ],
      hl('h2', i18n.site.board),
      hl(
        'div.board',
        { hook: { insert: el => boardEventsHook(ctx, el.elm as HTMLElement) } },
        renderBoard(
          ctrl.chessground.state.pieces,
          ctrl.data.game.variant.key === 'racingKings' ? 'white' : ctrl.bottomColor(),
          pieceStyle.get(),
          prefixStyle.get(),
          positionStyle.get(),
          boardStyle.get(),
        ),
      ),
      hl('div.boardstatus', { attrs: { 'aria-live': 'polite', 'aria-atomic': 'true' } }, ''),
      hl('div.content', {
        hook: {
          insert: vnode => {
            const root = $(vnode.elm as HTMLElement);
            root.append($('.blind-content').removeClass('none'));
            root.find('.copy-pgn').on('click', function (this: HTMLElement) {
              navigator.clipboard.writeText(this.dataset.pgn!).then(() => {
                notify.set(i18n.nvui.copiedToClipboard('PGN'));
              });
            });
            root.find('.copy-fen').on('click', function (this: HTMLElement) {
              const inputFen = document.querySelector('.analyse__underboard__fen input') as HTMLInputElement;
              const fen = inputFen.value;
              navigator.clipboard.writeText(fen).then(() => {
                notify.set(i18n.nvui.copiedToClipboard('FEN'));
              });
            });
          },
        },
      }),
      hl('h2', i18n.site.advancedSettings),
      hl('label', ['Move notation', renderSetting(moveStyle, ctrl.redraw)]),
      hl('h3', 'Board settings'),
      hl('label', ['Piece style', renderSetting(pieceStyle, ctrl.redraw)]),
      hl('label', ['Piece prefix style', renderSetting(prefixStyle, ctrl.redraw)]),
      hl('label', ['Show position', renderSetting(positionStyle, ctrl.redraw)]),
      hl('label', ['Board layout', renderSetting(boardStyle, ctrl.redraw)]),
      hl('h2', i18n.site.keyboardShortcuts),
      hl(
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
      boardCommands(),
      hl('h2', i18n.nvui.inputFormCommandList),
      hl(
        'p',
        [
          'Type these commands in the command input.',
          ...inputCommands
            .filter(c => !c.invalid?.(ctrl))
            .flatMap(command => [noTrans(`${command.cmd}: `), command.help]),
        ].reduce<VNodeChildren[]>(
          (acc, curr, i) => (i % 2 !== 0 ? addBreaks(acc, curr) : acc.concat(curr)),
          [],
        ),
      ),
      hl('h2', 'Chat'),
      ctrl.chatCtrl && renderChat(ctrl.chatCtrl),
      deps && ctrl.study?.relay && tourDetails(ctx),
    ]),
  ]);
}

export function clickHook(main?: (el: HTMLElement) => void, post?: () => void) {
  return {
    // put unique identifying props on the button container (such as class)
    // because snabbdom WILL mix plain adjacent buttons up.
    hook: {
      insert: (vnode: VNode) => {
        const el = vnode.elm as HTMLElement;
        el.addEventListener('click', () => {
          main?.(el);
          post?.();
        });
        el.addEventListener('keydown', (e: KeyboardEvent) => {
          if (e.key === 'Enter') {
            main?.(el);
            post?.();
          }
        });
      },
    },
  };
}

function boardEventsHook(
  { ctrl, pieceStyle, prefixStyle, moveStyle, notify }: AnalyseNvuiContext,
  el: HTMLElement,
): void {
  const $board = $(el);
  const $buttons = $board.find('button');
  const steps = () => ctrl.tree.getNodeList(ctrl.path);
  const fenSteps = () => steps().map(step => step.fen);
  const opponentColor = () => (ctrl.node.ply % 2 === 0 ? 'black' : 'white');
  $buttons.on('blur', leaveSquareHandler($buttons));
  $buttons.on('click', selectionHandler(opponentColor));
  $buttons.on('keydown', (e: KeyboardEvent) => {
    if (e.shiftKey && e.key.match(/^[ad]$/i)) jumpMoveOrLine(ctrl)(e);
    else if (e.key.match(/^x$/i))
      scanDirectionsHandler(ctrl.bottomColor(), ctrl.chessground.state.pieces, moveStyle.get())(e);
    else if (['o', 'l', 't'].includes(e.key)) boardCommandsHandler()(e);
    else if (e.key.startsWith('Arrow')) arrowKeyHandler(ctrl.bottomColor(), borderSound)(e);
    else if (e.key === 'c') lastCapturedCommandHandler(fenSteps, pieceStyle.get(), prefixStyle.get())();
    else if (e.key === 'i') {
      e.preventDefault();
      document.querySelector<HTMLElement>('input.move')?.focus();
    } else if (e.key === 'f') {
      if (ctrl.data.game.variant.key !== 'racingKings') {
        notify.set('Flipping the board');
        setTimeout(() => ctrl.flip(), 1000);
      }
    } else if (e.code.match(/^Digit([1-8])$/)) positionJumpHandler()(e);
    else if (e.key.match(/^[kqrbnp]$/i)) pieceJumpingHandler(selectSound, errorSound)(e);
    else if (e.key.toLowerCase() === 'm')
      possibleMovesHandler(ctrl.turnColor(), ctrl.chessground, ctrl.data.game.variant.key, ctrl.nodeList)(e);
    else if (e.key.toLowerCase() === 'v') notify.set(renderEvalAndDepth(ctrl));
    else if (e.key === 'G') ctrl.playBestMove();
    else if (e.key === 'g') notify.set(renderBestMove({ ctrl, moveStyle } as AnalyseNvuiContext));
  });
}

function renderEvalAndDepth(ctrl: AnalyseCtrl): string {
  if (ctrl.threatMode()) return `${evalInfo(ctrl.node.threat)} ${depthInfo(ctrl.node.threat, false)}`;
  const evs = { client: ctrl.getNode().ceval, server: ctrl.getNode().eval },
    bestEv = cevalView.getBestEval(ctrl);
  const evalStr = evalInfo(bestEv);
  return !evalStr ? noEvalStr(ctrl) : `${evalStr} ${depthInfo(evs.client, !!evs.client?.cloud)}`;
}

const evalInfo = (bestEv: EvalScore | undefined): string =>
  defined(bestEv?.cp)
    ? renderEval(bestEv.cp).replace('-', '−')
    : defined(bestEv?.mate)
      ? `mate in ${Math.abs(bestEv.mate)} for ${bestEv.mate > 0 ? 'white' : 'black'}`
      : '';

const depthInfo = (clientEv: ClientEval | undefined, isCloud: boolean): string =>
  clientEv ? `${i18n.site.depthX(clientEv.depth || 0)} ${isCloud ? 'Cloud' : ''}` : '';

const noEvalStr = (ctrl: AnalyseCtrl) =>
  !ctrl.isCevalAllowed()
    ? 'local evaluation not allowed'
    : !ctrl.cevalEnabled()
      ? 'local evaluation not enabled'
      : '';

function renderBestMove({ ctrl, moveStyle }: AnalyseNvuiContext): string {
  const noEvalMsg = noEvalStr(ctrl);
  if (noEvalMsg) return noEvalMsg;
  const node = ctrl.node,
    setup = parseFen(node.fen).unwrap();
  let pvs: PvData[] = [];
  if (ctrl.threatMode() && node.threat) {
    pvs = node.threat.pvs;
    setup.turn = opposite(setup.turn);
    if (setup.turn === 'white') setup.fullmoves += 1;
  } else if (node.ceval) pvs = node.ceval.pvs;
  const pos = setupPosition(lichessRules(ctrl.ceval.opts.variant.key), setup);
  if (pos.isOk && pvs.length > 0 && pvs[0].moves.length > 0) {
    const uci = pvs[0].moves[0];
    const san = makeSan(pos.unwrap(), parseUci(uci)!);
    return renderSan(san, uci, moveStyle.get());
  }
  return '';
}

function renderAriaResult(ctrl: AnalyseCtrl): VNode[] {
  const result = renderResult(ctrl);
  const res = result.length ? result : i18n.site.none;
  return [
    hl('h2', i18n.nvui.gameStatus),
    hl('div', { attrs: { role: 'status', 'aria-live': 'assertive', 'aria-atomic': 'true' } }, res),
  ];
}

function renderCurrentLine({ ctrl, moveStyle }: AnalyseNvuiContext) {
  if (ctrl.path.length === 0) return renderMainline(ctrl.mainline, ctrl.path, moveStyle.get(), !ctrl.retro);
  else {
    const futureNodes = ctrl.node.children.length > 0 ? ops.mainlineNodeList(ctrl.node.children[0]) : [];
    return renderMainline(ctrl.nodeList.concat(futureNodes), ctrl.path, moveStyle.get(), !ctrl.retro);
  }
}

function onSubmit(ctx: AnalyseNvuiContext, $input: Cash) {
  const { ctrl, notify } = ctx;
  return (e: SubmitEvent) => {
    e.preventDefault();
    const input = castlingFlavours(($input.val() as string).trim());
    // Allow commands with/without a leading '/'
    const command = getCommand(input) || getCommand(input.slice(1));
    if (command && !command.invalid?.(ctrl)) command.cb(ctx, input);
    else {
      const move = inputToMove(input, ctrl.node.fen, ctrl.chessground);
      const isDrop = (u: undefined | string | DropMove) => !!(u && typeof u !== 'string');
      const isInvalidDrop = (d: DropMove) =>
        !ctrl.crazyValid(d.role, d.key) || ctrl.chessground.state.pieces.has(d.key);
      const isInvalidCrazy = isDrop(move) && isInvalidDrop(move);

      if (!move || isInvalidCrazy) notify.set(`Invalid move: ${input}`);
      else sendMove(move, ctrl);
    }
    $input.val('');
  };
}

type Command = 'b' | 'p' | 's' | 'eval' | 'best' | 'prev' | 'next' | 'prev line' | 'next line' | 'pocket';
type InputCommand = {
  cmd: Command;
  help: VNode | string;
  cb: (ctrl: AnalyseNvuiContext, input: string) => void;
  invalid?: (ctrl: AnalyseCtrl) => boolean;
};

const inputCommands: InputCommand[] = [
  {
    cmd: 'b',
    help: commands().board.help,
    cb: ({ ctrl, notify, moveStyle }, input) =>
      notify.set(commands().board.apply(input, ctrl.chessground.state.pieces, moveStyle.get()) || ''),
  },
  {
    cmd: 'p',
    help: commands().piece.help,
    cb: ({ ctrl, notify, moveStyle }, input) =>
      notify.set(
        commands().piece.apply(input, ctrl.chessground.state.pieces, moveStyle.get()) ||
          `Bad input: ${input}. Exptected format: ${commands().piece.help}`,
      ),
  },
  {
    cmd: 's',
    help: commands().scan.help,
    cb: ({ ctrl, notify, moveStyle }, input) =>
      notify.set(
        commands().scan.apply(input, ctrl.chessground.state.pieces, moveStyle.get()) ||
          `Bad input: ${input}. Exptected format: ${commands().scan.help}`,
      ),
  },
  {
    cmd: 'eval',
    help: noTrans("announce last move's computer evaluation"),
    cb: ({ ctrl, notify }) => notify.set(renderEvalAndDepth(ctrl)),
  },
  {
    cmd: 'best',
    help: noTrans('announce the top engine move'),
    cb: ctx => ctx.notify.set(renderBestMove(ctx)),
  },
  {
    cmd: 'prev',
    help: noTrans('return to the previous move'),
    cb: ({ ctrl }) => doAndRedraw(ctrl, prev),
  },
  { cmd: 'next', help: noTrans('go to the next move'), cb: ({ ctrl }) => doAndRedraw(ctrl, next) },
  {
    cmd: 'prev line',
    help: noTrans('switch to the previous variation'),
    cb: ({ ctrl }) => doAndRedraw(ctrl, jumpPrevLine),
  },
  {
    cmd: 'next line',
    help: noTrans('switch to the next variation'),
    cb: ({ ctrl }) => doAndRedraw(ctrl, jumpNextLine),
  },
  {
    cmd: 'pocket',
    help: noTrans('Read out pockets for white or black. Example: "pocket black"'),
    cb: ({ ctrl, notify }, input) => {
      const pockets = ctrl.node.crazy?.pockets;
      const color = input.split(' ')?.[1]?.trim();
      return notify.set(
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
    inputCommands.find(c => c.cmd === input.toLowerCase()) ||
    inputCommands.find(c => split.length !== 1 && c.cmd === firstWordLowerCase)
  ); // 'next line' should not be interpreted as 'next'
};

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

function renderAcpl({ ctrl, moveStyle }: AnalyseNvuiContext): LooseVNodes {
  const analysis = ctrl.data.analysis;
  if (!analysis || ctrl.retro) return undefined;
  const analysisGlyphs = ['?!', '?', '??'];
  const analysisNodes = ctrl.mainline.filter(n => n.glyphs?.find(g => analysisGlyphs.includes(g.symbol)));
  const res: Array<VNode> = [];
  COLORS.forEach(color => {
    res.push(hl('h3', `${color} player: ${analysis[color].acpl} ${i18n.site.averageCentipawnLoss}`));
    res.push(
      hl(
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
            hl(
              'option',
              { attrs: { value: node.ply, selected: node.ply === ctrl.node.ply } },
              [
                plyToTurn(node.ply),
                renderSan(node.san!, node.uci, moveStyle.get()),
                renderComments(node, moveStyle.get()),
              ].join(' '),
            ),
          ),
      ),
    );
  });
  return res;
}

const requestAnalysisBtn = ({ ctrl, notify, analysisInProgress }: AnalyseNvuiContext) => {
  if (ctrl.ongoing || ctrl.synthetic || ctrl.hasFullComputerAnalysis()) return;
  return analysisInProgress()
    ? hl('p', 'Server-side analysis in progress')
    : hl(
        'button.request-analysis',
        clickHook(() =>
          xhrText(`/${ctrl.data.game.id}/request-analysis`, { method: 'post' }).then(
            () => {
              analysisInProgress(true);
              notify.set('Server-side analysis in progress');
            },
            () => notify.set('Cannot run server-side analysis'),
          ),
        ),
        i18n.site.requestAComputerAnalysis,
      );
};

function currentLineIndex(ctrl: AnalyseCtrl): { i: number; of: number } {
  if (ctrl.path === treePath.root) return { i: 1, of: 1 };
  const prevNode = ctrl.tree.parentNode(ctrl.path);
  return {
    i: prevNode.children.findIndex(node => node.id === ctrl.node.id),
    of: prevNode.children.length,
  };
}

function renderLineIndex(ctrl: AnalyseCtrl): string {
  const { i, of } = currentLineIndex(ctrl);
  return of > 1 ? `, line ${i + 1} of ${of} ,` : '';
}

export function renderCurrentNode({
  ctrl,
  moveStyle,
}: Pick<AnalyseNvuiContext, 'ctrl' | 'moveStyle'>): string {
  const node = ctrl.node;
  if (!node.san || !node.uci) return i18n.nvui.gameStart;
  return [
    plyToTurn(node.ply),
    node.ply % 2 === 1 ? i18n.site.white : i18n.site.black,
    renderSan(node.san, node.uci, moveStyle.get()),
    renderLineIndex(ctrl),
    !ctrl.retro && renderComments(node, moveStyle.get()),
  ]
    .filter(x => x)
    .join(' ')
    .trim();
}

const renderPlayer = (ctrl: AnalyseCtrl, player: Player): LooseVNodes =>
  player.ai ? i18n.site.aiNameLevelAiLevel('Stockfish', player.ai) : userHtml(ctrl, player);

function userHtml(ctrl: AnalyseCtrl, player: Player) {
  const d = ctrl.data,
    user = player.user,
    perf = user ? user.perfs[d.game.perf] : null,
    rating = player.rating ? player.rating : perf && perf.rating,
    rd = player.ratingDiff,
    ratingDiff = rd ? (rd > 0 ? '+' + rd : rd < 0 ? '−' + -rd : '') : '';
  const studyPlayers = ctrl.study && renderStudyPlayer(ctrl, player.color);
  return user
    ? hl('span', [
        hl(
          'a',
          { attrs: { href: '/@/' + user.username } },
          user.title ? `${user.title} ${user.username}` : user.username,
        ),
        rating ? ` ${rating}` : ``,
        ' ' + ratingDiff,
      ])
    : studyPlayers || hl('span', i18n.site.anonymous);
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
    hl(
      'span',
      keys
        .reduce<string[]>(
          (strs, [key, i18n]) =>
            player[key] ? strs.concat(`${i18n}: ${key === 'fed' ? player[key].name : player[key]}`) : strs,
          [],
        )
        .join(' '),
    )
  );
}

const playerByColor = (d: AnalyseData, color: Color): Player =>
  color === d.player.color ? d.player : d.opponent;

const jumpNextLine = (ctrl: AnalyseCtrl) => jumpLine(ctrl, 1);
const jumpPrevLine = (ctrl: AnalyseCtrl) => jumpLine(ctrl, -1);

function jumpLine(ctrl: AnalyseCtrl, delta: number) {
  const { i, of } = currentLineIndex(ctrl);
  if (of === 1) return;
  const newI = (i + delta + of) % of;
  const prevPath = treePath.init(ctrl.path);
  const prevNode = ctrl.tree.nodeAtPath(prevPath);
  const newPath = prevPath + prevNode.children[newI].id;
  ctrl.userJumpIfCan(newPath);
}

const redirectToSelectedHook = bind('change', (e: InputEvent) => {
  const target = e.target as HTMLSelectElement;
  const selectedOption = target.options[target.selectedIndex];
  const url = selectedOption.getAttribute('url');
  if (url) window.location.href = url;
});

function tourDetails({ ctrl, deps }: AnalyseNvuiContext): VNode[] {
  const ctx: RelayViewContext = { ...viewContext(ctrl, deps), allowVideo: false } as RelayViewContext;
  const tour = ctx.relay.data.tour;
  ctx.relay.redraw = ctrl.redraw;

  return [
    hl('h1', 'Tour details'),
    hl('h2', 'Overview'),
    hl('div', tourOverview(tour.info, tour.dates)),
    hl('h2', 'Players'),
    hl(
      'button.tournament-players',
      clickHook(() => ctx.relay.tab('players'), ctrl.redraw),
      'Load player list',
    ),
    hl('div', ctx.relay.tab() === 'players' && playersView(ctx.relay.players)),
  ];
}

function studyDetails(ctrl: AnalyseCtrl) {
  const study = ctrl.study;
  const relayGroups = study?.relay?.data.group;
  const relayRounds = study?.relay?.data.rounds;
  const tour = study?.relay?.data.tour;
  const hash = window.location.hash;
  return (
    study &&
    hl('div.study-details', [
      hl('h2', 'Study details'),
      hl('span', `Title: ${study.data.name}. By: ${study.data.ownerId}`),
      hl('br'),
      relayGroups &&
        hl(
          'div.relay-groups',
          hl('label', [
            'Current group:',
            hl(
              'select',
              {
                attrs: { autofocus: hash === '#group-select' },
                hook: redirectToSelectedHook,
              },
              relayGroups.tours.map(t =>
                hl(
                  'option',
                  { attrs: { selected: t.id === tour?.id, url: `/broadcast/-/${t.id}#group-select` } },
                  t.name,
                ),
              ),
            ),
          ]),
        ),
      tour &&
        relayRounds &&
        hl(
          'div.relay-rounds',
          hl('label', [
            'Current round:',
            hl(
              'select',
              {
                attrs: { autofocus: hash === '#round-select' },
                hook: redirectToSelectedHook,
              },
              relayRounds.map(r =>
                hl(
                  'option',
                  {
                    attrs: {
                      selected: r.id === study.data.id,
                      url: `/broadcast/${tour.slug}/${r.slug}/${r.id}#round-select`,
                    },
                  },
                  study.relay?.round.name,
                ),
              ),
            ),
          ]),
        ),
      hl('div.chapters', [
        hl('label', [
          'Current chapter:',
          hl(
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
            study.chapters.list
              .all()
              .map((ch, i) =>
                hl(
                  'option',
                  { attrs: { selected: ch.id === study.currentChapter().id, chapterId: ch.id } },
                  `${i + 1}. ${ch.name}`,
                ),
              ),
          ),
        ]),
        study.members.canContribute()
          ? hl('div.buttons', [
              hl(
                'button.edit-chapter',
                clickHook(() => study.chapters.editForm.toggle(study.currentChapter()), ctrl.redraw),
                [
                  'Edit current chapter',
                  study.chapters.editForm.current() && chapterEditFormView(study.chapters.editForm),
                ],
              ),
              hl(
                'button.create-chapter',
                clickHook(() => study.chapters.newForm.toggle(), ctrl.redraw),
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

const doAndRedraw = (ctrl: AnalyseCtrl, fn: (ctrl: AnalyseCtrl) => void): void => {
  fn(ctrl);
  ctrl.redraw();
};

function jumpMoveOrLine(ctrl: AnalyseCtrl) {
  return (e: KeyboardEvent) => {
    if (e.key === 'A') doAndRedraw(ctrl, e.altKey ? jumpPrevLine : prev);
    else if (e.key === 'D') doAndRedraw(ctrl, e.altKey ? jumpNextLine : next);
  };
}
