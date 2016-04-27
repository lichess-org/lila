function copy(obj, newValues) {
  var k, c = {};
  for (k in obj) {
    c[k] = obj[k];
  }
  for (k in newValues) {
    c[k] = newValues[k];
  }
  return c;
}

module.exports = {

  root: '',

  size: function(path) {
    return path.length / 2;
  },

  head: function(path) {
    return path.slice(0, 2);
  },

  tail: function(path) {
    return path.slice(2);
  },

  init: function(path) {
    return path.slice(0, -2);
  },

  last: function(path) {
    return path.slice(-2);
  },

  take: function(path, plies) {
    return path.slice(0, 2 * plies);
  },

  isRoot: function(path) {
    return path === '';
  },

  contains: function(p1, p2) {
    return p1.indexOf(p2) === 0;
  },

  fromNodeList: function(nodes) {
    return nodes.map(function(n) {
      return n.id;
    }).join('');
  }
};
