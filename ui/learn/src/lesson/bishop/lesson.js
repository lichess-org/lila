var util = require('../../util');

module.exports = {
  id: 'bishop',
  title: 'The bishop',
  subtitle: 'It moves in diagonals.',
  image: util.assetUrl + 'images/learn/pieces/B.svg',
  stages: [
    require('./stage1'),
    require('./stage2'),
    require('./stage3')
  ].map(util.incrementalId)
};
