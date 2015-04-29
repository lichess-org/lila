var piotr2key = require('game').piotr.piotr2key;

module.exports = {
  readDests: function(lines) {
    var dests = {};
    lines.split(' ').forEach(function(line) {
      dests[piotr2key[line[0]]] = line.split('').slice(1).map(function(c) {
        return piotr2key[c];
      });
    });
    return dests;
  }
};
