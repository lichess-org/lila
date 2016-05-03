function standardTour(id, title, content) {
  lichess.hopscotch(function() {
    hopscotch.configure({
      i18n: {
        doneBtn: 'OK, got it'
      }
    }).startTour({
      id: 'study-' + id,
      showPrevButton: true,
      steps: [{
        title: title,
        content: content,
        target: "study-sync",
        placement: "top",
        yOffset: -10
      }]
    });
  });
}

module.exports = {
  offline: function(ctrl) {
    if (lichess.once('study-offline'))
      standardTour('offline', "Offline mode", 'Your board is now longer shared!<br>Click this button to reconnect.');
  },
  becomeContributor: function(ctrl) {
    standardTour('contributor', "You are now a contributor", 'The changes you make are now visible by other members of this study.');
  },
  becomeViewer: function(ctrl) {
    standardTour('viewer', "You are now a viewer", 'The changes you make are no longer visible by other members of this study.');
  }
};
