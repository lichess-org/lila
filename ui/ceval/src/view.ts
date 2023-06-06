import * as winningChances from './winningChances';
import * as licon from 'common/licon';
import { stepwiseScroll } from 'common/scroll';
import { bind } from 'common/snabbdom';
import { defined, notNull } from 'common';
import { Eval, ParentCtrl, NodeEvals, EngineType } from './types';
import { h, Hooks, VNode } from 'snabbdom';
import { Position } from 'chessops/chess';
import { lichessRules } from 'chessops/compat';
import { makeSanAndPlay } from 'chessops/san';
import { opposite, parseUci } from 'chessops/util';
import { parseFen, makeBoardFen } from 'chessops/fen';
import { renderEval } from './util';
import { setupPosition } from 'chessops/variant';
import { uciToMove } from 'chessground/util';
import { CevalState } from './worker';
import { toggle, ToggleSettings } from 'common/controls';
import CevalCtrl from './ctrl';

let gaugeLast = 0;
const gaugeTicks: VNode[] = [...Array(8).keys()].map(i =>
  h(i === 3 ? 'tick.zero' : 'tick', { attrs: { style: `height: ${(i + 1) * 12.5}%` } })
);

function longEvalInfo(ctrl: ParentCtrl, evs: NodeEvals): string {
  const ceval = ctrl.ceval,
    state = ceval.getState(),
    trans = ctrl.trans;
  if (!ceval.enabled()) return '';
  if (ctrl.outcome()) return 'Game over';

  if (!ceval.showClientEval()) return trans.noarg('usingServerAnalysis');
  if (!evs.local) {
    if (!ceval.analysable) return 'Engine cannot analyze this position';
    if (state == CevalState.Failed) return trans.noarg('engineFailed');
    return state == CevalState.Loading ? trans.noarg('loadingEngine') : trans.noarg('calculatingMoves');
  }

  const depth = evs.local.depth || 0;
  let t: string = evs.local.cloud
    ? trans('depthX', depth)
    : trans('depthX', depth + '/' + Math.max(depth, evs.local.maxDepth));
  if (!evs.local.cloud && evs.local.knps) t += ', ' + Math.round(evs.local.knps) + 'k nodes/s';
  return ceval.longEngineName() + '\n' + t;
}

function shortEvalInfo(ctrl: ParentCtrl, evs: NodeEvals): Array<VNode | string> {
  const ceval = ctrl.ceval,
    state = ceval.getState(),
    trans = ctrl.trans;

  if (!ceval.enabled()) return [];
  if (ctrl.outcome()) return ['Game over'];

  if (!ceval.showClientEval()) {
    if (evs.server) return ['Server Analysis'];
    return ['Unavailable'];
  }

  if (!evs.local) {
    if (state === CevalState.Failed) return ['Failed'];
    const mb = ceval.downloadProgress() / 1024 / 1024;
    if (state !== CevalState.Loading || mb < 1) return ['Loading...'];
    const localEvalText = mb.toFixed(1) + ' MiB';
    return [evs.server && ctrl.nextNodeBest() ? trans.noarg('server') : localEvalText];
  }

  const depth = evs.local.depth || 0;
  const t: Array<VNode | string> = evs.local.cloud
    ? [trans('depthX', depth)]
    : [trans('depthX', depth + '/' + Math.max(depth, evs.local.maxDepth))];
  t.push(
    h('a.pause', {
      attrs: {
        hidden: !ceval.canContinue(),
        tabindex: 0,
        title: trans.noarg('goDeeper'),
        'data-icon': '',
      },
      hook: bind('click', ceval.continue),
    })
  );
  t.push(
    h('a.pause', {
      attrs: {
        hidden: !ceval.canPause(),
        tabindex: 0,
        title: 'Pause',
        'data-icon': licon.PlusButton,
      },
      hook: bind('click', ceval.stop),
    })
  );
  return t;
}

function longThreatInfo(ctrl: ParentCtrl, threat?: Tree.LocalEval | false): string {
  if (!threat) return ctrl.trans.noarg('calculatingMoves');
  let t = ctrl.trans('depthX', (threat.depth || 0) + '/' + threat.maxDepth);
  if (threat.knps) t += ', ' + Math.round(threat.knps) + 'k nodes/s';
  return ctrl.ceval.longEngineName() + '\n' + t;
}

