var ctrl = require('./ctrl');
var view = require('./view');
var studyView = require('./study/studyView');
var computerAnalysisView = require('./computerAnalysisView');
var m = require('mithril');

module.exports = function(opts) {

  var controller = new ctrl(opts);

  m.module(opts.element, {
    controller: function() {
      return controller;
    },
    view: view
  });

  if (controller.study) m.module(opts.sideElement, {
    controller: function() {
      m.redraw.strategy("diff"); // prevents double full redraw on page load
      return controller.study;
    },
    view: studyView.main
  });

  if (opts.computerAnalysisElement) m.module(opts.computerAnalysisElement, {
    controller: function() {
      m.redraw.strategy("diff");
      return controller;
    },
    view: computerAnalysisView
  });

  return {
    socketReceive: controller.socketReceive,
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
    jumpToGlyphSymbol: controller.jumpToGlyphSymbol
  };
};

// lol, that's for the rest of lichess to access mithril
// without having to include it a second time
window.Chessground = require('chessground');
