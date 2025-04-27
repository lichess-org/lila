import { h, type VNode, type VNodeChildren } from 'snabbdom';
import { defined, prop, type Prop } from 'lib';
import { text as xhrText } from 'lib/xhr';
import type AnalyseController from '../ctrl';
import { makeConfig as makeCgConfig } from '../ground';
import type { AnalyseData, NvuiPlugin } from '../interfaces';
import type { Player } from 'lib/game/game';
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
import { bind, onInsert, type MaybeVNode, type MaybeVNodes } from 'lib/snabbdom';
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
import { Chessground as makeChessground } from 'chessground';
import { pubsub } from 'lib/pubsub';
import { renderResult, viewContext, type RelayViewContext } from '../view/components';
import { view as chapterNewFormView } from '../study/chapterNewForm';
import { view as chapterEditFormView } from '../study/chapterEditForm';
import renderClocks from '../view/clocks';

import type * as studyDeps from '../study/studyDeps';
import type RelayCtrl from '../study/relay/relayCtrl';
import { playersView } from '../study/relay/relayPlayers';
import { showInfo as tourOverview } from '../study/relay/relayTourView';

const throttled = (sound: string) => throttle(100, () => site.sound.play(sound));
const selectSound = throttled('select');
const borderSound = throttled('outOfBound');
const errorSound = throttled('error');

export function initModule(ctrl: AnalyseController): NvuiPlugin {
  const notify = new Notify(),
    moveStyle = styleSetting(),
    pieceStyle = pieceSetting(),
    prefixStyle = prefixSetting(),
    positionStyle = positionSetting(),
    boardStyle = boardSetting(),
    analysisInProgress = prop(false);

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
                    hook: bind('click', _ => ctrl.explorer.toggle(), ctrl.redraw),
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
                .map(command => `${command.cmd}: ${command.help}`),
            ].reduce(addBreaks, []),
          ),
          ...(deps && ctrl.study?.relay ? tourDetails(ctrl, ctrl.study, ctrl.study.relay, deps) : []),
        ]),
      ]);
    },
  };
}

function renderEvalAndDepth(ctrl: AnalyseController): string {
  if (ctrl.threatMode()) return `${evalInfo(ctrl.node.threat)} ${depthInfo(ctrl.node.threat, false)}`;
  const evs = ctrl.currentEvals(),
    bestEv = cevalView.getBestEval(evs);
  const evalStr = evalInfo(bestEv);
  return !evalStr ? noEvalStr(ctrl.ceval) : `${evalStr} ${depthInfo(evs.client, !!evs.client?.cloud)}`;
}

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

