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

  setPly: function(path, ply) {
    path[path.length -1].ply = ply;
  },

  withVariation: function(path, index) {
    var p2 = path.slice(0);
    var last = p2.length - 1;
    p2[last] = {
      ply: p2[last].ply - 1,
      variation: index
    };
    p2.push({
      ply: null,
      variation: null
    });
    return p2;
  }
};
