var incrementalId = require('../../util').incrementalId;

module.exports = {
  id: 'bishop',
  title: 'The sniper bishop',
  stages: [
    require('./stage1')
  ].map(incrementalId)
};
