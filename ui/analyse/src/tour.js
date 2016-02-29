module.exports = {
  init: function(explorer) {
    if (explorer.authorized && lichess.once('explorer')) {
      if (!explorer.enabled()) setTimeout(function() {
        lichess.hopscotch(function() {
          hopscotch.configure({
            i18n: {
              doneBtn: 'OK, got it'
            }
          }).startTour({
            id: "explorer",
            showPrevButton: true,
            steps: [{
              title: "New feature!",
              content: 'Click this button to enable<br /><strong>lichess opening explorer</strong>.<br />' +
                '<a href="http://lichess.org/blog/Vs0xMTAAAD4We4Ey/opening-explorer">Learn more about it</a>',
              target: "#open_explorer",
              placement: "top",
              xOffset: -10
            }]
          });
        });
      }, 500);
    }
  }
};
