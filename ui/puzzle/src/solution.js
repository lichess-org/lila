var treeOps = require('tree').ops;

module.exports = function(tree, initialNode, solution, color) {

  tree.ops.updateAll(solution, function(node) {
    if ((color === 'white') === (node.ply % 2 === 1)) node.puzzle = 'good';
  });

  var solutionNode = treeOps.childById(initialNode, solution.id);

  if (solutionNode) treeOps.merge(solutionNode, solution);
  else initialNode.children.push(solution);
};