function renderBestMove(ctrl: AnalyseController, style: MoveStyle): string {
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

function renderAriaResult(ctrl: AnalyseController): VNode[] {
  const result = renderResult(ctrl);
  const res = result.length ? result : 'No result';
  return [
    h('h2', 'Game status'),
    h('div.status', { attrs: { role: 'status', 'aria-live': 'assertive', 'aria-atomic': 'true' } }, res),
  ];
}

function renderCurrentLine(ctrl: AnalyseController, style: MoveStyle): VNodeChildren {
  if (ctrl.path.length === 0) return renderMainline(ctrl.mainline, ctrl.path, style);
  else {
    const futureNodes = ctrl.node.children.length > 0 ? ops.mainlineNodeList(ctrl.node.children[0]) : [];
    return renderMainline(ctrl.nodeList.concat(futureNodes), ctrl.path, style);
  }
}

function onSubmit(
  ctrl: AnalyseController,
  notify: (txt: string) => void,
  style: () => MoveStyle,
  $input: Cash,
) {
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

type Command = 'p' | 's' | 'eval' | 'best' | 'prev' | 'next' | 'prev line' | 'next line' | 'pocket';
type InputCommand = {
  cmd: Command;
  help: string;
  cb: (ctrl: AnalyseController, notify: (txt: string) => void, style: MoveStyle, input: string) => void;
  invalid?: (ctrl: AnalyseController) => boolean;
};

const inputCommands: InputCommand[] = [
  {
    cmd: 'p',
    help: commands.piece.help,
    cb: (ctrl, notify, style, input) =>
      notify(
        commands.piece.apply(input, ctrl.chessground.state.pieces, style) ||
          `Bad input: ${input}. Exptected format: ${commands.piece.help}`,
      ),
  },
  {
    cmd: 's',
    help: commands.scan.help,
    cb: (ctrl, notify, style, input) =>
      notify(
        commands.scan.apply(input, ctrl.chessground.state.pieces, style) ||
          `Bad input: ${input}. Exptected format: ${commands.scan.help}`,
      ),
  },
  {
    cmd: 'eval',
    help: "announce last move's computer evaluation",
    cb: (ctrl, notify) => notify(renderEvalAndDepth(ctrl)),
  },
  {
    cmd: 'best',
    help: 'announce the top engine move',
    cb: (ctrl, notify, style) => notify(renderBestMove(ctrl, style)),
  },
  {
    cmd: 'prev',
    help: 'return to the previous move',
    cb: ctrl => doAndRedraw(ctrl, prev),
  },
  { cmd: 'next', help: 'go to the next move', cb: ctrl => doAndRedraw(ctrl, next) },
  {
    cmd: 'prev line',
    help: 'switch to the previous variation',
    cb: ctrl => doAndRedraw(ctrl, jumpPrevLine),
  },
  {
    cmd: 'next line',
    help: 'switch to the next variation',
    cb: ctrl => doAndRedraw(ctrl, jumpNextLine),
  },
  {
    cmd: 'pocket',
    help: 'Read out pockets for white or black. Example: "pocket black"',
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

function sendMove(uciOrDrop: string | DropMove, ctrl: AnalyseController) {
  if (typeof uciOrDrop === 'string')
    ctrl.sendMove(
      uciOrDrop.slice(0, 2) as Key,
      uciOrDrop.slice(2, 4) as Key,
      undefined,
      charToRole(uciOrDrop.slice(4)),
    );
  else if (ctrl.crazyValid(uciOrDrop.role, uciOrDrop.key)) ctrl.sendNewPiece(uciOrDrop.role, uciOrDrop.key);
}

function renderAcpl(ctrl: AnalyseController, style: MoveStyle): MaybeVNodes | undefined {
  const anal = ctrl.data.analysis; // heh
  if (!anal) return undefined;
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
  return res;
}

const requestAnalysisButton = (
  ctrl: AnalyseController,
  inProgress: Prop<boolean>,
  notify: (msg: string) => void,
): MaybeVNode =>
  ctrl.ongoing || ctrl.synthetic
    ? undefined
    : inProgress()
      ? h('p', 'Server-side analysis in progress')
      : h(
          'button',
          {
            hook: bind('click', _ =>
              xhrText(`/${ctrl.data.game.id}/request-analysis`, { method: 'post' }).then(
                () => {
                  inProgress(true);
                  notify('Server-side analysis in progress');
                },
                () => notify('Cannot run server-side analysis'),
              ),
            ),
          },
          i18n.site.requestAComputerAnalysis,
        );

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

function renderCurrentNode(ctrl: AnalyseController, style: MoveStyle): string {
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

const renderPlayer = (ctrl: AnalyseController, player: Player): VNodeChildren =>
  player.ai ? i18n.site.aiNameLevelAiLevel('Stockfish', player.ai) : userHtml(ctrl, player);

function userHtml(ctrl: AnalyseController, player: Player) {
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

function renderStudyPlayer(ctrl: AnalyseController, color: Color): VNode | undefined {
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

const playerByColor = (d: AnalyseData, color: Color): Player =>
  color === d.player.color ? d.player : d.opponent;

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
const onInsertHandler = (callback: () => void, el: HTMLElement) => {
  el.addEventListener('click', callback);
  el.addEventListener('keydown', ev => ev.key === 'Enter' && callback());
};

const redirectToSelectedHook = bind('change', (e: InputEvent) => {
  const target = e.target as HTMLSelectElement;
  const selectedOption = target.options[target.selectedIndex];
  const url = selectedOption.getAttribute('url');
  if (url) window.location.href = url;
});

function tourDetails(
  ctrl: AnalyseController,
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

function studyDetails(ctrl: AnalyseController): MaybeVNode {
  const study = ctrl.study;
  const relayGroups = study?.relay?.data.group;
  const relayRounds = study?.relay?.data.rounds;
  const tour = study?.relay?.data.tour;
  const hash = window.location.hash;
  return (
    study &&
    h('div.study-details', [
      h('h2', 'Study details'),
      h('span', `Title: ${study.data.name}. By: ${study.data.ownerId}`),
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

const doAndRedraw = (ctrl: AnalyseController, fn: (ctrl: AnalyseController) => void): void => {
  fn(ctrl);
  ctrl.redraw();
};

function jumpMoveOrLine(ctrl: AnalyseController) {
  return (e: KeyboardEvent) => {
    if (e.key === 'A') doAndRedraw(ctrl, e.altKey ? jumpPrevLine : prev);
    else if (e.key === 'D') doAndRedraw(ctrl, e.altKey ? jumpNextLine : next);
  };
}
