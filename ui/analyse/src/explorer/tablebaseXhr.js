var m = require('mithril');

module.exports = function(endpoint, fen) {
  endpoint = '//expl.lichess.org/tablebase'; // PROD FIX HACK, delete me
  return m.request({
    background: true,
    method: 'GET',
    url: endpoint,
    data: {
      fen: fen
    }
  });
};
