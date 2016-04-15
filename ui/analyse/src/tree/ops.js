function mutateAll(node, mutation) {
  mutation(node);
  node.children.forEach(function(n) {
    mutateAll(n, mutation);
  });
}

function mainlineChild(node) {
  if (node.children.length) return node.children[0];
}

function withMainlineChild(node, f) {
  var next = mainlineChild(node);
  if (next) return f(next);
}

function findInMainline(fromNode, predicate) {
  var findFrom = function(node) {
    if (predicate(node)) return node;
    return withMainlineChild(node, findFrom);
  };
  return findFrom(fromNode);
}

// op: acc => node => acc
function foldRightMainline(acc, node, op) {
  var next = mainlineChild(node);
  if (!next) return op(acc, node);
  return op(acc, foldMainline(acc, next, op));
}

// op: acc => node => acc
function foldLeftMainline(acc, node, op) {
  var next = mainlineChild(node);
  if (!next) return op(acc, node);
  return foldLeftMainline(op(acc, node), next, op);
}

// returns a list of nodes collected from the original one
function collect(from, pickChild) {
  var nodes = [];
  var rec = function(node) {
    nodes.push(node);
    var child = pickChild(node);
    if (child) rec(child);
  };
  rec(from);
  return nodes;
}

function pickFirstChild(node) {
  return node.children[0];
}

function childById(node, id) {
  for (i in node.children)
    if (node.children[i].id === id) return node.children[i];
}

function last(nodeList) {
  return nodeList[nodeList.length - 1];
}

function nodeAtPly(nodeList, ply) {
  for (var i in nodeList)
    if (nodeList[i].ply === ply) return nodeList[i];
}

function takePathWhile(nodeList, predicate) {
  var path = '';
  for (var i in nodeList) {
    if (predicate(nodeList[i])) path += nodeList[i].id;
    else break;
  }
  return path;
}

module.exports = {
  mutateAll: mutateAll,
  findInMainline: findInMainline,
  withMainlineChild: withMainlineChild,
  foldRightMainline: foldRightMainline,
  foldLeftMainline: foldLeftMainline,
  collect: collect,
  mainlineNodeList: function(from) {
    return collect(from, pickFirstChild);
  },
  childById: childById,
  last: last,
  nodeAtPly: nodeAtPly,
  takePathWhile: takePathWhile
}
