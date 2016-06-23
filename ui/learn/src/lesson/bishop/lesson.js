var util = require('../../util');

module.exports = {
  title: 'The bishop',
  subtitle: 'It moves diagonally.',
  image: util.assetUrl + 'images/learn/pieces/B.svg',
  stages: [
    require('./stage1'),
    require('./stage2'),
    require('./stage3')
  ].map(util.incrementalId),
  complete: 'Congratulations! You can command a bishop.<br>' +
    'Shall we proceed to the next level?'
};
