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

  default: function(ply) {
    return [{
      ply: ply || 0,
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

  isRoot: function(path) {
    return path.length === 1;
  },

  contains: function(p1, p2) {
    if (p2.length < p1.length) return;
    for (var i = 0; i < p2.length; i++) {
      if (!p1[i].variation) return true;
      if (p1[i].ply !== p2[i].ply || p1[i].variation !== p2[i].variation) return false;
    }
    return false;
  },

  currentPly: function(path) {
    return path[path.length - 1].ply;
  },

  withPly: function(path, ply) {
    var p2 = path.slice(0);
    var last = p2.length - 1;
    p2[last] = copy(p2[last], {
      ply: ply
    });
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
  },

  withoutVariation: function(path) {
    var p2 = path.slice(0, path.length - 1);
    var last = p2.length - 1;
    p2[last] = copy(p2[last], {
      variation: null
    });
    return p2;
  }
};
