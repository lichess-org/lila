var m = require('mithril');
var xhr = require('../studyXhr');
var embedYoutube = require('../studyComments').embedYoutube;

var readOnlyProp = function(value) {
  return function() {
    return value;
  };
};

var commentYoutubeRegex = /(?:https?:\/\/)(?:www\.)(?:youtube\.com|youtu\.be)\/(?:watch)?(?:\?v=)?([^"&?\/ ]{11})/gi;

module.exports = function(root, studyData, data) {

  var goal = m.prop();
  var comment = m.prop();
  var nbMoves = m.prop(0);
  var won = m.prop(false);

  var makeComment = function(treeRoot) {
    if (!treeRoot.comments) return;
    var c = embedYoutube(treeRoot.comments[0].text);
    delete treeRoot.comments;
    return c;
  };

  var onLoad = function() {
    root.vm.showAutoShapes = readOnlyProp(true);
    root.vm.showGauge = readOnlyProp(true);
    root.vm.showComputer = readOnlyProp(true);
    goal(root.data.practiceGoal);
    nbMoves(0);
    won(false);
    comment(makeComment(root.tree.root));
  };
  onLoad();

  var isDrawish = function(eval) {
    return eval && eval.depth >= 16 && !eval.mate && Math.abs(eval.cp) < 200;
  };
  var isWinning = function(eval, goalCp) {
    if (!eval || eval.depth < 16) return;
    var cp = eval.mate > 0 ? 9999 : (eval.mate < 0 ? -9999 : eval.cp);
    return goalCp > 0 ? cp >= goalCp : cp <= goalCp;
  };

  var checkVictory = function() {
    if (won()) return;
    var n = root.vm.node,
      g = goal();
    nbMoves(Math.floor(n.ply / 2));
    var isVictory = false;
    switch (g.result) {
      case 'drawIn':
        isVictory = root.gameOver() === 'draw' || (nbMoves() >= g.moves && isDrawish(n.ceval));
        break;
      case 'evalIn':
        isVictory = nbMoves() >= g.moves && isWinning(n.ceval, g.cp);
        break;
      case 'mate':
      default:
        isVictory = root.gameOver() === 'checkmate' && root.turnColor() !== root.bottomColor();
    }
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
