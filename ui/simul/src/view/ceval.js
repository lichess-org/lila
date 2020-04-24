var m = require('mithril');
var renderEval = require('draughts').renderEval;
var status = require('game/status');

var gaugeTicks = [1, 2, 3, 4, 5, 6, 7].map(function(i) {
  return m(i === 4 ? 'tick.zero' : 'tick', {
    style: 'height: ' + i * 12.5 + '%'
  });
});

function toPov(color, diff) {
  return color === 'white' ? diff : -diff;
}

function rawWinningChances(cp) {
  return 2 / (1 + Math.exp(-0.004 * cp)) - 1;
}

function cpWinningChances(cp) {
  return rawWinningChances(Math.min(Math.max(-1000, cp), 1000));
}

function winWinningChances(win) {
  var cp = (21 - Math.min(10, Math.abs(win))) * 100;
  var signed = cp * (win > 0 ? 1 : -1);
  return rawWinningChances(signed);
}

function evalWinningChances(ev) {
  return ev.win !== undefined ? winWinningChances(ev.win) : (ev.cp !== undefined ? cpWinningChances(ev.cp) : 0);
}

// winning chances for a color
// 1  infinitely winning
// -1 infinitely losing
function povChances(color, ev) {
  return toPov(color, ev ? evalWinningChances(ev) : 0);
}

module.exports = {
  renderGauge: function(pairing, evals) {
    var eval = evals.find(function (e) { return e.id === pairing.game.id }),
      ev = povChances('white', eval);
    if (!eval && pairing.game.status === status.ids.mate && pairing.winnerColor)
      ev = pairing.winnerColor === 'white' ? 1 : -1;
    var height = 100 - (ev + 1) * 50;
    return m('div.eval_gauge', {
      class: pairing.game.orient === 'black' ? 'reverse' : ''
    }, [
      m('div.black', { style: 'height: ' + height + '%' })
    ].concat(gaugeTicks));
  },
  renderEval: function(eval, pairing, draughtsResult) {
    if (!eval && pairing && pairing.winnerColor)
      // there is no eval in positions where no move is possible, show result
      return pairing.winnerColor === 'white' ? (draughtsResult ? '2-0' : '1-0') : (draughtsResult ? '0-2' : '0-1');
    else if (eval && eval.cp !== undefined)
      return renderEval(pairing.hostColor !== 'white' ? -eval.cp : eval.cp);
    else if (eval && eval.win !== undefined)
      return '#' + (pairing.hostColor !== 'white' ? -eval.win : eval.win);
    return '-';
  },
  compareEval: function(eval, pairing) {
    var ev;
    if (eval && eval.cp !== undefined) {
      ev = pairing.hostColor !== 'white' ? -eval.cp : eval.cp;
    } else if (eval && eval.win !== undefined) {
      ev = pairing.hostColor !== 'white' ? -eval.win : eval.win;
      if (ev >= 0) ev = (1e4 - ev);
      else if (ev < 0) ev = -(1e4 + ev);
    } else {
      if (pairing.winnerColor) ev = pairing.winnerColor === pairing.hostColor ? 1e5 : -1e5;
      else if (!pairing.winnerColor && pairing.game.status === status.ids.draw) ev = 0;
      else ev = -1e6;
    }
    return ev;
  }
}