import { Eval, CevalController, ParentController, NodeEvals } from './types';
import * as winningChances from './winningChances';
import { defined } from 'common';
import { renderEval } from 'chess';
import pv2san from './pv2san';
import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

let gaugeLast = 0;
const gaugeTicks: VNode[] = [];
for (let i = 1; i < 8; i++) gaugeTicks.push(h(i === 4 ? 'tick.zero' : 'tick', {
  attrs: { style: `height: ${i * 12.5}%` }
}));

function range(len: number): number[] {
  const r = [];
  for (let i = 0; i < len; i++) r.push(i);
  return r;
}

function localEvalInfo(ctrl: ParentController, evs: NodeEvals): VNode[] {
  const ceval = ctrl.getCeval();
  if (!evs.client) {
    return [
      h('span', evs.server && ctrl.nextNodeBest() ? 'Using server analysis' : 'Loading engine...')
    ];
  }
  const t: VNode[] = evs.client.cloud ? [
    h('span', 'Depth ' + (evs.client.depth || 0)),
    h('span.cloud', { attrs: { title: 'Cloud Analysis' } }, 'cloud')
  ] : [
    h('span', 'Depth ' + (evs.client.depth || 0) + '/' + evs.client.maxDepth)
  ];
  if (ceval.canGoDeeper() && (
    evs.client.depth >= (evs.client.maxDepth || ceval.effectiveMaxDepth())
  ))
  t.push(h('a.deeper', {
    attrs: {
      title: 'Go deeper',
      'data-icon': 'O'
    },
    hook: {
      insert: vnode => (vnode.elm as HTMLElement).addEventListener('click', ceval.goDeeper)
    }
  }));
  else if (!evs.client.cloud && evs.client.knps) t.push(
    h('span', ', ' + Math.round(evs.client.knps) + ' knodes/s'));
  return t;
}

function threatInfo(threat?: Tree.ClientEval | false): string {
  if (!threat) return 'Loading engine...';
  let t = 'Depth ' + (threat.depth || 0) + '/' + threat.maxDepth;
  if (threat.knps) t += ', ' + Math.round(threat.knps) + ' knodes/s';
  return t;
}

function threatButton(ctrl: ParentController): VNode | undefined {
  if (ctrl.disableThreatMode && ctrl.disableThreatMode()) return;
  return h('a.show-threat', {
    class: {
      active: ctrl.vm.threatMode,
      hidden: ctrl.vm.node.check
    },
    attrs: {
      'data-icon': '7',
      title: 'Show threat (x)'
    },
    hook: {
      insert: vnode => (vnode.elm as HTMLElement).addEventListener('click', ctrl.toggleThreatMode)
    }
  });
}

function engineName(ctrl: CevalController) {
  return [
    window.lichess.engineName,
    ctrl.pnaclSupported ? h('span.native', 'pnacl') : (ctrl.wasmSupported ? h('span.native', 'wasm') : h('span.asmjs', 'asmjs'))
  ];
}

const serverNodes = 4e6;

export function getBestEval(evs: NodeEvals): Eval | undefined {
  const serverEv = evs.server,
  localEv = evs.client;

  if (!serverEv) return localEv;
  if (!localEv) return serverEv;

  // Prefer localEv if it exeeds fishnet node limit or finds a better mate.
  if (localEv.nodes > serverNodes ||
    (typeof localEv.mate !== 'undefined' && (typeof serverEv.mate === 'undefined' || Math.abs(localEv.mate) < Math.abs(serverEv.mate))))
  return localEv;

  return serverEv;
}

export function renderGauge(ctrl: ParentController): VNode | undefined {
  if (ctrl.ongoing || !ctrl.showEvalGauge()) return;
  let ev, bestEv = getBestEval(ctrl.currentEvals());
  if (bestEv) {
    ev = winningChances.povChances('white', bestEv);
    gaugeLast = ev;
  } else ev = gaugeLast;
  const height = 100 - (ev + 1) * 50;
  return h('div.eval_gauge', {
    class: {
      empty: ev === null,
      reverse: ctrl.getOrientation() === 'black'
    }
  }, [
    h('div.black', { attrs: { style: `height: ${height}%` } })
  ].concat(gaugeTicks)
  );
}

