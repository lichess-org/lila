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

  var getCurrentNodesAfterPly = function(nodeList, mainline, ply) {
    var node, nodes = [];
    for (var i in nodeList) {
      node = nodeList[i];
      if (node.ply <= ply && mainline[i].id !== node.id) break;
      if (node.ply > ply) nodes.push(node);
    }
    return nodes;
  };

  function pathIsMainline(path) {
    return pathIsMainlineFrom(root, path);
  }

  function pathExists(path) {
    return !!nodeAtPath(path);
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
    if (nodeAtPathOrNull(newPath)) return newPath;
    if (updateAt(path, function(parent) {
      parent.children.push(node);
    })) return newPath;
  }

  function deleteNodeAt(path) {
    var parent = nodeAtPath(treePath.init(path));
    var id = treePath.last(path);
    ops.removeChild(parent, id);
  }

  function promoteNodeAt(path) {
    var nodes = getNodeList(path);
    for (var i = nodes.length - 2; i >= 0; i--) {
      var node = nodes[i + 1];
      var parent = nodes[i];
      if (parent.children[0].id !== node.id) {
        parent.children = parent.children.filter(function(c) {
          return c.id !== node.id;
        });
        parent.children.unshift(node);
      }
    }
  }

  function setCommentAt(comment, path) {
    updateAt(path, function(node) {
      node.comments = node.comments || [];
      var existing = node.comments.find(function(c) {
        return c.by === comment.by;
      });
      if (existing) existing.text = comment.text;
      else node.comments.push(comment);
      node.comments = node.comments.filter(function(c) {
        return c.text !== '';
      });
    });
  }

  function setGlyphsAt(glyphs, path) {
    updateAt(path, function(node) {
      node.glyphs = glyphs;
    });
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
    setShapes: function(shapes, path) {
      return updateAt(path, function(node) {
        node.shapes = shapes;
      });
    },
    setCommentAt: setCommentAt,
    setGlyphsAt: setGlyphsAt,
    pathIsMainline: pathIsMainline,
    pathExists: pathExists,
    deleteNodeAt: deleteNodeAt,
    promoteNodeAt: promoteNodeAt,
    plyOfNextNag: function(color, nag, mainline, fromPly) {
      var len = mainline.length;
      for (var i = 1; i < len; i++) {
        var ply = (fromPly + i) % len;
        if (mainline[ply].nag === nag && (ply % 2 === (color === 'white' ? 1 : 0))) return ply;
      }
    },
    getCurrentNodesAfterPly: getCurrentNodesAfterPly
  };
}
