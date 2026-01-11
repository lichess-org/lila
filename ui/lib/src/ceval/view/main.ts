/* eslint no-restricted-syntax:"error" */ // no side effects allowed due to re-export by index.ts

import { povChances } from '../winningChances';
import * as licon from '@/licon';
import { stepwiseScroll, type VNode, type LooseVNodes, onInsert, bind, hl } from '@/view';
import { defined, notNull, requestIdleCallback } from '@/index';
import { type CevalHandler, type NodeEvals, CevalState } from '../types';
import type { Position } from 'chessops/chess';
import { lichessRules } from 'chessops/compat';
import { makeSanAndPlay } from 'chessops/san';
import { opposite, parseUci } from 'chessops/util';
import { parseFen, makeBoardFen } from 'chessops/fen';
import { renderEval } from '../util';
import { setupPosition } from 'chessops/variant';
import { uciToMove } from '@lichess-org/chessground/util';
import { renderCevalSettings } from './settings';
import type CevalCtrl from '../ctrl';
import { Chessground as makeChessground } from '@lichess-org/chessground';
import { isTouchDevice } from '@/device';

type EvalInfo = { knps: number; npsText: string; depthText: string };

function localEvalNodes(ctrl: CevalHandler, evs: NodeEvals): Array<VNode | string> {
  const ceval = ctrl.ceval,
    state = ceval.state,
    status = ceval.opts.custom?.statusNode?.();
  if (status) return [status];
  if (!evs.client) {
    if (!ceval.analysable) return ['Engine cannot analyze this position'];
    if (state === CevalState.Failed) return [i18n.site.engineFailed];
    const localEvalText = state === CevalState.Loading ? loadingText(ctrl) : i18n.site.calculatingMoves;
    return [evs.server && ctrl.nextNodeBest() ? i18n.site.usingServerAnalysis : localEvalText];
  }
  const t: Array<VNode | string> = [];
  if (!ceval.opts.custom && ceval.canGoDeeper)
    t.push(
      hl('a.deeper', {
        attrs: { title: i18n.site.goDeeper, 'data-icon': licon.PlusButton },
        hook: bind('click', ceval.goDeeper),
      }),
    );
  const { depthText, npsText } = localInfo(ctrl, evs.client);

  t.push(depthText);
  if (evs.client.cloud && !ceval.isComputing)
    t.push(hl('span.cloud', { attrs: { title: i18n.site.cloudAnalysis } }, 'Cloud'));
  if (ceval.isInfinite) t.push(hl('span.infinite', { attrs: { title: i18n.site.infiniteAnalysis } }, '∞'));
  if (npsText) t.push(' · ' + npsText);
  return t;
}

function threatInfo(ctrl: CevalHandler, threat?: Tree.LocalEval | false): string {
  const info = localInfo(ctrl, threat);
  return info.depthText + (info.knps ? ' · ' + info.npsText : '');
}

function localInfo(ctrl: CevalHandler, ev?: Tree.ClientEval | false): EvalInfo {
  const info = {
    npsText: '',
    knps: 0,
    depthText: i18n.site.calculatingMoves,
  };

  if (!ev) return info;

  const ceval = ctrl.ceval;
  info.depthText = i18n.site.depthX(ev.depth || 0) + (ceval.isDeeper() || ceval.isInfinite ? '/99' : '');

  if (!ceval.isComputing) return info;

  const knps = ev.nodes / (ev?.millis ?? Number.POSITIVE_INFINITY);

  if (knps > 0) {
    info.npsText = `${
      knps > 1000 ? (knps / 1000).toFixed(knps > 10000 ? 0 : 1) + ' Mn/s' : Math.round(knps) + ' kn/s'
    }`;
    info.knps = knps;
  }
  return info;
}

const threatButton = (ctrl: CevalHandler): VNode | null =>
  ctrl.ceval.download
    ? null
    : hl('button.show-threat', {
        class: { active: ctrl.threatMode(), hidden: !!ctrl.getNode().check },
        attrs: { 'data-icon': licon.Target, title: i18n.site.showThreat + ' (x)' },
        hook: bind('click', () => ctrl.toggleThreatMode()),
      });

