import { defined, notNull } from 'common/common';
import { makeNotationLineWithPosition, notationsWithColor } from 'common/notation';
import stepwiseScroll from 'common/wheel';
import { Config } from 'shogiground/config';
import { usiToSquareNames } from 'shogiops/compat';
import { forsythToRole, makeSfen, parseSfen, roleToForsyth } from 'shogiops/sfen';
import { MoveOrDrop } from 'shogiops/types';
import { makeUsi, opposite, parseUsi } from 'shogiops/util';
import { Position } from 'shogiops/variant/position';
import { handRoles } from 'shogiops/variant/util';
import { VNode, h } from 'snabbdom';
import { CevalCtrl, Eval, NodeEvals, ParentCtrl } from './types';
import { renderEval, unsupportedVariants } from './util';
import * as winningChances from './winningChances';

let gaugeLast = 0;
const gaugeTicks: VNode[] = [...Array(8).keys()].map(i =>
  h(i === 3 ? 'tick.zero' : 'tick', { attrs: { style: `height: ${(i + 1) * 12.5}%` } })
);

function localEvalInfo(ctrl: ParentCtrl, evs: NodeEvals): Array<VNode | string | null> {
  const ceval = ctrl.getCeval(),
    trans = ctrl.trans;
  if (!evs.client) {
    if (!ceval.analysable) return ['Engine cannot analyze this position'];

    const mb = ceval.downloadProgress() / 1024 / 1024;
    return [
      evs.server && ctrl.nextNodeBest()
        ? trans.noarg('usingServerAnalysis')
        : trans.noarg('loadingEngine') + (mb >= 1 ? ` (${mb.toFixed(1)} MiB)` : ''),
    ];
  }
  const depth = evs.client.depth || 0;
  const t: Array<VNode | string | null> = evs.client.cloud
    ? [
        trans('depthX', depth),
        h(
          'span.cloud',
          { attrs: { title: trans.noarg('cloudAnalysis') } },
          `Cloud - ${ceval.shouldUseYaneuraou ? 'NNUE' : 'HCE'}`
        ),
      ]
    : [trans('depthX', depth + '/' + Math.max(depth, evs.client.maxDepth))];
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
  else if (!evs.client.cloud && evs.client.knps) t.push(', ' + Math.round(evs.client.knps) + 'k nodes/s');
  return t;
}

function threatInfo(ctrl: ParentCtrl, threat?: Tree.LocalEval | false): string {
  if (!threat) return ctrl.trans.noarg('loadingEngine');
  let t = ctrl.trans('depthX', (threat.depth || 0) + '/' + threat.maxDepth);
  if (threat.knps) t += ', ' + Math.round(threat.knps) + 'k nodes/s';
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
  return [
    h('span', ctrl.engineName),
    ctrl.technology == 'nnue'
      ? h(
          'span.technology.good',
          {
            attrs: {
              title:
                'Multi-threaded WebAssembly with SIMD (efficiently updatable neural network, using 4x smaller net by Sopel97)',
            },
          },
          'NNUE'
        )
      : ctrl.technology == 'hce'
        ? h(
            'span.technology.good',
            { attrs: { title: 'Multi-threaded WebAssembly (classical hand crafted evaluation)' } },
            'HCE'
          )
        : h(
            'span.technology.bad.' + ctrl.variant.key,
            { attrs: { title: 'Unfortunately local analysis is not available for this device or browser' } },
            'No engine supported'
          ),
  ];
}

const serverNodes = 4e6;

