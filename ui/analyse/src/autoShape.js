function decomposeUci(uci) {
  return [uci.slice(0, 2), uci.slice(2, 4), uci.slice(4, 5)];
};

function makeAutoShapesFromUci(color, uci, brush) {
  var move = decomposeUci(uci);
  if (uci[1] === '@') return [{
    orig: move[1],
    brush: brush
  }, {
    orig: move[1],
    piece: {
      color: color,
      role: util.sanToRole[uci[0].toUpperCase()],
      scale: 0.8
    }
  }];
  return [{
    orig: move[0],
    dest: move[1],
    brush: brush
  }];
}

module.exports = function(ctrl) {
  var n = ctrl.vm.node,
    shapes = [],
    explorerUci = ctrl.explorer.hoveringUci();
  var color = ctrl.chessground.data.movable.color;
  if (explorerUci) shapes = shapes.concat(makeAutoShapesFromUci(color, explorerUci, 'paleBlue'));
  if (ctrl.vm.showAutoShapes()) {
    if (n.eval && n.eval.best) shapes = shapes.concat(makeAutoShapesFromUci(color, n.eval.best, 'paleGreen'));
    if (!explorerUci) {
      var nextNodeBest = ctrl.nextNodeBest();
      if (nextNodeBest) shapes = shapes.concat(makeAutoShapesFromUci(color, nextNodeBest, 'paleBlue'));
      else if (ctrl.ceval.enabled() && n.ceval && n.ceval.best) shapes = shapes.concat(makeAutoShapesFromUci(color, n.ceval.best, 'paleBlue'));
    }
  }
  return shapes;
};
