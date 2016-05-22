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
  },
  offline: function() {
    lichess.shepherd(function(theme) {
      var tour = new Shepherd.Tour({
        defaults: {
          classes: theme,
          scrollTo: true
        }
      });
      tour.addStep('off', {
        title: 'Offline mode',
        text: 'Your board is now longer shared!<br>Click this button to reconnect.',
        attachTo: '#study-sync top',
        buttons: [{
          text: 'OK',
          action: tour.next
        }],
      });
      tour.start();
    });
  }
};
