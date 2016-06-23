var util = require('../../util');

module.exports = {
  title: 'The rook',
  subtitle: 'It moves in straight lines.',
  image: util.assetUrl + 'images/learn/pieces/R.svg',
  stages: [
    require('./stage1'),
    require('./stage2'),
    require('./stage3'),
    require('./stage4'),
    require('./stage5')
  ].map(util.incrementalId),
  complete: 'Congratulations! You have successfully mastered the rook.<br>' +
    'Shall we proceed to the next level?'
};
