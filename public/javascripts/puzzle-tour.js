lichess = lichess || {};
lichess.startPuzzleTour = function() {
  var baseUrl = $('body').data('asset-url');
  $('head').append($('<link rel="stylesheet" type="text/css" />')
    .attr('href', baseUrl + '/assets/vendor/hopscotch/dist/css/hopscotch.min.css'));
  $.getScript(baseUrl + "/assets/vendor/hopscotch/dist/js/hopscotch.min.js").done(function() {
    var tour = {
      id: "puzzle",
      showPrevButton: true,
      steps: [{
        title: "First puzzle complete!",
        content: "bla, bla, bla.<br>"
        target: "#puzzle .right .box",
        placement: "top"
      }]
    };
    var t = hopscotch.startTour(tour);
  });
};
