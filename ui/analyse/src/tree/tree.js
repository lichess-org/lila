var treePath = require('./path');
var ops = require('./ops');

module.exports = function(root) {

  root.id = '';
  ops.mutateAll(root, function(node) {
    node.fixed = true;
  });

  function firstPly() {
    return root.ply;
  };

  function lastNode() {
    return ops.findInMainline(root, function(node) {
      return !node.children.length;
    });
  };

  function lastPly() {
    return lastNode().ply;
  }

  function nodeAtPath(path) {
    return nodeAtPathFrom(root, path);
  }

  function nodeAtPathFrom(node, path) {
    if (path === '') return node;
    var child = ops.childById(node, treePath.head(path));
    return child ? nodeAtPathFrom(child, treePath.tail(path)) : node;
  }

  function nodeAtPathOrNull(path) {
    return nodeAtPathOrNullFrom(root, path);
  }

  function nodeAtPathOrNullFrom(node, path) {
    if (path === '') return node;
    var child = ops.childById(node, treePath.head(path));
    if (child) return nodeAtPathOrNullFrom(child, treePath.tail(path));
  }

  // function mainlineNodeAtPathOrNull(path) {
  //   return mainlineNodeAtPathFrom(root, path);
  // }

  // function mainlineNodeAtPathOrNullFrom(node, path) {
  //   if (path === '') return node;
  //   var child = ops.mainlineChild(node);
  //   if (!child) return null;
  //   if (child.id !== treePath.head(path)) return null;
  //   return mainlineNodeAtPathOrNullFrom(child, treePath.tail(path));
  // }

  // this.nextNodeEvalBest = function(path) {
  //   if (!treePath.isRoot(path)) return;
  //   var nextPly = path[0].ply + 1;
  //   var nextStep = this.tree[nextPly - this.firstPly()];
  //   return (nextStep && nextStep.eval) ? nextStep.eval.best : null;
  // }.bind(this);

  // this.nextNodeEvalBest = function(path) {
  //   var node = mainlineNodeAtPathOrNullFrom(path)
  //   var nextPly = path[0].ply + 1;
  //   var nextStep = this.tree[nextPly - this.firstPly()];
  //   return (nextStep && nextStep.eval) ? nextStep.eval.best : null;
  // }.bind(this);

  function pathIsMainline(path) {
    return pathIsMainlineFrom(root, path);
  }

  function pathIsMainlineFrom(node, path) {
    if (path === '') return true;
    var pathId = treePath.head(path);
    var child = node.children[0];
    if (!child || child.id !== pathId) return false;
    return pathIsMainlineFrom(child, treePath.tail(path));
  }

  function getNodeList(path) {
    return ops.collect(root, function(node) {
      var id = treePath.head(path);
      if (id === '') return null;
      path = treePath.tail(path);
      return ops.childById(node, id);
    });
  }

  function getOpening(nodeList) {
    var opening;
    nodeList.forEach(function(node) {
      opening = node.opening || opening;
    });
    return opening;
  }

  function updateAt(path, update) {
    var node = nodeAtPathOrNull(path);
    if (node) {
      update(node);
      return node;
    }
  }

  // returns new path
  function addNode(node, path) {
    var newPath = path + node.id;
    if (!nodeAtPathOrNull(newPath))
      updateAt(path, function(parent) {
        parent.children.push(node);
      });
    return newPath;
  }

  function deleteNodeAt(path) {
    var parent = nodeAtPath(treePath.init(path));
    var id = treePath.last(path);
    ops.removeChild(parent, id);
  }

  function promoteVariation(path) {
    var parent = nodeAtPath(treePath.init(path));
    var id = treePath.last(path);
    var child = ops.childById(parent, id);
    ops.removeChild(parent, id);
    parent.children.unshift(child);
  }

  return {
    root: root,
    ops: ops,
    firstPly: firstPly,
    lastPly: lastPly,
    nodeAtPath: nodeAtPath,
    getNodeList: getNodeList,
    getOpening: getOpening,
    updateAt: updateAt,
    addNode: addNode,
    addDests: function(dests, path, opening) {
      return updateAt(path, function(node) {
        node.dests = dests;
        if (opening) node.opening = opening;
      });
    },
    pathIsMainline: pathIsMainline,
    deleteNodeAt: deleteNodeAt,
    promoteVariation: promoteVariation
  };

  //   this.getStep = function(path) {
  //     var tree = this.tree;
  //     for (var j in path) {
  //       var p = path[j];
  //       for (var i = 0, nb = tree.length; i < nb; i++) {
  //         if (p.ply === tree[i].ply) {
  //           if (p.variation) {
  //             tree = tree[i].variations[p.variation - 1];
  //             break;
  //           }
  //           return tree[i];
  //         }
  //       }
  //     }
  //   };
  //   this.getStepAtPly = function(ply) {
  //     return this.getStep(treePath.default(ply));
  //   }.bind(this);


  //   this.getStepsAfterPly = function(path, ply) {
  //     if (path[0].ply <= ply) return [];
  //     return this.getSteps(path).filter(function(step) {
  //       return step.ply > ply;
  //     });
  //   }.bind(this);


  //   this.addStep = function(step, path) {
  //     var nextPath = treePath.withPly(path, treePath.currentPly(path) + 1);
  //     var tree = this.tree;
  //     var curStep = null;
  //     nextPath.forEach(function(p) {
  //       for (i = 0, nb = tree.length; i < nb; i++) {
  //         var step = tree[i];
  //         if (p.ply === step.ply) {
  //           if (p.variation) {
  //             tree = step.variations[p.variation - 1];
  //             break;
  //           } else curStep = step;
  //         } else if (p.ply < step.ply) break;
  //       }
  //     });
  //     if (curStep) {
  //       curStep.variations = curStep.variations || [];
  //       if (curStep.san === step.san) return nextPath;
  //       for (var i = 0; i < curStep.variations.length; i++) {
  //         if (curStep.variations[i][0].san === step.san)
  //           return treePath.withVariation(nextPath, i + 1);
  //       }
  //       curStep.variations.push([step]);
  //       return treePath.withVariation(nextPath, curStep.variations.length);
  //     }
  //     tree.push(step);
  //     return nextPath;
  //   }.bind(this);

  //   this.addSteps = function(steps, path) {
  //     var step = steps[0];
  //     if (!step) return path;
  //     var newPath = this.addStep(step, path);
  //     return this.addSteps(steps.slice(1), newPath);
  //   }.bind(this);

  //   this.plyOfNextNag = function(color, nag, fromPly) {
  //     var len = this.tree.length;
  //     for (var i = 1; i < len; i++) {
  //       var ply = (fromPly + i) % len;
  //       if(this.tree[ply].nag === nag && (ply % 2 === (color === 'white' ? 1 : 0))) return ply;
  //     }
  //   }.bind(this);
}
