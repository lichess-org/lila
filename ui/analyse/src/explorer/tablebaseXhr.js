var m = require('mithril');

module.exports = function(fen) {
  return m.request({
    background: true,
    method: 'GET',
    url: 'https://syzygy-tables.info/api/v2',
    data: {
      fen: fen
    }
  });
};