function engineName(ctrl: CevalCtrl): VNode[] {
  const engine = ctrl.engines.active;
  return engine
    ? [
        hl('span', { attrs: { title: engine.name } }, engine.short ?? engine.name),
        ctrl.engines.isExternalEngineInfo(engine)
          ? hl(
              'span.technology.good',
              { attrs: { title: 'Engine running outside of the browser' } },
              engine.tech,
            )
          : engine.requires.includes('simd')
            ? hl(
                'span.technology.good',
                { attrs: { title: 'Multi-threaded WebAssembly with SIMD' } },
                engine.tech,
              )
            : engine.requires.includes('sharedMem')
              ? hl('span.technology.good', { attrs: { title: 'Multi-threaded WebAssembly' } }, engine.tech)
              : engine.requires.includes('wasm')
                ? hl('span.technology', { attrs: { title: 'Single-threaded WebAssembly' } }, engine.tech)
                : hl('span.technology', { attrs: { title: 'Single-threaded JavaScript' } }, engine.tech),
      ]
    : [];
}

export const getBestEval = (ctrl: CevalHandler): EvalScore | undefined => {
  return ctrl.getNode().ceval ?? (ctrl.showFishnetAnalysis?.() ? ctrl.getNode().eval : undefined);
};

let gaugeLast = 0;
let gaugeTicks: VNode[];

export function renderGauge(ctrl: CevalHandler): VNode | undefined {
  if (ctrl.ongoing || !ctrl.showEvalGauge()) return;
  gaugeTicks ??= [...Array(8).keys()].map(i =>
    hl(i === 3 ? 'tick.zero' : 'tick', { attrs: { style: `height: ${(i + 1) * 12.5}%` } }),
  );
  const bestEv = getBestEval(ctrl);
  let ev;
  if (bestEv) {
    ev = povChances('white', bestEv);
    gaugeLast = ev;
  } else ev = gaugeLast;
  return hl(
    'div.eval-gauge',
    { class: { empty: !defined(bestEv), reverse: ctrl.getOrientation() === 'black' } },
    [hl('div.black', { attrs: { style: `height: ${100 - (ev + 1) * 50}%` } }), gaugeTicks],
  );
}

export function renderCeval(ctrl: CevalHandler): VNode[] {
  const ceval = ctrl.ceval;
  const enabled = !ceval.isPaused && ctrl.cevalEnabled(),
    client = ctrl.getNode().ceval,
    server = ctrl.getNode().eval,
    threatMode = ctrl.threatMode(),
    threat = threatMode ? ctrl.getNode().threat : undefined,
    bestEv = threat || getBestEval(ctrl),
    search = ceval.search,
    download = ceval.download;
  let pearl: VNode | undefined,
    percent = 0;

  if (client) {
    if (client.cloud && !threatMode) percent = 100;
    else if (ceval.isDeeper() || ceval.isInfinite) percent = Math.min(100, (100 * client.depth) / 99);
    else if ('movetime' in search.by)
      percent = Math.min(100, (100 * ((threat ?? client)?.millis ?? 0)) / search.by.movetime);
    else if ('depth' in search.by) percent = Math.min(100, (100 * client.depth) / search.by.depth);
    else if ('nodes' in search.by) percent = Math.min(100, (100 * client.nodes) / search.by.nodes);
  }
  if (ceval.opts.custom?.pearlNode) {
    pearl = ceval.opts.custom.pearlNode();
  } else if (bestEv && typeof bestEv.cp !== 'undefined') {
    pearl = hl('pearl', renderEval(bestEv.cp));
  } else if (bestEv && defined(bestEv.mate)) {
    pearl = hl('pearl', '#' + bestEv.mate);
    percent = 100;
  } else {
    if (!enabled) pearl = hl('pearl', hl('i'));
    else if (ctrl.outcome() || ctrl.getNode().threefold) pearl = hl('pearl', '-');
    else if (ceval.state === CevalState.Failed)
      pearl = hl('pearl', hl('i.is-red', { attrs: { 'data-icon': licon.CautionCircle } }));
    else pearl = hl('pearl', hl('i.ddloader'));
    percent = ctrl.outcome() ? 100 : 0;
  }
  if (download) percent = Math.min(100, Math.round((100 * download.bytes) / download.total));
  else if (ceval.search.indeterminate || (percent > 0 && !ceval.isComputing)) percent = 100;

  const progressBar: VNode | undefined =
    (enabled || download) &&
    hl(
      'div.bar',
      hl('span', {
        class: { threat: enabled && threatMode },
        attrs: { style: `width: ${percent}%` },
        hook: {
          postpatch: (old, vnode) => {
            if (old.data!.percent > percent || !!old.data!.threatMode !== threatMode) {
              const el = vnode.elm as HTMLElement;
              const p = el.parentNode as HTMLElement;
              p.removeChild(el);
              p.appendChild(el);
            }
            vnode.data!.percent = percent;
            vnode.data!.threatMode = threatMode;
          },
        },
      }),
    );

  const body: LooseVNodes = enabled
    ? [
        pearl,
        hl('div.engine', [
          threatMode ? [i18n.site.showThreat] : engineName(ceval),
          hl(
            'span.info',
            ctrl.outcome()
              ? [i18n.site.gameOver]
              : ctrl.getNode().threefold
                ? [i18n.site.threefoldRepetition]
                : threatMode
                  ? [threatInfo(ctrl, threat)]
                  : localEvalNodes(ctrl, { client, server }),
          ),
        ]),
      ]
    : [
        pearl,
        hl('div.engine', [
          engineName(ceval),
          hl('br'),
          ceval.analysable ? i18n.site.inLocalBrowser : 'Illegal positions cannot be analyzed',
        ]),
      ];

  const settingsGear = hl('button.settings-gear', {
    attrs: { role: 'button', 'data-icon': licon.Gear, title: 'Engine settings' },
    class: { active: ctrl.ceval.showEnginePrefs() }, // must use ctrl.ceval rather than ceval here
    hook: bind(
      'click',
      e => {
        e.stopPropagation();
        ctrl.ceval.showEnginePrefs.toggle(); // must use ctrl.ceval rather than ceval here
        if (ctrl.ceval.showEnginePrefs())
          setTimeout(() => document.querySelector<HTMLElement>('#select-engine')?.focus()); // nvui
      },
      () => ctrl.ceval.opts.redraw(), // must use ctrl.ceval rather than ceval here
      false,
    ),
  });
  return [
    hl('div.ceval' + (enabled ? '.enabled' : ''), { class: { computing: ceval.isComputing } }, [
      renderCevalSwitch(ctrl),
      body,
      !ctrl.ceval.opts.custom && threatButton(ctrl),
      settingsGear,
      progressBar,
    ]),
    renderCevalSettings(ctrl),
  ].filter(v => v != null);
}

