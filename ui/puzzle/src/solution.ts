import { ops as treeOps } from 'tree';

export default function(initialNode: Tree.Node, solution, color: Color) {

  treeOps.updateAll(solution, function(node) {
    if ((color === 'white') === (node.ply % 2 === 1)) node.puzzle = 'good';
  });

  const solutionNode = treeOps.childById(initialNode, solution.id);

  if (solutionNode) treeOps.merge(solutionNode, solution);
  else initialNode.children.push(solution);
};
