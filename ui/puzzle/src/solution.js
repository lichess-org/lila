var treeOps = require('tree').ops;

module.exports = function(tree, initialNode, solution) {

  tree.ops.updateAll(solution, function(node) {
    node.puzzle = 'good';
  });

  var solutionNode = treeOps.childById(initialNode, solution.id);

  if (solutionNode) treeOps.merge(solutionNode, solution);
  else initialNode.children.push(solution);
};
