import { ops as treeOps } from 'tree';

export default function (initialNode: Tree.Node, solution, color: Color): Tree.Node | undefined {

    var mergedSolution = treeOps.mergeExpandedNodes(solution);
    treeOps.updateAll(mergedSolution, function (node) {
        if ((color === 'white') === ((node.displayPly ? node.displayPly : node.ply) % 2 === 1)) node.puzzle = 'good';
    });

    const solutionNode = treeOps.childById(initialNode, mergedSolution.id);

    var merged: Tree.Node | undefined = undefined;
    if (solutionNode) {
        merged = treeOps.merge(solutionNode, mergedSolution, solution);
        if (merged)
            treeOps.updateAll(merged, function (node) {
                if ((color === 'white') === ((node.displayPly ? node.displayPly : node.ply) % 2 === 1)) node.puzzle = 'good';
            });
    }
    else initialNode.children.push(mergedSolution);
    
    return merged;

};