function shortThreatInfo(ctrl: ParentCtrl, threat?: Tree.LocalEval | false): string {
  if (!threat) return ctrl.trans.noarg('calculatingMoves');
  return ctrl.trans('depthX', (threat.depth || 0) + '/' + threat.maxDepth);
}

const serverNodes = 4e6;

export function getBestEval(ctrl: CevalCtrl | undefined, evs: NodeEvals): Eval | undefined {
  if (!ctrl) return;

  const serverEv = evs.server,
    localEv = evs.local,
    type = ctrl.getFallbackType();

  if (!ctrl.showClientEval()) return serverEv;
  if (type === 'disabled') return localEv;

  if (!serverEv) return localEv;
  if (!localEv && type === 'overwrite') return serverEv;
  if (!localEv) return;

  // Prefer localEv if it exceeds fishnet node limit or finds a better mate.
  if (
    localEv.nodes > serverNodes ||
    (typeof localEv.mate !== 'undefined' &&
      (typeof serverEv.mate === 'undefined' || Math.abs(localEv.mate) < Math.abs(serverEv.mate)))
  )
    return localEv;

  return serverEv;
}

export function renderGauge(ctrl: ParentCtrl): VNode | undefined {
  if (ctrl.ongoing || !ctrl.showEvalGauge()) return;
  const bestEv = getBestEval(ctrl.ceval, ctrl.currentEvals());
  let ev;
  if (bestEv) {
    ev = winningChances.povChances('white', bestEv);
    gaugeLast = ev;
  } else if (ctrl.ceval.showClientEval()) {
    ev = gaugeLast;
  } else {
    return;
  }
  return h(
    'div.eval-gauge',
    {
      class: {
        empty: ev === null,
        reverse: ctrl.getOrientation() === 'black',
      },
    },
    [h('div.black', { attrs: { style: `height: ${100 - (ev + 1) * 50}%` } }), ...gaugeTicks]
  );
}

export function renderEngineSelect(sel: string, ctrl: ParentCtrl): VNode {
  const ceval = ctrl.ceval;

  const options: Array<{ value: string; name: string }> = [];

  if (!ctrl.mandatoryCeval()) options.push({ value: 'disabled', name: 'Analysis disabled' });

  if (ceval.analysable) {
    if (ctrl.showServerAnalysis) options.push({ value: 'server', name: 'Stockfish 15.1 (Server)' });
    options.push({ value: 'local', name: ceval.localEngineName() + ' (Local)' });
  }

  const type = ceval.getEngineType();
  const engines = ceval.externalEngines();

  const nodes: VNode[] = options.map(({ value, name }) => h('option', { attrs: { value } }, [name]));

  if (engines.length !== 0)
    nodes.push(
      h(
        'optgroup',
        { attrs: { label: 'External Engines' } },
        engines.map(({ type: value, name, disabled }) =>
          h(
            'option',
            {
              attrs: {
                value,
                disabled,
              },
            },
            [name + ' (External)']
          )
        )
      )
    );

  return h(
    sel,
    {
      attrs: {
        disabled: !ceval.analysable,
      },
      hook: {
        insert: ({ elm }) => {
          if (!elm) return;
          const e = elm as HTMLSelectElement;
          elm.addEventListener('change', () => ceval.setEngineType(e.value as EngineType));
          e.value = type;
        },
        postpatch: (_, { elm }) => elm && ((elm as HTMLSelectElement).value = type),
      },
    },
    nodes
  );
}

