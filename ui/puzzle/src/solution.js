var treeOps = require('tree').ops;

module.exports = function(tree, initialPath, solution) {

  var initialNode = tree.nodeAtPath(initialPath);
  var solutionNode = treeOps.childById(initialNode, solution.id);

  if (solutionNode) treeOps.merge(solutionNode, solution);
  else initialNode.children.push(solution);
};
