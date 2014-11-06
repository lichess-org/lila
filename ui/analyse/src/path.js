module.exports = {

  default: [{
    ply: 1,
    variation: null
  }],

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

  withPly: function(path, ply) {
    var p2 = path.slice(0);
    p2[p2.length - 1].ply = ply;
    return p2;
  },

  withVariation: function(path, index) {
    var p2 = path.slice(0);
    var last = p2.length - 1;
    p2[last] = {
      ply: p2[last].ply,
      variation: index
    };
    p2.push({
      ply: null,
      variation: null
    });
    return p2;
  }
};
