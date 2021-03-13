import * as winningChances from './winningChances';
import { defined } from 'common';
import { Eval, CevalCtrl, ParentCtrl, NodeEvals } from './types';
import { h } from 'snabbdom';
import { Position } from 'chessops/chess';
import { lichessVariantRules } from 'chessops/compat';
import { makeSanAndPlay } from 'chessops/san';
import { opposite, parseUci } from 'chessops/util';
import { parseFen, makeBoardFen } from 'chessops/fen';
import { renderEval } from './util';
import { setupPosition } from 'chessops/variant';
import { VNode } from 'snabbdom/vnode';

let gaugeLast = 0;
const gaugeTicks: VNode[] = [...Array(8).keys()].map(i =>
  h(i === 3 ? 'tick.zero' : 'tick', { attrs: { style: `height: ${(i + 1) * 12.5}%` } })
);

function localEvalInfo(ctrl: ParentCtrl, evs: NodeEvals): Array<VNode | string> {
  const ceval = ctrl.getCeval(),
    trans = ctrl.trans;
  if (!evs.client) {
    const mb = ceval.downloadProgress() / 1024 / 1024;
    return [
      evs.server && ctrl.nextNodeBest()
        ? trans.noarg('usingServerAnalysis')
        : trans.noarg('loadingEngine') + (mb >= 1 ? ` (${mb.toFixed(1)} MiB)` : ''),
    ];
  }

  const t: Array<VNode | string> = evs.client.cloud
    ? [
        trans('depthX', evs.client.depth || 0),
        h('span.cloud', { attrs: { title: trans.noarg('cloudAnalysis') } }, 'Cloud'),
      ]
    : [trans('depthX', (evs.client.depth || 0) + '/' + evs.client.maxDepth)];
  if (ceval.canGoDeeper())
    t.push(
      h('a.deeper', {
        attrs: {
          title: trans.noarg('goDeeper'),
          'data-icon': 'O',
        },
        hook: {
          insert: vnode =>
            (vnode.elm as HTMLElement).addEventListener('click', () => {
              ceval.goDeeper();
              ceval.redraw();
            }),
        },
      })
    );
  else if (!evs.client.cloud && evs.client.knps) t.push(', ' + Math.round(evs.client.knps) + ' knodes/s');
  return t;
}

function threatInfo(ctrl: ParentCtrl, threat?: Tree.ClientEval | false): string {
  if (!threat) return ctrl.trans.noarg('loadingEngine');
  let t = ctrl.trans('depthX', (threat.depth || 0) + '/' + threat.maxDepth);
  if (threat.knps) t += ', ' + Math.round(threat.knps) + ' knodes/s';
  return t;
}

function threatButton(ctrl: ParentCtrl): VNode | null {
  if (ctrl.disableThreatMode && ctrl.disableThreatMode()) return null;
  return h('a.show-threat', {
    class: {
      active: ctrl.threatMode(),
      hidden: !!ctrl.getNode().check,
    },
    attrs: {
      'data-icon': '7',
      title: ctrl.trans.noarg('showThreat') + ' (x)',
    },
    hook: {
      insert: vnode => (vnode.elm as HTMLElement).addEventListener('click', ctrl.toggleThreatMode),
    },
  });
}

function engineName(ctrl: CevalCtrl): VNode[] {
  const version = ctrl.engineName();
  return [
    h(
      'span',
      { attrs: { title: version || '' } },
      ctrl.technology == 'nnue' ? 'Stockfish 13+' : ctrl.technology == 'hce' ? 'Stockfish 11+' : 'Stockfish 10+'
    ),
    ctrl.technology == 'nnue'
      ? h(
          'span.technology.good',
          {
            attrs: { title: 'Multi-threaded WebAssembly with SIMD (efficiently updatable neural network, strongest)' },
          },
          'NNUE'
        )
      : ctrl.technology == 'hce'
      ? h(
          'span.technology.good',
          { attrs: { title: 'Multi-threaded WebAssembly (classical hand crafted evaluation)' } },
          'HCE'
        )
      : ctrl.technology == 'wasm'
      ? h('span.technology', { attrs: { title: 'Single-threaded WebAssembly fallback (slow)' } }, 'WASM')
      : h('span.technology', { attrs: { title: 'Single-threaded JavaScript fallback (very slow)' } }, 'ASMJS'),
  ];
}

const serverNodes = 4e6;

