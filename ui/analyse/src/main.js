var ctrl = require('./ctrl');
var view = require('./view');
var studyView = require('./study/studyView');
var studyPracticeView = require('./study/practice/studyPracticeView');
var boot = require('./boot');
var m = require('mithril');

module.exports = {
  mithril: function(opts) {

    var controller = new ctrl(opts);

    m.module(opts.element, {
      controller: function() {
        return controller;
      },
      view: view
    });

    if (controller.study && opts.sideElement) m.module(opts.sideElement, {
      controller: function() {
        m.redraw.strategy("diff"); // prevents double full redraw on page load
        return controller.study;
      },
      view: controller.studyPractice ? studyPracticeView.main : studyView.main
    });

    return {
      socketReceive: controller.socket.receive,
      jumpToIndex: function(index) {
        controller.jumpToIndex(index);
        m.redraw();
      },
      path: function() {
        return controller.vm.path;
      },
      pathStr: function() {
        return controller.vm.pathStr;
      },
      setChapter: function(id) {
        if (controller.study) controller.study.setChapter(id);
      }
    }
  },
  boot: boot
};

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = require('chessground').Chessground;
