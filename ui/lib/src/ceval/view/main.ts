import { povChances } from '../winningChances';
import * as licon from '../../licon';
import { stepwiseScroll } from '../../view/controls';
import { type VNode, type LooseVNodes, onInsert, bind, hl } from '../../snabbdom';
import { defined, notNull, requestIdleCallback } from '../../common';
import { type ParentCtrl, type NodeEvals, CevalState } from '../types';
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

type EvalInfo = { knps: number; npsText: string; depthText: string };

let gaugeLast = 0;
const gaugeTicks: VNode[] = [...Array(8).keys()].map(i =>
  hl(i === 3 ? 'tick.zero' : 'tick', { attrs: { style: `height: ${(i + 1) * 12.5}%` } }),
);

function localEvalNodes(ctrl: ParentCtrl, evs: NodeEvals): Array<VNode | string> {
  const ceval = ctrl.getCeval(),
    state = ceval.state;
  if (!evs.client) {
    if (!ceval.analysable) return ['Engine cannot analyze this position'];
    if (state === CevalState.Failed) return [i18n.site.engineFailed];
    const localEvalText = state === CevalState.Loading ? loadingText(ctrl) : i18n.site.calculatingMoves;
    return [evs.server && ctrl.nextNodeBest() ? i18n.site.usingServerAnalysis : localEvalText];
  }

  const t: Array<VNode | string> = [];
  if (ceval.canGoDeeper)
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

function threatInfo(ctrl: ParentCtrl, threat?: Tree.LocalEval | false): string {
  const info = localInfo(ctrl, threat);
  return info.depthText + (info.knps ? ' · ' + info.npsText : '');
}

function localInfo(ctrl: ParentCtrl, ev?: Tree.ClientEval | false): EvalInfo {
  const info = {
    npsText: '',
    knps: 0,
    depthText: i18n.site.calculatingMoves,
  };

  if (!ev) return info;

  const ceval = ctrl.getCeval();
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

function threatButton(ctrl: ParentCtrl): VNode | null {
  if (ctrl.getCeval().download || ctrl.disableThreatMode?.()) return null;
  return hl('button.show-threat', {
    class: { active: ctrl.threatMode(), hidden: !!ctrl.getNode().check },
    attrs: { 'data-icon': licon.Target, title: i18n.site.showThreat + ' (x)' },
    hook: bind('click', ctrl.toggleThreatMode),
  });
}

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

export const getBestEval = (evs: NodeEvals): EvalScore | undefined => evs.client || evs.server;

export function renderGauge(ctrl: ParentCtrl): VNode | undefined {
  if (ctrl.ongoing || !ctrl.showEvalGauge()) return;
  const bestEv = getBestEval(ctrl.currentEvals());
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

export function renderCeval(ctrl: ParentCtrl): VNode[] {
  const ceval = ctrl.getCeval();
  if (!ceval.allowed() || !ceval.possible) return [];
  if (!ctrl.showComputer()) return [analysisDisabled(ctrl)];
  const enabled = ceval.enabled(),
    evs = ctrl.currentEvals(),
    threatMode = ctrl.threatMode(),
    threat = threatMode ? ctrl.getNode().threat : undefined,
    bestEv = threat || getBestEval(evs),
    search = ceval.search,
    download = ceval.download;
  let pearl: VNode | string,
    percent = 0;

  if (evs.client) {
    if (evs.client.cloud && !threatMode) percent = 100;
    else if (ceval.isDeeper() || ceval.isInfinite) percent = Math.min(100, (100 * evs.client.depth) / 99);
    else if ('movetime' in search.by)
      percent = Math.min(100, (100 * ((threat ?? evs.client)?.millis ?? 0)) / search.by.movetime);
    else if ('depth' in search.by) percent = Math.min(100, (100 * evs.client.depth) / search.by.depth);
    else if ('nodes' in search.by) percent = Math.min(100, (100 * evs.client.nodes) / search.by.nodes);
  }
  if (bestEv && typeof bestEv.cp !== 'undefined') pearl = renderEval(bestEv.cp);
  else if (bestEv && defined(bestEv.mate)) {
    pearl = '#' + bestEv.mate;
    percent = 100;
  } else {
    if (!enabled) pearl = hl('i');
    else if (ctrl.outcome() || ctrl.getNode().threefold) pearl = '-';
    else if (ceval.state === CevalState.Failed)
      pearl = hl('i.is-red', { attrs: { 'data-icon': licon.CautionCircle } });
    else pearl = hl('i.ddloader');
    percent = 0;
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
      }),
    );

  const body: LooseVNodes = enabled
    ? [
        hl('pearl', [pearl]),
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
                  : localEvalNodes(ctrl, evs),
          ),
        ]),
      ]
    : [
        pearl && hl('pearl', [pearl]),
        hl('div.engine', [
          engineName(ceval),
          hl('br'),
          ceval.analysable ? i18n.site.inLocalBrowser : 'Illegal positions cannot be analyzed',
        ]),
      ];

  const switchButton: VNode | false =
    !ctrl.mandatoryCeval?.() &&
    hl('div.switch', { attrs: { role: 'button', title: i18n.site.toggleLocalEvaluation + ' (L)' } }, [
      hl('input#analyse-toggle-ceval.cmn-toggle.cmn-toggle--subtle', {
        attrs: { type: 'checkbox', checked: enabled, disabled: !ceval.analysable },
        hook: onInsert((el: HTMLInputElement) => {
          el.addEventListener('keydown', e => (e.key === 'Enter' || e.key === ' ') && ctrl.toggleCeval());
          el.addEventListener('change', () => ctrl.toggleCeval());
        }),
      }),
      hl('label', { attrs: { for: 'analyse-toggle-ceval' } }),
    ]);

  const settingsGear = hl('button.settings-gear', {
    attrs: { role: 'button', 'data-icon': licon.Gear, title: 'Engine settings' },
    class: { active: ctrl.getCeval().showEnginePrefs() }, // must use ctrl.getCeval() rather than ceval here
    hook: bind(
      'click',
      () => {
        ctrl.getCeval().showEnginePrefs.toggle(); // must use ctrl.getCeval() rather than ceval here
        if (ctrl.getCeval().showEnginePrefs())
          setTimeout(() => document.querySelector<HTMLElement>('.select-engine')?.focus()); // nvui
      },
      () => ctrl.getCeval().opts.redraw(), // must use ctrl.getCeval() rather than ceval here
      false,
    ),
  });

  return [
    hl('div.ceval' + (enabled ? '.enabled' : ''), { class: { computing: ceval.isComputing } }, [
      switchButton,
      body,
      threatButton(ctrl),
      settingsGear,
      progressBar,
    ]),
    renderCevalSettings(ctrl),
  ].filter((v): v is VNode => !!v);
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
    () => ceval.setHovering(getElFen(el), $(el).find('div.pv:hover').attr('data-uci') || undefined),
    500,
  );
}