export function getBestEval(evs: NodeEvals): Eval | undefined {
  const serverEv = evs.server,
    localEv = evs.client;

  if (!serverEv) return localEv;
  if (!localEv) return serverEv;

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
  const bestEv = getBestEval(ctrl.currentEvals());
  let ev;
  if (bestEv) {
    ev = winningChances.povChances('sente', bestEv);
    gaugeLast = ev;
  } else ev = gaugeLast;

  return h(
    'div.eval-gauge',
    {
      class: {
        empty: ev === null,
        reverse: ctrl.getOrientation() === 'gote',
      },
    },
    [h('div.gote', { attrs: { style: `height: ${100 - (ev + 1) * 50}%` } }), ...gaugeTicks]
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
    bestEv = threat || getBestEval(evs),
    isImpasseOutcome = instance.enteringKingRule() && ctrl.isImpasse();
  let pearl: VNode | string, percent: number;
  if (bestEv && typeof bestEv.cp !== 'undefined') {
    pearl = renderEval(bestEv.cp);
    percent = evs.client
      ? evs.client.cloud
        ? 100
        : Math.min(100, Math.round((100 * evs.client.depth) / evs.client.maxDepth))
      : 0;
  } else if (bestEv && defined(bestEv.mate)) {
    pearl = '#' + bestEv.mate;
    percent = 100;
  } else if (ctrl.outcome() || isImpasseOutcome) {
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
            ctrl.outcome() || isImpasseOutcome
              ? [trans.noarg('gameOver')]
              : threatMode
                ? [threatInfo(ctrl, threat)]
                : localEvalInfo(ctrl, evs)
          ),
        ]),
      ]
    : [
        pearl ? h('pearl', [pearl]) : null,
        h('help', [
          ...engineName(instance),
          h('span', [
            h('br'),
            instance.analysable
              ? trans.noarg('inLocalBrowser')
              : unsupportedVariants.includes(instance.variant.key)
                ? trans.noarg('variantNotSupported')
                : 'Engine cannot analyse this game',
          ]),
        ]),
      ];

  const switchButton: VNode | null =
    (ctrl.mandatoryCeval && ctrl.mandatoryCeval()) || !instance.analysable
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
                disabled: !instance.analysable,
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

function getElSfen(el: HTMLElement): string {
  return el.getAttribute('data-sfen')!;
}

function getElUsi(e: TouchEvent | MouseEvent): string | undefined {
  return (
    $(e.target as HTMLElement)
      .closest('div.pv')
      .attr('data-usi') || undefined
  );
}

function getElUsiList(e: TouchEvent | MouseEvent): string[] {
  return getElPvMoves(e)
    .filter(notNull)
    .map(move => move.split('|')[1]);
}

function getElPvMoves(e: TouchEvent | MouseEvent): (string | null)[] {
  const pvMoves: (string | null)[] = [];

  $(e.target as HTMLElement)
    .closest('div.pv')
    .children()
    .filter('span.pv-move')
    .each(function (this: Element) {
      pvMoves.push($(this).attr('data-board'));
    });

  return pvMoves;
}

function checkHover(el: HTMLElement, instance: CevalCtrl): void {
  window.lishogi.requestIdleCallback(() => {
    instance.setHovering(getElSfen(el), $(el).find('div.pv:hover').attr('data-usi') || undefined);
  });
}

