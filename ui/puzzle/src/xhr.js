var m = require('mithril');

// do NOT set mobile API headers here
// they trigger a compat layer
module.exports = {
  round: function(puzzleId, win) {
    return $.ajax({
      method: 'POST',
      url: '/training/' + puzzleId + '/round2',
      data: {
        win: win ? 1 : 0
      }
    });
  },
  vote: function(puzzleId, v) {
    return $.ajax({
      method: 'POST',
      url: '/training/' + puzzleId + '/vote',
      data: {
        vote: v ? 1 : 0
      }
    });
  },
  nextPuzzle: function() {
    return $.ajax({
      url: '/training/new'
    });
  }
};