export function renderCeval(ctrl: ParentCtrl): VNode | undefined {
  const instance = ctrl.ceval;
  if (!instance.allowed() || !instance.possible) return;
  const enabled = instance.enabled(),
    evs = ctrl.currentEvals(),
    threatMode = instance.threatMode(),
    threat = threatMode && ctrl.getNode().threat,
    bestEv = threat || getBestEval(instance, evs);
  if (!enabled) instance.actionMenu(false);

  let pearl: VNode | string, percent: number;
  let shortEval = true;

  if (!enabled) {
    pearl = '';
    shortEval = false;
    percent = 0;
  } else if (bestEv && bestEv.cp !== undefined) {
    pearl = renderEval(bestEv.cp);
    percent = evs.local
      ? evs.local.cloud
        ? 100
        : Math.min(100, Math.round((100 * evs.local.depth) / evs.local.maxDepth))
      : 0;
  } else if (bestEv && bestEv.mate !== undefined) {
    pearl = '#' + bestEv.mate;
    percent = 100;
  } else {
    if (ctrl.outcome() || ctrl.getNode().threefold) pearl = '-';
    else if (instance.getState() === CevalState.Failed)
      pearl = h('i.is-red', { attrs: { 'data-icon': licon.CautionCircle } });
    else if (!instance.showClientEval()) pearl = '? ?';
    else {
      shortEval = instance.getState() === CevalState.Loading;
      pearl = h('i.ddloader');
    }
    percent = 0;
  }
  if (threatMode) {
    if (threat) percent = Math.min(100, Math.round((100 * threat.depth) / threat.maxDepth));
    else percent = 0;
  }

  const progressBar = instance.showClientEval()
    ? h(
        'div.bar',
        h('span', {
          class: { threat: threatMode },
          attrs: { style: `width: ${percent}%` },
          hook: {
            postpatch: (old, vnode) => {
              if (old.data!.percent > percent || !!old.data!.threatMode != threatMode) {
                const el = vnode.elm as HTMLElement;
                const p = el.parentNode as HTMLElement;
                p.removeChild(el);
                p.appendChild(el);
              }
              vnode.data!.percent = percent;
              vnode.data!.threatMode = threatMode;
            },
          },
        })
      )
    : null;

  const pearlNode = threatMode
    ? h('div.pearl', { attrs: { title: longThreatInfo(ctrl, threat) } }, [
        pearl,
        h('span.pearl-eval', shortThreatInfo(ctrl, threat)),
      ])
    : h('div.pearl', { attrs: { title: longEvalInfo(ctrl, evs) } }, [
        pearl,
        shortEval ? h('span.pearl-eval', shortEvalInfo(ctrl, evs)) : undefined,
      ]);

  const configButton = h('button.fbt.config-engine', {
    attrs: {
      'data-icon': licon.Gear,
      title: 'Configure engine',
      disabled: !enabled,
    },
    hook: bind('click', instance.actionMenu.toggle, ctrl.redraw),
    class: {
      active: enabled && instance.actionMenu(),
    },
  });

  const config = !instance.actionMenu() ? undefined : h('div.action-menu.engine-config', renderEngineConfig(ctrl));

  return h(
    'div.ceval',
    {
      class: {
        enabled,
        computing: percent < 100 && instance.getState() === CevalState.Computing,
      },
    },
    [progressBar, pearlNode, renderEngineSelect('select.engine-choice', ctrl), configButton, config]
  );
}

function getElFen(el: HTMLElement): string {
  return el.getAttribute('data-fen')!;
}

function getElUci(e: TouchEvent | MouseEvent): string | undefined {
  return (
    $(e.target as HTMLElement)
      .closest('div.pv')
      .attr('data-uci') || undefined
  );
}

function getElUciList(e: TouchEvent | MouseEvent): string[] {
  return getElPvMoves(e)
    .filter(notNull)
    .map(move => move.split('|')[1]);
}

function getElPvMoves(e: TouchEvent | MouseEvent): (string | null)[] {
  const pvMoves: (string | null)[] = [];

  $(e.target as HTMLElement)
    .closest('div.pv')
    .children()
    .filter('span.pv-san')
    .each(function (this: HTMLElement) {
      pvMoves.push($(this).attr('data-board'));
    });

  return pvMoves;
}

function checkHover(el: HTMLElement, instance: CevalCtrl): void {
  lichess.requestIdleCallback(
    () => instance.setHovering(getElFen(el), $(el).find('div.pv:hover').attr('data-uci') || undefined),
    500
  );
}

