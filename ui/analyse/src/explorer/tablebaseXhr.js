var m = require('mithril');

module.exports = function(endpoint, fen) {
  return m.request({
    background: true,
    method: 'GET',
    url: endpoint,
    data: {
      fen: fen
    }
  });
};
