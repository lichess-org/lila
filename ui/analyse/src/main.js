var ctrl = require('./ctrl');
var view = require('./view');
var studyView = require('./study/studyView');
var m = require('mithril');

module.exports = function(opts) {

  var controller = new ctrl(opts);

  m.module(opts.element, {
    controller: function () { return controller; },
    view: view
  });

  if (controller.study) m.module(opts.sideElement, {
    controller: function () { return controller.study; },
    view: studyView.main
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
    jumpToNag: controller.jumpToNag
  };
};

// lol, that's for the rest of lichess to access mithril
// without having to include it a second time
window.Chessground = require('chessground');