export function renderPvs(ctrl: ParentCtrl): VNode | undefined {
  const instance = ctrl.ceval;
  if (!instance.allowed() || !instance.possible || !instance.enabled()) return;
  const multiPv = instance.multiPv(),
    node = ctrl.getNode(),
    setup = parseFen(node.fen).unwrap();
  let pvs: Tree.PvData[],
    threat = false,
    pvMoves: (string | null)[],
    pvIndex: number | null;
  if (instance.threatMode() && node.threat) {
    pvs = node.threat.pvs;
    threat = true;
  } else if (node.ceval) pvs = node.ceval.pvs;
  else pvs = [];
  if (threat) {
    setup.turn = opposite(setup.turn);
    if (setup.turn == 'white') setup.fullmoves += 1;
  }
  const pos = setupPosition(lichessRules(instance.opts.variant.key), setup);

  return h(
    'div.pv_box',
    {
      attrs: { 'data-fen': node.fen },
      hook: {
        insert: vnode => {
          const el = vnode.elm as HTMLElement;
          el.addEventListener('mouseover', (e: MouseEvent) => {
            const instance = ctrl.ceval;
            instance.setHovering(getElFen(el), getElUci(e));
            const pvBoard = (e.target as HTMLElement).dataset.board;
            if (pvBoard) {
              pvIndex = Number((e.target as HTMLElement).dataset.moveIndex);
              pvMoves = getElPvMoves(e);
              const [fen, uci] = pvBoard.split('|');
              instance.setPvBoard({ fen, uci });
            }
          });
          el.addEventListener(
            'wheel',
            stepwiseScroll((e: WheelEvent, scroll: boolean) => {
              e.preventDefault();
              if (pvIndex != null && pvMoves != null) {
                if (e.deltaY < 0 && pvIndex > 0 && scroll) pvIndex -= 1;
                else if (e.deltaY > 0 && pvIndex < pvMoves.length - 1 && scroll) pvIndex += 1;
                const pvBoard = pvMoves[pvIndex];
                if (pvBoard) {
                  const [fen, uci] = pvBoard.split('|');
                  ctrl.ceval.setPvBoard({ fen, uci });
                }
              }
            })
          );
          el.addEventListener('mouseout', () => ctrl.ceval.setHovering(getElFen(el)));
          for (const event of ['touchstart', 'mousedown']) {
            el.addEventListener(event, (e: TouchEvent | MouseEvent) => {
              const uciList = getElUciList(e);
              if (uciList.length > (pvIndex ?? 0) && !instance.threatMode()) {
                ctrl.playUciList(uciList.slice(0, (pvIndex ?? 0) + 1));
                e.preventDefault();
              }
            });
          }
          el.addEventListener('mouseleave', () => {
            ctrl.ceval.setPvBoard(null);
            pvIndex = null;
          });
          checkHover(el, instance);
        },
        postpatch: (_, vnode) => checkHover(vnode.elm as HTMLElement, instance),
      },
    },
    [
      ...[...Array(multiPv).keys()].map(i => renderPv(threat, multiPv, pvs[i], pos.isOk ? pos.value : undefined)),
      renderPvBoard(ctrl),
    ]
  );
}

const MAX_NUM_MOVES = 16;

function renderPv(threat: boolean, multiPv: number, pv?: Tree.PvData, pos?: Position): VNode {
  const data: any = {};
  const children: VNode[] = [renderPvWrapToggle()];
  if (pv) {
    if (!threat) {
      data.attrs = { 'data-uci': pv.moves[0] };
    }
    if (multiPv > 1) {
      children.push(h('strong', defined(pv.mate) ? '#' + pv.mate : renderEval(pv.cp!)));
    }
    if (pos) {
      children.push(...renderPvMoves(pos.clone(), pv.moves.slice(0, MAX_NUM_MOVES)));
    }
  }
  return h('div.pv.pv--nowrap', data, children);
}

function renderPvWrapToggle(): VNode {
  return h('span.pv-wrap-toggle', {
    hook: {
      insert: (vnode: VNode) => {
        const el = vnode.elm as HTMLElement;
        for (const event of ['touchstart', 'mousedown']) {
          el.addEventListener(event, (e: Event) => {
            e.stopPropagation();
            e.preventDefault();
            $(el).closest('.pv').toggleClass('pv--nowrap');
          });
        }
      },
    },
  });
}

