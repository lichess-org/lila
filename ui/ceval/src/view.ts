import { Eval, CevalController, ParentController, NodeEvals } from './types';
import * as m from 'mithril';
import * as winningChances from './winningChances';
import { defined, classSet } from 'common';
import { renderEval } from 'chess';
import pv2san from './pv2san';

var gaugeLast = 0;
var gaugeTicks: Mithril.VirtualElement[] = [];
for (var i = 1; i < 8; i++) gaugeTicks.push(m(i === 4 ? 'tick.zero' : 'tick', {
  style: {
    height: (i * 12.5) + '%'
  }
}));

function range(len: number): number[] {
  var r = [];
  for (var i = 0; i < len; i++) r.push(i);
  return r;
}

function localEvalInfo(ctrl: ParentController, evs: NodeEvals) {
  var ceval = ctrl.getCeval();
  if (!evs.client) {
    if (evs.server && ctrl.nextNodeBest()) return 'Using server analysis';
    return 'Loading engine...';
  }
  var t: Mithril.Renderable[] = evs.client.cloud ? [
    [
      'Depth ' + (evs.client.depth || 0),
      m('span.cloud', {
        title: 'Cloud Analysis'
      }, 'cloud')
    ]
  ] : [
    'Depth ' + (evs.client.depth || 0) + '/' + evs.client.maxDepth
  ];
  if (ceval.canGoDeeper()) {
    t.push(m('a.deeper', {
      title: 'Go deeper',
      'data-icon': 'O',
      onclick: ceval.goDeeper
    }));
  }
  else if (!evs.client.cloud && evs.client.knps) t.push(', ' + Math.round(evs.client.knps) + ' knodes/s');
  return t;
}

function threatInfo(threat?: Tree.ClientEval | false): string {
  if (!threat) return 'Loading engine...';
  var t = 'Depth ' + (threat.depth || 0) + '/' + threat.maxDepth;
  if (threat.knps) t += ', ' + Math.round(threat.knps) + ' knodes/s';
  return t;
}

function threatButton(ctrl: ParentController): Mithril.VirtualElement | undefined {
  if (ctrl.disableThreatMode && ctrl.disableThreatMode()) return;
  return m('a', {
    class: classSet({
      'show-threat': true,
      active: ctrl.vm.threatMode,
      hidden: ctrl.vm.node.check
    }),
    'data-icon': '7',
    title: 'Show threat (x)',
    onclick: ctrl.toggleThreatMode
  });
}

function engineName(ctrl: CevalController) {
  return [
    window.lichess.engineName,
    ctrl.pnaclSupported ? m('span.native', 'pnacl') : (ctrl.wasmSupported ? m('span.native', 'wasm') : m('span.asmjs', 'asmjs'))
  ];
}

var serverNodes = 4e6;

export function getBestEval(evs: NodeEvals): Eval | undefined {
  var serverEv = evs.server,
    localEv = evs.client;

  if (!serverEv) return localEv;
  if (!localEv) return serverEv;

  // Prefer localEv if it exeeds fishnet node limit or finds a better mate.
  if (localEv.nodes > serverNodes ||
    (typeof localEv.mate !== 'undefined' && (typeof serverEv.mate === 'undefined' || Math.abs(localEv.mate) < Math.abs(serverEv.mate))))
    return localEv;

  return serverEv;
}

export function renderGauge(ctrl: ParentController): Mithril.Renderable {
  if (ctrl.ongoing || !ctrl.showEvalGauge()) return;
  var ev, bestEv = getBestEval(ctrl.currentEvals());
  if (bestEv) {
    ev = winningChances.povChances('white', bestEv);
    gaugeLast = ev;
  } else ev = gaugeLast;
  var height = 100 - (ev + 1) * 50;
  return m('div', {
    class: classSet({
      eval_gauge: true,
      empty: ev === null,
      reverse: ctrl.getOrientation() === 'black'
    })
  }, [
    m('div', {
      class: 'black',
      style: {
        height: height + '%'
      }
    }),
    gaugeTicks
  ]);
}

export function renderCeval(ctrl: ParentController) {
  var instance = ctrl.getCeval();
  if (!instance.allowed() || !instance.possible || !ctrl.vm.showComputer()) return;
  var enabled = instance.enabled();
  var evs = ctrl.currentEvals();
  var bestEv = getBestEval(evs);
  var threatMode = ctrl.vm.threatMode;
  var threat = threatMode && ctrl.vm.node.threat;
  var pearl: Mithril.Renderable, percent: number;
  if (bestEv && typeof bestEv.cp !== 'undefined') {
    pearl = renderEval(bestEv.cp);
    percent = evs.client ? Math.min(100, Math.round(100 * evs.client.depth / (evs.client.maxDepth || instance.effectiveMaxDepth()))) : 0;
  } else if (bestEv && defined(bestEv.mate)) {
    pearl = '#' + bestEv.mate;
    percent = 100;
  } else if (ctrl.gameOver()) {
    pearl = '-';
    percent = 0;
  } else {
    pearl = enabled ? m('span.ddloader') : m('span');
    percent = 0;
  }
  if (threatMode) {
    if (threat) percent = Math.min(100, Math.round(100 * threat.depth / threat.maxDepth));
    else percent = 0;
  }
  var mandatoryCeval = ctrl.mandatoryCeval && ctrl.mandatoryCeval();
  return m('div', {
      class: 'ceval_box ' + (instance.isComputing() ? 'computing' : '')
    },
    enabled ? m('div.bar', m('span', {
      class: threatMode ? 'threat' : '',
      style: {
        width: percent + '%'
      },
      config: function(el: Element, isUpdate: boolean, ctx: any) {
        // reinsert the node to avoid downward animation
        if (isUpdate && (ctx.percent > percent || ctx.threatMode !== threatMode)) {
          var p = el.parentNode;
          p!.removeChild(el);
          p!.appendChild(el);
        }
        ctx.percent = percent;
        ctx.threatMode = threatMode;
      }
    })) : null,
    enabled ? [
      m('pearl', pearl),
      m('div.engine', [
        threatMode ? 'Show threat' : engineName(instance),
        m('span.info', ctrl.gameOver() ? 'Game over.' : (
          threatMode ? threatInfo(threat) : localEvalInfo(ctrl, evs)
        ))
      ])
    ] : [
      pearl ? m('pearl', pearl) : null,
      m('help', [engineName(instance), m('br'), 'in local browser'])
    ],
    mandatoryCeval ? null : m('div.switch', {
      title: 'Toggle local evaluation (l)'
    }, [
      m('input', {
        id: 'analyse-toggle-ceval',
        class: 'cmn-toggle cmn-toggle-round',
        type: 'checkbox',
        checked: enabled,
        onchange: ctrl.toggleCeval
      }),
      m('label', {
        'for': 'analyse-toggle-ceval'
      })
    ]),
    threatButton(ctrl)
  )
}

export function renderPvs(ctrl: ParentController) {
  var instance = ctrl.getCeval();
  if (!instance.allowed() || !instance.possible || !instance.enabled()) return;
  var multiPv = parseInt(instance.multiPv());
  var pvs : Tree.PvData[], threat = false;
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
          var uci = $(e.target).closest('div.pv').attr('data-uci');
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
