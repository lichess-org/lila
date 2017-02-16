var m = require('mithril');
var xhr = require('../studyXhr');
var embedYoutube = require('../studyComments').embedYoutube;
var makeSuccess = require('./studyPracticeSuccess');
var makeSound = require('./sound');

var readOnlyProp = function(value) {
  return function() {
    return value;
  };
};

module.exports = function(root, studyData, data) {

  var goal = m.prop();
  var comment = m.prop();
  var nbMoves = m.prop(0);
  var success = m.prop(null); // null = ongoing, true = win, false = fail
  var sound = makeSound();

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
    success(null);
    comment(makeComment(root.tree.root));
    var chapter = studyData.chapter;
    history.replaceState(null, chapter.name, data.url + '/' + chapter.id);
  };
  onLoad();

  var computeNbMoves = function() {
    var plies = root.vm.node.ply - root.tree.root.ply;
    if (root.bottomColor() !== root.data.player.color) plies--;
    return Math.ceil(plies / 2);
  };

  var checkSuccess = function() {
    if (success() !== null) return;
    nbMoves(computeNbMoves());
    var res = success(makeSuccess(root, goal(), nbMoves()));
    if (res) onVictory();
    else if (res === false) onFailure();
  };

  var onVictory = function() {
    var chapterId = root.study.currentChapter().id;
    var former = data.completion[chapterId] || 999;
    if (nbMoves() < former) {
      data.completion[chapterId] = nbMoves();
      xhr.practiceComplete(chapterId, nbMoves());
    }
    sound.success();
    var next = nextChapter();
    if (next) root.study.setChapter(next.id);
  };

  var onFailure = function() {
    sound.failure();
  };

  var nextChapter = function() {
    var chapters = root.study.data.chapters;
    var currentId = root.study.currentChapter().id;
    for (var i in chapters)
      if (chapters[i].id === currentId) return chapters[parseInt(i) + 1];
  };

  return {
    onReload: onLoad,
    onJump: checkSuccess,
    onCeval: checkSuccess,
    data: data,
    goal: goal,
    success: success,
    comment: comment,
    nbMoves: nbMoves,
    reset: function() {
      root.tree.root.children = [];
      root.userJump('');
      root.practice.reset();
      onLoad();
    },
    isWhite: function() {
      return root.bottomColor() === 'white';
    }
  };
};