export function renderPvs(ctrl: ParentCtrl): VNode | undefined {
  const ceval = ctrl.getCeval();
  if (!ceval.allowed() || !ceval.possible || !ceval.enabled()) return;
  const multiPv = ceval.search.multiPv,
    node = ctrl.getNode(),
    setup = parseFen(node.fen).unwrap();
  let pvs: Tree.PvData[],
    threat = false,
    pvMoves: (string | null)[],
    pvIndex: number | null;
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

  return hl(
    'div.pv_box',
    {
      attrs: { 'data-fen': node.fen },
      hook: {
        insert: vnode => {
          const el = vnode.elm as HTMLElement;
          el.addEventListener('mouseover', (e: MouseEvent) => {
            const ceval = ctrl.getCeval();
            ceval.setHovering(getElFen(el), getElUci(e));
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
                  ctrl.getCeval().setPvBoard({ fen, uci });
                }
              }
            }),
          );
          el.addEventListener('mouseout', () => ctrl.getCeval().setHovering(getElFen(el)));
          for (const event of ['touchstart', 'mousedown']) {
            el.addEventListener(event, (e: TouchEvent | MouseEvent) => {
              const uciList = getElUciList(e);
              if (uciList.length > (pvIndex ?? 0) && !ctrl.threatMode()) {
                ctrl.playUciList(uciList.slice(0, (pvIndex ?? 0) + 1));
                e.preventDefault();
              }
            });
          }
          el.addEventListener('mouseleave', () => {
            ctrl.getCeval().setPvBoard(null);
            pvIndex = null;
          });
          checkHover(el, ceval);
        },
        postpatch: (_, vnode) => checkHover(vnode.elm as HTMLElement, ceval),
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

function renderPvBoard(ctrl: ParentCtrl): VNode | undefined {
  const ceval = ctrl.getCeval();
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

const analysisDisabled = (ctrl: ParentCtrl): VNode =>
  hl('div.comp-off__hint', [
    hl('span', i18n.site.computerAnalysisDisabled),
    hl(
      'button',
      { hook: bind('click', () => ctrl.toggleComputer?.(), ctrl.redraw), attrs: { type: 'button' } },
      i18n.site.enable,
    ),
  ]);

function loadingText(ctrl: ParentCtrl): string {
  const d = ctrl.getCeval().download;
  return d?.total
    ? `Downloaded ${Math.round((d.bytes * 100) / d.total)}% of ${Math.round(d.total / 1000 / 1000)}MB`
    : i18n.site.loadingEngine;
}
