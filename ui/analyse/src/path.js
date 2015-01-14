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

  default: function() {
    return [{
      ply: 0,
      variation: null
    }];
  },

  read: function(str) {
    return str.split(',').map(function(step) {
      var s = step.split(':');
      return {
        ply: parseInt(s[0]),
        variation: s[1] ? parseInt(s[1]) : null
      };
    })
  },

  write: function(path) {
    return path.map(function(step) {
      return step.variation ? step.ply + ':' + step.variation : step.ply;
    }).join(',');
  },

  currentPly: function(path) {
    return path[path.length - 1].ply;
  },

  withPly: function(path, ply) {
    var p2 = path.slice(0);
    var last = p2.length - 1;
    p2[last] = copy(p2[last], {ply: ply});
    return p2;
  },

  withVariation: function(path, index) {
    var p2 = path.slice(0);
    var last = p2.length - 1;
    var ply = p2[last].ply;
    p2[last] = copy(p2[last], {
      ply: ply,
      variation: index
    });
    p2.push({
      ply: ply,
      variation: null
    });
    return p2;
  }
};