function renderPvMoves(pos: Position, pv: Uci[]): VNode[] {
  const vnodes: VNode[] = [];
  let key = makeBoardFen(pos.board);
  for (let i = 0; i < pv.length; i++) {
    let text;
    if (pos.turn === 'white') {
      text = `${pos.fullmoves}.`;
    } else if (i === 0) {
      text = `${pos.fullmoves}...`;
    }
    if (text) {
      vnodes.push(h('span', { key: text }, text));
    }
    const uci = pv[i];
    const san = makeSanAndPlay(pos, parseUci(uci)!);
    const fen = makeBoardFen(pos.board); // Chessground uses only board fen
    if (san === '--') {
      break;
    }
    key += '|' + uci;
    vnodes.push(
      h(
        'span.pv-san',
        {
          key,
          attrs: {
            'data-move-index': i,
            'data-board': `${fen}|${uci}`,
          },
        },
        san
      )
    );
  }
  return vnodes;
}

function renderPvBoard(ctrl: ParentCtrl): VNode | undefined {
  const instance = ctrl.ceval;
  const pvBoard = instance.pvBoard();
  if (!pvBoard) {
    return;
  }
  const { fen, uci } = pvBoard;
  const orientation = ctrl.getOrientation();
  const cgConfig = {
    fen,
    lastMove: uciToMove(uci),
    orientation,
    coordinates: false,
    viewOnly: true,
    drawable: {
      enabled: false,
      visible: false,
    },
  };
  const cgVNode = h('div.cg-wrap.is2d', {
    hook: {
      insert: (vnode: any) => (vnode.elm._cg = window.Chessground(vnode.elm, cgConfig)),
      update: (vnode: any) => vnode.elm._cg.set(cgConfig),
      destroy: (vnode: any) => vnode.elm._cg.destroy(),
    },
  });
  return h('div.pv-board', h('div.pv-board-square', cgVNode));
}

const ctrlToggle = (ctrl: ParentCtrl, t: ToggleSettings) => toggle(t, ctrl.trans, ctrl.redraw);

const rangeConfig = (read: () => number, write: (value: number) => void): Hooks => ({
  insert: vnode => {
    const el = vnode.elm as HTMLInputElement;
    el.value = '' + read();
    el.addEventListener('input', _ => write(parseInt(el.value)));
    el.addEventListener('mouseout', _ => el.blur());
  },
});

const formatHashSize = (v: number): string => (v < 1024 ? v + ' MiB' : Math.round(v / 1024) + ' GiB');

