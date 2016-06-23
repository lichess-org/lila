var incrementalId = require('../../util').incrementalId;

module.exports = {
  id: 'rook',
  title: 'The mighty rook',
  stages: [
    require('./stage1'),
    require('./stage2')
  ].map(incrementalId)
};