export function getBestEval(evs: NodeEvals): Eval | undefined {
  const serverEv = evs.server,
    localEv = evs.client;

  if (!serverEv) return localEv;
  if (!localEv) return serverEv;

  // Prefer localEv if it exeeds fishnet node limit or finds a better mate.
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
  const bestEv = getBestEval(ctrl.currentEvals());
  let ev;
  if (bestEv) {
    ev = winningChances.povChances('white', bestEv);
    gaugeLast = ev;
  } else ev = gaugeLast;
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

export function renderCeval(ctrl: ParentCtrl): VNode | undefined {
  const instance = ctrl.getCeval(),
    trans = ctrl.trans;
  if (!instance.allowed() || !instance.possible || !ctrl.showComputer()) return;
  const enabled = instance.enabled(),
    evs = ctrl.currentEvals(),
    threatMode = ctrl.threatMode(),
    threat = threatMode && ctrl.getNode().threat,
    bestEv = threat || getBestEval(evs);
  let pearl: VNode | string, percent: number;
  if (bestEv && typeof bestEv.cp !== 'undefined') {
    pearl = renderEval(bestEv.cp);
    percent = evs.client
      ? Math.min(100, Math.round((100 * evs.client.depth) / (evs.client.maxDepth || instance.effectiveMaxDepth())))
      : 0;
  } else if (bestEv && defined(bestEv.mate)) {
    pearl = '#' + bestEv.mate;
    percent = 100;
  } else if (ctrl.outcome()) {
    pearl = '-';
    percent = 0;
  } else {
    pearl = enabled ? h('i.ddloader') : h('i');
    percent = 0;
  }
  if (threatMode) {
    if (threat) percent = Math.min(100, Math.round((100 * threat.depth) / threat.maxDepth));
    else percent = 0;
  }

  const progressBar: VNode | null = enabled
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

  const body: Array<VNode | null> = enabled
    ? [
        h('pearl', [pearl]),
        h('div.engine', [
          ...(threatMode ? [trans.noarg('showThreat')] : engineName(instance)),
          h(
            'span.info',
            ctrl.outcome()
              ? [trans.noarg('gameOver')]
              : threatMode
              ? [threatInfo(ctrl, threat)]
              : localEvalInfo(ctrl, evs)
          ),
        ]),
      ]
    : [
        pearl ? h('pearl', [pearl]) : null,
        h('help', [...engineName(instance), h('br'), trans.noarg('inLocalBrowser')]),
      ];

  const switchButton: VNode | null =
    ctrl.mandatoryCeval && ctrl.mandatoryCeval()
      ? null
      : h(
          'div.switch',
          {
            attrs: { title: trans.noarg('toggleLocalEvaluation') + ' (l)' },
          },
          [
            h('input#analyse-toggle-ceval.cmn-toggle.cmn-toggle--subtle', {
              attrs: {
                type: 'checkbox',
                checked: enabled,
              },
              hook: {
                insert: vnode => (vnode.elm as HTMLElement).addEventListener('change', ctrl.toggleCeval),
              },
            }),
            h('label', { attrs: { for: 'analyse-toggle-ceval' } }),
          ]
        );

  return h(
    'div.ceval' + (enabled ? '.enabled' : ''),
    {
      class: {
        computing: percent < 100 && instance.isComputing(),
      },
    },
    [progressBar, ...body, threatButton(ctrl), switchButton]
  );
}

function getElFen(el: HTMLElement): string {
  return el.getAttribute('data-fen')!;
}

function getElUci(e: MouseEvent): string | undefined {
  return (
    $(e.target as HTMLElement)
      .closest('div.pv')
      .attr('data-uci') || undefined
  );
}

function checkHover(el: HTMLElement, instance: CevalCtrl): void {
  lichess.requestIdleCallback(
    () => instance.setHovering(getElFen(el), $(el).find('div.pv:hover').attr('data-uci') || undefined),
    500
  );
}

export function renderPvs(ctrl: ParentCtrl): VNode | undefined {
  const instance = ctrl.getCeval();
  if (!instance.allowed() || !instance.possible || !instance.enabled()) return;
  const multiPv = parseInt(instance.multiPv()),
    node = ctrl.getNode(),
    setup = parseFen(node.fen).unwrap();
  let pvs: Tree.PvData[],
    threat = false;
  if (ctrl.threatMode() && node.threat) {
    pvs = node.threat.pvs;
    threat = true;
  } else if (node.ceval) pvs = node.ceval.pvs;
  else pvs = [];
  if (threat) {
    setup.turn = opposite(setup.turn);
    if (setup.turn == 'white') setup.fullmoves += 1;
  }
  const pos = setupPosition(lichessVariantRules(instance.variant.key), setup);
  return h(
    'div.pv_box',
    {
      attrs: { 'data-fen': node.fen },
      hook: {
        insert: vnode => {
          const el = vnode.elm as HTMLElement;
          el.addEventListener('mouseover', (e: MouseEvent) => {
            instance.setHovering(getElFen(el), getElUci(e));
            const pvBoard = (e.target as HTMLElement).getAttribute('data-board');
            if (pvBoard) {
              const [fen, uci] = pvBoard.split('|');
              instance.setPvBoard({ fen, uci });
            }
          });
          el.addEventListener('mouseout', () => instance.setHovering(getElFen(el)));
          el.addEventListener('mousedown', (e: MouseEvent) => {
            const uci = getElUci(e);
            if (uci) ctrl.playUci(uci);
          });
          el.addEventListener('mouseleave', () => instance.setPvBoard(null));
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
        el.addEventListener('mousedown', (e: MouseEvent) => {
          e.stopPropagation();
          $(el).closest('.pv').toggleClass('pv--nowrap');
        });
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
  const instance = ctrl.getCeval();
  const pvBoard = instance.pvBoard();
  if (!pvBoard) {
    return;
  }
  const { fen, uci } = pvBoard;
  const lastMove = uci[1] === '@' ? [uci.slice(2)] : [uci.slice(0, 2), uci.slice(2, 4)];
  const orientation = ctrl.getOrientation();
  const cgConfig = {
    fen,
    lastMove,
    orientation,
    coordinates: false,
    viewOnly: true,
    resizable: false,
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
