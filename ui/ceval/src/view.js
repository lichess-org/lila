var m = require('mithril');
var winningChances = require('./winningChances');
var defined = require('common').defined;
var classSet = require('common').classSet;
var renderEval = require('chess').renderEval;
var pv2san = require('./pv2san');

var gaugeLast = 0;
var gaugeTicks = [];
for (var i = 1; i < 8; i++) gaugeTicks.push(m(i === 4 ? 'tick.zero' : 'tick', {
  style: {
    height: (i * 12.5) + '%'
  }
}));

function range(len) {
  var r = [];
  for (var i = 0; i < len; i++) r.push(i);
  return r;
}

function localEvalInfo(ctrl, evs) {
  var ceval = ctrl.getCeval();
  if (!evs.client) {
    if (evs.server && ctrl.nextNodeBest()) return 'Using server analysis';
    return 'Loading engine...';
  }
  var t = evs.client.cloud ? [
    [
      'Depth ' + (evs.client.depth || 0),
      m('span.cloud', {
        title: 'Cloud Analysis'
      }, 'cloud')
    ]
  ] : [
    'Depth ' + (evs.client.depth || 0) + '/' + evs.client.maxDepth
  ];
  if (ceval.canGoDeeper() && (
      evs.client.depth >= (evs.client.maxDepth || ceval.effectiveMaxDepth())
    ))
    t.push(m('a.deeper', {
      title: 'Go deeper',
      'data-icon': 'O',
      onclick: ceval.goDeeper
    }))
  else if (!evs.client.cloud && evs.client.knps) t.push(', ' + Math.round(evs.client.knps) + ' knodes/s');
  return t;
}

function threatInfo(threat) {
  if (!threat) return 'Loading engine...';
  var t = 'Depth ' + (threat.depth || 0) + '/' + threat.maxDepth;
  if (threat.knps) t += ', ' + Math.round(threat.knps) + ' knodes/s';
  return t;
}

function threatButton(ctrl) {
  if (!ctrl.disableThreatMode || !ctrl.disableThreatMode()) return m('a', {
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

function engineName(ctrl) {
  return [
    lichess.engineName,
    ctrl.pnaclSupported ? m('span.native', 'native') : m('span.asmjs', 'asmjs')
  ];
}

var serverNodes = 3.5e6;

function getBestEval(evs) {
  var serverEv = evs.server,
    localEv = evs.client;

  if (!serverEv) return localEv;
  if (!localEv) return serverEv;

  // Prefer localEv if it exeeds fishnet node limit or finds a better mate.
  if (localEv.nodes > serverNodes ||
    (localEv.mate && !(Math.abs(serverEv.mate) < Math.abs(localEv.mate))))
    return localEv;

  return serverEv;
}

module.exports = {
  getBestEval: getBestEval,
  renderGauge: function(ctrl) {
    if (ctrl.ongoing || !ctrl.showEvalGauge()) return;
    var eval, bestEv = getBestEval(ctrl.currentEvals());
    if (bestEv) {
      eval = winningChances.povChances('white', bestEv);
      gaugeLast = eval;
    } else eval = gaugeLast;
    var height = 100 - (eval + 1) * 50;
    return m('div', {
      class: classSet({
        eval_gauge: true,
        empty: eval === null,
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
  },
  renderCeval: function(ctrl) {
    var instance = ctrl.getCeval();
    if (!instance.allowed() || !instance.possible || !ctrl.vm.showComputer()) return;
    var enabled = instance.enabled();
    var evs = ctrl.currentEvals();
    var bestEv = getBestEval(evs);
    var threatMode = ctrl.vm.threatMode;
    var threat = threatMode && ctrl.vm.node.threat;
    var pearl, percent;
    if (bestEv && defined(bestEv.cp)) {
      pearl = renderEval(bestEv.cp);
      percent = ctrl.nextNodeBest() ?
        100 :
        (evs.client ? Math.min(100, Math.round(100 * evs.client.depth / (evs.client.maxDepth || instance.effectiveMaxDepth()))) : 0)
    } else if (bestEv && defined(bestEv.mate)) {
      pearl = '#' + bestEv.mate;
      percent = 100;
    } else if (ctrl.gameOver()) {
      pearl = '-';
      percent = 0;
    } else {
      pearl = m('span.cpu', 'CPU');
      percent = 0;
    }
    if (threatMode) {
      if (threat) percent = Math.min(100, Math.round(100 * threat.depth / threat.maxDepth));
      else percent = 0;
    }
    var mandatoryCeval = ctrl.mandatoryCeval && ctrl.mandatoryCeval();
    return m('div.ceval_box',
      enabled ? m('div.bar', m('span', {
        class: threatMode ? 'threat' : '',
        style: {
          width: percent + '%'
        },
        config: function(el, isUpdate, ctx) {
          // reinsert the node to avoid downward animation
          if (isUpdate && (ctx.percent > percent || ctx.threatMode !== threatMode)) {
            var p = el.parentNode;
            p.removeChild(el);
            p.appendChild(el);
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
        m('help',
          engineName(instance),
          m('br'),
          'in local browser'
        )
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
  },
  renderPvs: function(ctrl) {
    var instance = ctrl.getCeval();
    if (!instance.allowed() || !instance.possible || !instance.enabled()) return;
    var multiPv = instance.multiPv();
    var pvs, threat = false;
    if (ctrl.vm.threatMode && ctrl.vm.node.threat) {
      pvs = ctrl.vm.node.threat.pvs;
      threat = true;
    } else if (ctrl.vm.node.ceval)
      pvs = ctrl.vm.node.ceval.pvs;
    else
      pvs = [];
    return m('div.pv_box', {
      'data-fen': ctrl.vm.node.fen,
      config: function(el, isUpdate, ctx) {
        if (!isUpdate) {
          el.addEventListener('mouseover', function(e) {
            instance.setHovering($(el).attr('data-fen'), $(e.target).closest('div.pv').attr('data-uci'));
          });
          el.addEventListener('mouseout', function(e) {
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
        multiPv > 1 ? m('strong', defined(pvs[i].mate) ? ('#' + pvs[i].mate) : renderEval(pvs[i].cp)) : null,
        m('span', pv2san(instance.variant.key, ctrl.vm.node.fen, threat, pvs[i].moves, pvs[i].mate))
      ]);
    }));
  }
};