export function renderPvs(ctrl: ParentCtrl): VNode | undefined {
  const instance = ctrl.getCeval();
  if (!instance.allowed() || !instance.possible || !instance.enabled()) return;
  const multiPv = parseInt(instance.multiPv()),
    node = ctrl.getNode(),
    position = parseSfen(instance.variant.key, node.sfen, false);
  let pvs: Tree.PvData[],
    threat = false,
    pvMoves: (string | null)[],
    pvIndex: number | null;
  if (ctrl.threatMode() && node.threat) {
    pvs = node.threat.pvs;
    threat = true;
  } else if (node.ceval) pvs = node.ceval.pvs;
  else pvs = [];
  if (position.isOk) {
    if (node.usi) position.value.lastMoveOrDrop = parseUsi(node.usi);
    if (threat) {
      position.value.turn = opposite(position.value.turn);
      if (position.value.turn == 'sente') position.value.moveNumber += 1;
    }
  }
  return h(
    'div.pv_box',
    {
      attrs: { 'data-sfen': node.sfen },
      hook: {
        insert: vnode => {
          const el = vnode.elm as HTMLElement;
          el.addEventListener('mouseover', (e: MouseEvent) => {
            const instance = ctrl.getCeval();
            instance.setHovering(getElSfen(el), getElUsi(e));
            const pvBoard = (e.target as HTMLElement).dataset.board;
            if (pvBoard) {
              pvIndex = Number((e.target as HTMLElement).dataset.moveIndex);
              pvMoves = getElPvMoves(e);
              const [sfen, usi] = pvBoard.split('|');
              instance.setPvBoard({ sfen, usi });
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
                  const [sfen, usi] = pvBoard.split('|');
                  ctrl.getCeval().setPvBoard({ sfen, usi });
                }
              }
            })
          );
          el.addEventListener('mouseout', () => ctrl.getCeval().setHovering(getElSfen(el)));
          for (const event of ['touchstart', 'mousedown']) {
            el.addEventListener(event, (e: TouchEvent | MouseEvent) => {
              const usiList = getElUsiList(e);
              if (usiList.length > (pvIndex ?? 0) && !ctrl.threatMode()) {
                ctrl.playUsiList(usiList.slice(0, (pvIndex ?? 0) + 1));
                e.preventDefault();
              }
            });
          }
          el.addEventListener('mouseleave', () => {
            ctrl.getCeval().setPvBoard(null);
            pvIndex = null;
          });
          checkHover(el, instance);
        },
        postpatch: (_, vnode) => checkHover(vnode.elm as HTMLElement, instance),
      },
    },
    [
      ...[...Array(multiPv).keys()].map(i =>
        renderPv(threat, multiPv, pvs[i], position.isOk ? position.value : undefined)
      ),
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
      data.attrs = { 'data-usi': pv.moves[0] };
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

function renderPvMoves(pos: Position, pv: Usi[]): VNode[] {
  let key = makeSfen(pos);
  const vnodes: VNode[] = [],
    moves = pv.map(u => parseUsi(u)).filter((m): m is MoveOrDrop => defined(m)),
    notationMoves = makeNotationLineWithPosition(pos, moves, pos.lastMoveOrDrop),
    addColorIcon = notationsWithColor();

  for (let i = 0; i < moves.length; i++) {
    const colorIcon = addColorIcon ? '.color-icon.' + pos.turn : '',
      moveNumber = `${pos.moveNumber}. `;
    pos.play(moves[i]);
    const usi = makeUsi(moves[i]),
      sfen = makeSfen(pos);
    key += '|' + usi;
    vnodes.push(
      h(
        'span.pv-move' + colorIcon,
        {
          key,
          attrs: {
            'data-move-index': i,
            'data-board': `${sfen}|${usi}`,
          },
        },
        (addColorIcon ? '' : moveNumber) + notationMoves[i]
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
  const { sfen, usi } = pvBoard;
  const orientation = ctrl.getOrientation();
  const sgConfig: Config = {
    sfen: {
      board: sfen.split(' ')[0],
      hands: sfen.split(' ')[2],
    },
    hands: {
      roles: handRoles(instance.variant.key),
      inlined: instance.variant.key !== 'chushogi',
    },
    forsyth: {
      fromForsyth: forsythToRole(instance.variant.key),
      toForsyth: roleToForsyth(instance.variant.key),
    },
    lastDests: usiToSquareNames(usi),
    orientation,
    coordinates: { enabled: false },
    viewOnly: true,
    drawable: {
      enabled: false,
      visible: false,
    },
  };
  const sgVNode = h(
    'div.mini-board.v-' + instance.variant.key,
    h('div.sg-wrap', {
      hook: {
        insert: (vnode: any) => (vnode.elm._sg = window.Shogiground(sgConfig, { board: vnode.elm })),
        update: (vnode: any) => vnode.elm._sg.set(sgConfig),
        destroy: (vnode: any) => vnode.elm._sg.destroy(),
      },
    })
  );
  return h('div.pv-board', sgVNode);
}
