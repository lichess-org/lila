var util = require('../../util');

module.exports = {
  id: 'rook',
  title: 'The rook',
  subtitle: 'It moves in straight lines.',
  image: util.assetUrl + 'images/learn/pieces/R.svg',
  stages: [
    require('./stage1'),
    require('./stage2'),
    require('./stage3')
  ].map(util.incrementalId)
};
