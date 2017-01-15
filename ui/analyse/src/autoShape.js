var winningChances = require('ceval').winningChances;
var decomposeUci = require('chess').decomposeUci;
var sanToRole = require('chess').sanToRole;

function pieceDrop(key, role, color) {
  return {
    orig: key,
    piece: {
      color: color,
      role: role,
      scale: 0.8
    }
  };
}

function makeAutoShapesFromUci(color, uci, brush, modifiers) {
  var move = decomposeUci(uci);
  if (uci[1] === '@') return [{
      orig: move[1],
      brush: brush
    },
    pieceDrop(move[1], sanToRole[uci[0].toUpperCase()], color)
  ];
  var shapes = [{
    orig: move[0],
    dest: move[1],
    brush: brush,
    brushModifiers: modifiers
  }];
  if (move[2]) shapes.push(pieceDrop(move[1], move[2], color));
  return shapes;
}

module.exports = {
  makeAutoShapesFromUci: makeAutoShapesFromUci,
  compute: function(ctrl) {
    var instance = ctrl.getCeval(),
      n = ctrl.vm.node,
      shapes = [],
      hoveringUci = ctrl.explorer.hoveringUci() || instance.hoveringUci();
    var color = ctrl.chessground.data.movable.color;
    var rcolor = color === 'white' ? 'black' : 'white';
    if (ctrl.retro && ctrl.retro.showBadNode()) {
      return makeAutoShapesFromUci(color, ctrl.retro.showBadNode().uci, 'paleRed', {
        lineWidth: 8
      });
    }
    if (hoveringUci) shapes = shapes.concat(makeAutoShapesFromUci(color, hoveringUci, 'paleBlue'));
    if (ctrl.vm.showAutoShapes() && ctrl.vm.showComputer()) {
      if (n.eval && n.eval.best)
        shapes = shapes.concat(makeAutoShapesFromUci(rcolor, n.eval.best, 'paleGreen'));
      if (!hoveringUci) {
        var nextBest = ctrl.nextNodeBest();
        if (!nextBest && instance.enabled() && n.ceval && n.ceval.best) nextBest = n.ceval.best;
        if (nextBest) shapes = shapes.concat(makeAutoShapesFromUci(color, nextBest, 'paleBlue'));
        if (instance.enabled() && n.ceval && n.ceval.pvs && n.ceval.pvs[1] && !(ctrl.vm.threatMode && n.threat && n.threat.pvs && n.threat.pvs[2])) {
          n.ceval.pvs.forEach(function(pv) {
            if (pv.best === nextBest) return;
            var shift = winningChances.povDiff(color, n.ceval.pvs[0], pv);
            if (shift > 0.2 || isNaN(shift) || shift < 0) return;
            shapes = shapes.concat(makeAutoShapesFromUci(color, pv.best, 'paleGrey', {
              lineWidth: Math.round(12 - shift * 50) // 12 to 2
            }));
          });
        }
      }
    }
    if (instance.enabled() && ctrl.vm.threatMode && n.threat && n.threat.best) {
      if (n.threat.pvs[1]) {
        shapes = shapes.concat(makeAutoShapesFromUci(rcolor, n.threat.best, 'paleRed'));
        n.threat.pvs.slice(1).forEach(function(pv) {
          var shift = winningChances.povDiff(rcolor, pv, n.threat.pvs[0]);
          if (shift > 0.2 || isNaN(shift) || shift < 0) return;
          shapes = shapes.concat(makeAutoShapesFromUci(rcolor, pv.best, 'paleRed', {
            lineWidth: Math.round(11 - shift * 45) // 11 to 2
          }));
        });
      } else
        shapes = shapes.concat(makeAutoShapesFromUci(rcolor, n.threat.best, 'red'));
    }
    return shapes;
  }
};
