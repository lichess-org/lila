module.exports = function(tree, initialPath, lines) {

  return function(path, move) {
    var uci = move.orig + move.dest + (move.promotion || '');
    var node = tree.nodeAtPath(path);
    var isNew = node.children.filter(function(c) {
      return c.uci === uci;
    }).length === 0;
    return 'win';
  };
};
