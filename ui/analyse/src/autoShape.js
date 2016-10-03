var util = require('./util');
var winningChances = require('./winningChances');

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
  var move = util.decomposeUci(uci);
  if (uci[1] === '@') return [{
      orig: move[1],
      brush: brush
    },
    pieceDrop(move[1], util.sanToRole[uci[0].toUpperCase()], color)
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

module.exports = function(ctrl) {
  var n = ctrl.vm.node,
    shapes = [],
    explorerUci = ctrl.explorer.hoveringUci();
  var color = ctrl.chessground.data.movable.color;
  if (explorerUci) shapes = shapes.concat(makeAutoShapesFromUci(color, explorerUci, 'paleBlue'));
  if (ctrl.vm.showAutoShapes()) {
    if (n.eval && n.eval.best)
      shapes = shapes.concat(makeAutoShapesFromUci(color, n.eval.best, 'paleGreen'));
    if (!explorerUci) {
      var nextNodeBest = ctrl.nextNodeBest();
      if (nextNodeBest) shapes = shapes.concat(makeAutoShapesFromUci(color, nextNodeBest, 'paleBlue'));
      else if (ctrl.ceval.enabled() && n.ceval && n.ceval.best)
        shapes = shapes.concat(makeAutoShapesFromUci(color, n.ceval.best, 'paleBlue'));
      if (ctrl.ceval.enabled() && n.ceval && n.ceval.pvs && n.ceval.pvs[1]) {
        n.ceval.pvs.slice(1).forEach(function(pv) {
          var shift = winningChances.povDiff(color, n.ceval.pvs[0], pv);
          if (isNaN(shift) || shift < 0) console.log('------------------', shift, n.ceval.pvs);
          // console.log(shift);
          if (shift > 0.2) return;
          // 12 to 2
          var width = Math.round(12 - shift * 50);
          // console.log(shift, width);
          shapes = shapes.concat(makeAutoShapesFromUci(color, pv.best, 'paleGrey', {
            lineWidth: width
          }));
        });
      }
    }
  }
  if (ctrl.ceval.enabled() && ctrl.vm.threatMode && n.threat && n.threat.best)
    shapes = shapes.concat(makeAutoShapesFromUci(color === 'white' ? 'black' : 'white', n.threat.best, 'red'));
  return shapes;
};
