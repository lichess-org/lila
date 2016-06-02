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

function removeChild(parent, id) {
  parent.children = parent.children.filter(function(n) {
    return n.id !== id;
  });
}

function countChildrenAndComments(node) {
  var count = {
    nodes: 1,
    comments: (node.comments || []).length
  };
  node.children.forEach(function(child) {
    var c = countChildrenAndComments(child);
    count.nodes += c.nodes;
    count.comments += c.comments;
  });
  return count;
}

function reconstruct(parts) {
  var root = parts[0],
    node = root;
  root.id = '';
  root.fixed = true;
  for (var i = 1, nb = parts.length; i < nb; i++) {
    var n = parts[i];
    if (node.children) node.children.unshift(n);
    else node.children = [n];
    node = n;
  }
  node.children = node.children || [];
  node.fixed = true;
  return root;
}

// adds n2 into n1
function merge(n1, n2) {
  n1.eval = n2.eval;
  n2.comments && n2.comments.forEach(function(c) {
    if (!n1.comments) n1.comments = [c];
    else if (!n1.comments.filter(function(d) {
      return d.text === c.text;
    }).length) n1.comments.push(c);
  });
  n2.children.forEach(function(c) {
    var existing = childById(n1, c.id);
    if (existing) merge(existing, c);
    else n1.children.push(c);
  });
}

module.exports = {
  findInMainline: findInMainline,
  withMainlineChild: withMainlineChild,
  collect: collect,
  mainlineNodeList: function(from) {
    return collect(from, pickFirstChild);
  },
  childById: childById,
  last: last,
  nodeAtPly: nodeAtPly,
  takePathWhile: takePathWhile,
  removeChild: removeChild,
  countChildrenAndComments: countChildrenAndComments,
  reconstruct: reconstruct,
  merge: merge
}