export function renderEngineConfig(ctrl: ParentCtrl): Array<VNode | undefined> {
  const noarg = ctrl.trans.noarg,
    ceval = ctrl.ceval,
    type = ceval.getEngineType(),
    fallbackType = ceval.getFallbackType();

  if (type === 'disabled') return [];

  const notSupported = 'Engine does not support this option';

  if (type === 'server' && !ctrl.hasServerEval!()) {
    if (!ctrl.canRequestServerEval?.()) return [h('p.config-study-message', noarg('theChapterIsTooShortToBeAnalysed'))];
    return [
      !ctrl.isStudy
        ? undefined
        : h('p.config-study-message', [
            noarg('getAFullComputerAnalysis'),
            h('br'),
            noarg('makeSureTheChapterIsComplete'),
          ]),
      h(
        'button.button.text.setting',
        {
          attrs: {
            'data-icon': '\ue004',
          },
          hook: bind('click', ctrl.requestServerEval!.bind(ctrl), ctrl.redraw),
        },
        [ctrl.trans.noarg('requestAComputerAnalysis')]
      ),
    ];
  }

  const maxThreads = ceval.platform.maxThreads();

  return [
    type === 'server'
      ? undefined
      : ctrlToggle(ctrl, {
          name: 'bestMoveArrow',
          title: 'Hotkey: a',
          id: 'shapes',
          checked: ceval.showAutoShapes(),
          change: ceval.toggleAutoShapes,
        }),
    ctrlToggle(ctrl, {
      name: 'evaluationGauge',
      id: 'gauge',
      checked: ceval.showGauge(),
      change: ceval.toggleGauge,
    }),
    type === 'server'
      ? undefined
      : ctrlToggle(ctrl, {
          name: 'infiniteAnalysis',
          title: 'removesTheDepthLimit',
          id: 'infinite',
          checked: ceval.infinite(),
          change: ceval.cevalSetInfinite,
        }),
    type === 'local'
      ? ctrlToggle(ctrl, {
          name: 'showThreat',
          title: 'Hotkey: x',
          id: `showThreat-${ctrl.getNode().ply}-${ctrl.getNode().uci}`, // full redraw on node change
          checked: ceval.threatMode(),
          change: ceval.toggleThreatMode,
          disabled: !!ctrl.getNode().check || ctrl.disableThreatMode(),
        })
      : undefined,
    type.startsWith('external-') || type === 'server'
      ? undefined
      : ctrlToggle(ctrl, {
          name: 'Use NNUE',
          title: ceval.platform.supportsNnue
            ? 'Downloads 6 MB neural network evaluation file (page reload required after change)'
            : notSupported,
          id: 'enable-nnue',
          checked: ceval.platform.supportsNnue && ceval.enableNnue(),
          change: ceval.enableNnue,
          disabled: !ceval.platform.supportsNnue,
        }),
    (() => {
      if (type === 'server') return;
      const id = 'analyse-multipv';
      const max = 5;
      return h('div.setting', [
        h('label', { attrs: { for: id } }, noarg('multipleLines')),
        h('input', {
          attrs: {
            id,
            type: 'range',
            min: 0,
            max,
            step: 1,
          },
          hook: rangeConfig(() => ceval!.multiPv(), ceval.cevalSetMultiPv),
        }),
        h('div.range_value', ceval.multiPv() + ' / ' + max),
      ]);
    })(),
    (() => {
      if (type === 'server') return;
      const id = 'analyse-threads';
      return h('div.setting', [
        h('label', { attrs: { for: id } }, noarg('cpus')),
        h('input', {
          attrs: {
            id,
            type: 'range',
            min: 1,
            max: maxThreads,
            step: 1,
            disabled: maxThreads <= 1,
            ...(maxThreads <= 1 ? { title: notSupported } : null),
          },
          hook: rangeConfig(() => ceval.threads(), ceval.cevalSetThreads),
        }),
        h('div.range_value', `${ceval.threads ? ceval.threads() : 1} / ${maxThreads}`),
      ]);
    })(),
    (() => {
      if (type === 'server') return;
      const id = 'analyse-memory';
      return h('div.setting', [
        h('label', { attrs: { for: id } }, noarg('memory')),
        h('input', {
          attrs: {
            id,
            type: 'range',
            min: 4,
            max: Math.floor(Math.log2(ceval.platform.maxHashSize())),
            step: 1,
            disabled: ceval.platform.maxHashSize() <= 16,
            ...(ceval.platform.maxHashSize() <= 16 ? { title: notSupported } : null),
          },
          hook: rangeConfig(
            () => Math.floor(Math.log2(ceval.hashSize())),
            v => ceval.cevalSetHashSize(Math.pow(2, v))
          ),
        }),
        h('div.range_value', formatHashSize(ceval.hashSize())),
      ]);
    })(),
    type === 'server' || !ctrl.hasServerEval?.()
      ? undefined
      : h('label.setting', [
          'Complement server:',
          h(
            'select',
            {
              hook: bind('change', e => ceval.setFallbackType((e.target as any).value), ctrl.redraw),
            },
            [
              h('option', { attrs: { selected: fallbackType === 'disabled', value: 'disabled' } }, ['Disabled']),
              h('option', { attrs: { selected: fallbackType === 'complement', value: 'complement' } }, [
                'Only on variations',
              ]),
              h('option', { attrs: { selected: fallbackType === 'overwrite', value: 'overwrite' } }, ['Always']),
            ]
          ),
        ]),
    !ceval.useServerEval()
      ? undefined
      : ctrlToggle(ctrl, {
          name: 'Annotations on board',
          title: 'Display analysis symbols on the board',
          id: 'move-annotation',
          checked: ceval.showMoveAnnotation(),
          change: ceval.toggleMoveAnnotation,
        }),
    !ctrl.hasServerEval?.() || (type !== 'server' && fallbackType === 'disabled')
      ? undefined
      : ctrlToggle(ctrl, {
          name: 'Show comments',
          title: 'Shows analysis comments on the score sheet',
          id: 'comments',
          checked: ceval.showServerComments(),
          change: ceval.setShowServerComments.bind(ceval),
        }),
  ];
}
