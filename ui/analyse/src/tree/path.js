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

  // currentPly: function(path) {
  //   return path[path.length - 1].ply;
  // },

  // withPly: function(path, ply) {
  //   var p2 = path.slice(0);
  //   var last = p2.length - 1;
  //   p2[last] = copy(p2[last], {
  //     ply: ply
  //   });
  //   return p2;
  // },

  // withVariation: function(path, index) {
  //   var p2 = path.slice(0);
  //   var last = p2.length - 1;
  //   var ply = p2[last].ply;
  //   p2[last] = copy(p2[last], {
  //     ply: ply,
  //     variation: index
  //   });
  //   p2.push({
  //     ply: ply,
  //     variation: null
  //   });
  //   return p2;
  // },

  // withoutVariation: function(path) {
  //   var p2 = path.slice(0, path.length - 1);
  //   var last = p2.length - 1;
  //   p2[last] = copy(p2[last], {
  //     variation: null
  //   });
  //   return p2;
  // }
};
