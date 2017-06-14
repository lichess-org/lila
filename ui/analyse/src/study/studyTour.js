var m = require('mithril');

module.exports = {
  study: function(ctrl) {
    lichess.loadScript('/assets/javascripts/study/tour.js').then(function() {
      lichess.studyTour({
        userId: ctrl.userId,
        isContrib: ctrl.study.members.canContribute(),
        setTab: function(tab) {
          ctrl.study.vm.tab(tab);
          m.redraw();
        }
      });
    });
  },
  chapter: function(setTab) {
    lichess.loadScript('/assets/javascripts/study/tour-chapter.js').then(function() {
      lichess.studyTourChapter({
        setTab: function(tab) {
          setTab(tab);
          m.redraw();
        }
      });
    });
  }
};
