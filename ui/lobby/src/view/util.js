var m = require('mithril');

module.exports = {
  tds: function(bits) {
    return bits.map(function(bit) {
      return {
        tag: 'td',
        children: [bit]
      };
    });
  }
};
