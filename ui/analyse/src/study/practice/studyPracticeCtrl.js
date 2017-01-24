var m = require('mithril');
var xhr = require('../studyXhr');

var readOnlyProp = function(value) {
  return function() {
    return value;
  };
};

var drawInRegex = /draw in (\d+)/i;

module.exports = function(root, studyData, data) {

  var goal = m.prop();
  var comment = m.prop();
  var nbMoves = m.prop(0);
  var won = m.prop(false);

  var findGoal = function() {
    var tags = studyData.chapter.tags;
    for (var i in tags)
      if (tags[i][0] === 'Termination') {
        var nbMoves = parseInt(tags[i][1].replace(drawInRegex, '$1'));
        if (nbMoves) return {
          result: 'draw',
          nbMoves: nbMoves
        };
      }
    return {
      result: 'checkmate'
    };
  };

  var onLoad = function() {
    root.vm.showAutoShapes = readOnlyProp(true);
    root.vm.showGauge = readOnlyProp(true);
    root.vm.showComputer = readOnlyProp(true);
    goal(findGoal());
    nbMoves(0);
    won(false);
    var r = root.tree.root;
    if (r.comments) {
      comment(r.comments[0].text);
      delete r.comments;
    }
  };
  onLoad();

  var checkVictory = function() {
    if (won()) return;
    var n = root.vm.node;
    nbMoves(Math.floor(n.ply / 2));
    var isVictory = goal().result === 'checkmate' ? (
      root.gameOver() === 'checkmate' && root.turnColor() !== root.bottomColor()
    ) : (
      root.gameOver() === 'draw' || (
        nbMoves() >= goal().nbMoves && n.ceval && n.ceval.depth >= 16 && Math.abs(n.ceval.cp) < 200
      )
    );
    if (!isVictory) return;
    nbMoves(Math.max(1, nbMoves()));
    won(true);
    var chapterId = root.study.currentChapter().id;
    var former = data.completion[chapterId] || 999;
    if (nbMoves() < former) {
      data.completion[chapterId] = nbMoves();
      xhr.practiceComplete(chapterId, nbMoves());
    }
  };

  var findNextOngoingChapter = function() {
    return root.study.data.chapters.filter(function(c) {
      return !data.completion[c.id];
    })[0];
  };
  var findNextChapter = function() {
    var chapters = root.study.data.chapters;
    var currentId = root.study.currentChapter().id;
    for (var i in chapters)
      if (chapters[i].id === currentId) return chapters[(i + 1) % chapters.length];
    return chapters[0];
  };

  return {
    onReload: onLoad,
    onJump: checkVictory,
    onCeval: checkVictory,
    data: data,
    goal: goal,
    won: won,
    comment: comment,
    nbMoves: nbMoves,
    nextChapter: function() {
      return findNextOngoingChapter() || findNextChapter();
    }
  };
};