export function renderCevalSwitch(ctrl: CevalHandler): VNode | false {
  return (
    ctrl.cevalEnabled() !== 'force' &&
    hl('div.switch', { attrs: { role: 'button', title: i18n.site.toggleLocalEvaluation + ' (L)' } }, [
      hl('input#analyse-toggle-ceval.cmn-toggle.cmn-toggle--subtle', {
        attrs: { type: 'checkbox', disabled: !ctrl.ceval.analysable, checked: ctrl.cevalEnabled() },
        props: { checked: !ctrl.ceval.isPaused && ctrl.cevalEnabled() },
        hook: onInsert((el: HTMLInputElement) => {
          el.addEventListener('change', () => ctrl.cevalEnabled(el.checked));
          el.addEventListener('keydown', e => {
            if (e.key === 'Enter' || e.key === ' ') ctrl.cevalEnabled(el.checked);
          });
        }),
      }),
      hl('label', { attrs: { for: 'analyse-toggle-ceval' } }),
    ])
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
    .each(function () {
      pvMoves.push($(this).attr('data-board'));
    });

  return pvMoves;
}

function checkHover(el: HTMLElement, ceval: CevalCtrl): void {
  requestIdleCallback(
    () => setHovering(ceval, getElFen(el), $(el).find('div.pv:hover').attr('data-uci') || undefined),
    500,
  );
}

function setHovering(ceval: CevalCtrl, fen: FEN | null, uci?: Uci): void {
  ceval.hovering(fen && uci ? { fen, uci } : null);
  ceval.opts.onUciHover(ceval.hovering());
}

export function renderPvs(ctrl: CevalHandler): VNode | undefined {
  if (!ctrl.cevalEnabled()) return;
  const ceval = ctrl.ceval;
  const multiPv = ceval.search.multiPv,
    node = ctrl.getNode(),
    setup = parseFen(node.fen).unwrap();
  let pvs: Tree.PvData[],
    threat = false,
    pvMoves: (string | null)[],
    pvIndex: number | null = null;
  if (ctrl.threatMode() && node.threat) {
    pvs = node.threat.pvs;
    threat = true;
  } else if (node.ceval) pvs = node.ceval.pvs;
  else pvs = [];
  if (threat) {
    setup.turn = opposite(setup.turn);
    if (setup.turn === 'white') setup.fullmoves += 1;
  }
  const pos = setupPosition(lichessRules(ceval.opts.variant.key), setup);

  const resetPvIndexAndBoard = () => {
    ctrl.ceval.setPvBoard(null);
    pvIndex = null;
  };

  return hl(
    'div.pv_box',
    {
      attrs: { 'data-fen': node.fen },
      hook: {
        insert: vnode => {
          const el = vnode.elm as HTMLElement;
          el.addEventListener('pointerdown', (e: PointerEvent) => {
            const uciList = getElUciList(e);
            if ((e.target as HTMLElement).closest('.pv-wrap-toggle')) return;
            if (uciList.length > (pvIndex ?? 0) && !ctrl.threatMode()) {
              ctrl.playUciList(uciList.slice(0, (pvIndex ?? 0) + 1));
              resetPvIndexAndBoard();
              e.preventDefault();
            }
          });
          if (isTouchDevice()) return;
          el.addEventListener('mouseover', (e: MouseEvent) => {
            const ceval = ctrl.ceval;
            setHovering(ceval, getElFen(el), getElUci(e));
            const pvBoard = (e.target as HTMLElement).dataset.board;
            if (pvBoard) {
              pvIndex = Number((e.target as HTMLElement).dataset.moveIndex);
              pvMoves = getElPvMoves(e);
              const [fen, uci] = pvBoard.split('|');
              ceval.setPvBoard({ fen, uci });
            }
          });
          el.addEventListener(
            'wheel',
            stepwiseScroll((e: WheelEvent, scroll: boolean) => {
              if (scroll) e.preventDefault();
              if (pvIndex !== null) {
                if (e.deltaY < 0 && pvIndex > 0 && scroll) pvIndex -= 1;
                else if (e.deltaY > 0 && pvIndex < pvMoves.length - 1 && scroll) pvIndex += 1;
                const pvBoard = pvMoves[pvIndex];
                if (pvBoard) {
                  const [fen, uci] = pvBoard.split('|');
                  ctrl.ceval.setPvBoard({ fen, uci });
                }
              }
            }),
          );
          el.addEventListener('mouseout', () => setHovering(ceval, null));
          el.addEventListener('mouseleave', resetPvIndexAndBoard);
          checkHover(el, ceval);
        },
        postpatch: (_, vnode) => !isTouchDevice() && checkHover(vnode.elm as HTMLElement, ceval),
      },
    },
    [
      [...Array(multiPv).keys()].map(i =>
        renderPv(threat, multiPv, pvs[i], pos.isOk ? pos.value : undefined),
      ),
      renderPvBoard(ctrl),
    ],
  );
}

const MAX_NUM_MOVES = 16;

function renderPv(threat: boolean, multiPv: number, pv?: Tree.PvData, pos?: Position): VNode {
  const data: any = {};
  const children: VNode[] = [renderPvWrapToggle()];
  if (pv) {
    if (!threat) data.attrs = { 'data-uci': pv.moves[0] };
    if (multiPv > 1) children.push(hl('strong', defined(pv.mate) ? '#' + pv.mate : renderEval(pv.cp!)));
    if (pos) children.push(...renderPvMoves(pos.clone(), pv.moves.slice(0, MAX_NUM_MOVES)));
  }
  return hl('div.pv.pv--nowrap', data, children);
}

function renderPvWrapToggle(): VNode {
  return hl('span.pv-wrap-toggle', {
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
    if (pos.turn === 'white') text = `${pos.fullmoves}.`;
    else if (i === 0) text = `${pos.fullmoves}...`;
    if (text) vnodes.push(hl('span', { key: text }, text));
    const uci = pv[i];
    const san = makeSanAndPlay(pos, parseUci(uci)!);
    const fen = makeBoardFen(pos.board); // Chessground uses only board fen
    if (san === '--') break;
    key += '|' + uci;
    vnodes.push(
      hl('span.pv-san', { key, attrs: { 'data-move-index': i, 'data-board': `${fen}|${uci}` } }, san),
    );
  }
  return vnodes;
}

function renderPvBoard(ctrl: CevalHandler): VNode | undefined {
  const ceval = ctrl.ceval;
  const pvBoard = ceval.pvBoard();
  if (!pvBoard) return;
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
  const cgVNode = hl('div.cg-wrap.is2d', {
    hook: {
      insert: (vnode: any) => (vnode.elm._cg = makeChessground(vnode.elm, cgConfig)),
      update: (vnode: any) => vnode.elm._cg?.set(cgConfig),
      destroy: (vnode: any) => vnode.elm._cg?.destroy(),
    },
  });
  return hl('div.pv-board', hl('div.pv-board-square', cgVNode));
}

function loadingText(ctrl: CevalHandler): string {
  const d = ctrl.ceval.download;
  return d?.total
    ? `Downloaded ${Math.round((d.bytes * 100) / d.total)}% of ${Math.round(d.total / 1000 / 1000)}MB`
    : i18n.site.loadingEngine;
}