export function renderCeval(ctrl: ParentController) {
  const instance = ctrl.getCeval();
  if (!instance.allowed() || !instance.possible || !ctrl.vm.showComputer()) return;
  const enabled = instance.enabled();
  const evs = ctrl.currentEvals();
  const bestEv = getBestEval(evs);
  const threatMode = ctrl.vm.threatMode;
  const threat = threatMode && ctrl.vm.node.threat;
  let pearl: VNode, percent: number;
  if (bestEv && typeof bestEv.cp !== 'undefined') {
    pearl = h('span', renderEval(bestEv.cp));
    percent = evs.client ? Math.min(100, Math.round(100 * evs.client.depth / (evs.client.maxDepth || instance.effectiveMaxDepth()))) : 0;
  } else if (bestEv && defined(bestEv.mate)) {
    pearl = h('span', '#' + bestEv.mate);
    percent = 100;
  } else if (ctrl.gameOver()) {
    pearl = h('span', '-');
    percent = 0;
  } else {
    pearl = enabled ? h('span.ddloader') : h('span');
    percent = 0;
  }
  if (threatMode) {
    if (threat) percent = Math.min(100, Math.round(100 * threat.depth / threat.maxDepth));
    else percent = 0;
  }
  const mandatoryCeval = ctrl.mandatoryCeval && ctrl.mandatoryCeval();
  return h('div.ceval_box', {
    class: {
      computing: percent < 100 && instance.isComputing()
    }
  }, [
  enabled ? h('div.bar', h('span', {
    class: { threat: threatMode },
    attrs: { style: `width: ${percent}%` },
    // hook: {
    //   insert: vnode => {
    // config: function(el: Element, isUpdate: boolean, ctx: any) {
    //   // reinsert the node to avoid downward animation
    //   if (isUpdate && (ctx.percent > percent || ctx.threatMode !== threatMode)) {
    //     const p = el.parentNode;
    //     p!.removeChild(el);
    //     p!.appendChild(el);
    //   }
    //   ctx.percent = percent;
    //   ctx.threatMode = threatMode;
    // }
  })) : null,
  enabled ? [
    h('pearl', pearl),
    h('div.engine', [
      threatMode ? 'Show threat' : engineName(instance),
      h('span.info', ctrl.gameOver() ? 'Game over.' : (
        threatMode ? threatInfo(threat) : localEvalInfo(ctrl, evs)
      ))
    ])
  ] : [
    pearl ? h('pearl', pearl) : null,
    h('help', [
      h('span', engineName(instance)), 
        h('br'), 
        h('span', 'in local browser')
    ])
  ],
  mandatoryCeval ? null : h('div.switch', {
    attrs: { title: 'Toggle local evaluation (l)' }
  }, [
    h('input#analyse-toggle-ceval.cmn-toggle.cmn-toggle-round', {
      attrs: {
        type: 'checkbox',
        checked: enabled
      },
      hook: {
        insert: vnode => (vnode.elm as HTMLElement).addEventListener('change', ctrl.toggleCeval)
      }
    }),
    h('label', { attrs: { 'for': 'analyse-toggle-ceval' } })
  ]),
  threatButton(ctrl)
  ])
}

export function renderPvs(ctrl: ParentController) {
  const instance = ctrl.getCeval();
  if (!instance.allowed() || !instance.possible || !instance.enabled()) return;
  const multiPv = parseInt(instance.multiPv());
  const pvs : Tree.PvData[], threat = false;
  if (ctrl.vm.threatMode && ctrl.vm.node.threat) {
    pvs = ctrl.vm.node.threat.pvs;
    threat = true;
  } else if (ctrl.vm.node.ceval)
  pvs = ctrl.vm.node.ceval.pvs;
  else
  pvs = [];
  return m('div.pv_box', {
    'data-fen': ctrl.vm.node.fen,
    config: function(el: Element, isUpdate: boolean) {
      if (!isUpdate) {
        el.addEventListener('mouseover', function(e) {
          instance.setHovering($(el).attr('data-fen'), $(e.target).closest('div.pv').attr('data-uci'));
        });
        el.addEventListener('mouseout', function() {
          instance.setHovering($(el).attr('data-fen'), null);
        });
        el.addEventListener('mousedown', function(e) {
          const uci = $(e.target).closest('div.pv').attr('data-uci');
          if (uci) ctrl.playUci(uci);
        });
      }
      setTimeout(function() {
        instance.setHovering($(el).attr('data-fen'), $(el).find('div.pv:hover').attr('data-uci'));
      }, 100);
    }
  }, range(multiPv).map(function(i) {
    if (!pvs[i]) return m('div.pv');
    else return m('div.pv', threat ? {} : {
      'data-uci': pvs[i].moves[0]
    }, [
      multiPv > 1 ? m('strong', defined(pvs[i].mate) ? ('#' + pvs[i].mate) : renderEval(pvs[i].cp!)) : null,
      m('span', pv2san(instance.variant.key, ctrl.vm.node.fen, threat, pvs[i].moves, pvs[i].mate))
    ]);
  }));
}
