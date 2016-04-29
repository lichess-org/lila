module.exports = {
  offline: function(ctrl) {
    if (lichess.once('study-offline')) {
      lichess.hopscotch(function() {
        hopscotch.configure({
          i18n: {
            doneBtn: 'OK, got it'
          }
        }).startTour({
          id: "study-offline",
          showPrevButton: true,
          steps: [{
            title: "Offline mode",
            content: 'Your board is now longer shared!<br>Click this button to reconnect.',
            target: "study-sync",
            placement: "top",
            yOffset: -10
          }]
        });
      });
    }
  }
};
