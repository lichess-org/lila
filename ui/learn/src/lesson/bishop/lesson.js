var util = require('../../util');

module.exports = {
  id: 'bishop',
  title: 'The sniper bishop',
  image: util.assetUrl + 'piece/mono/B.svg',
  stages: [
    require('./stage1')
  ].map(util.incrementalId)
};
