lichess = lichess || {};
lichess.startInsightTour = function() {
  lichess.hopscotch(function() {
    var tour = {
      id: "insights",
      showPrevButton: true,
      steps: [{
        title: "Welcome to chess insights!",
        content: "Know your strengths and weaknesses!<br>" +
          "Insights let you analyse your playing style, " +
          "using pertinent metrics and dimensions.<br><br>" +
          "It's a powerful tool, let's take some time to see how it works.",
        target: "#insight header h2",
        placement: "bottom"
      }, {
        title: "Insights answer questions",
        content: "Here are a few example questions you can ask. Try clicking them!",
        target: "#insight .panel-tabs a.preset",
        placement: "top",
        yOffset: 10,
        onShow: function() {
          lichess.insight.setPanel('preset');
        }
      }, {
        title: "Answers are graphs",
        content: "Colorful bars represent the answer to the question posed.<br>" +
          "Gray bars represent the size of each data sample, like the number of moves.",
        target: "#insight .chart",
        placement: "left",
        xOffset: 50
      }, {
        title: "The same data, in a table",
        content: "This table provides an alternative way to read the answer.<br>" +
          "Further down the page are a few of the games used to answer the question.",
        target: "#insight table.slist",
        placement: "top"
      }, {
        title: "Ask a question: metric",
        content: "To ask your own questions, start by selecting a metric.<br>" +
          "For instance, let's ask a question about move times.",
        target: "#insight div.ms.metric",
        placement: "left",
        onShow: function() {
          lichess.insight.clearFilters();
          lichess.insight.setPanel('filter');
        }
      }, {
        title: "Ask a question: dimension",
        content: "Now select a dimension to compare move times with.<br>" +
          "For instance, try seeing your move times per variant, or per piece moved.",
        target: "#insight div.ms.dimension",
        placement: "left"
      }, {
        title: "Ask a question: filters",
        content: "Make your question more precise by filtering the results.<br>" +
          "For instance, you can select games where you only played black and castled kingside.",
        target: "#insight .panel-tabs a.filter",
        placement: "top",
        yOffset: 10,
        onShow: function() {
          lichess.insight.clearFilters();
          lichess.insight.setPanel('filter');
        }
      }, {
        title: "Thank you for your time!",
        content: "Now be inventive and find the right questions to ask!<br>" +
          "You can copy the URL at any time to share the results you're seeing.<br><br>" +
          "Oh and one last thing...",
        target: "#insight header h2",
        placement: "bottom"
      }, {
        title: "Share your insights data",
        content: "By default, your data is visible to your lichess friends only.<br>" +
          "You can make it public or private <a href='/account/preferences/privacy'>from your privacy settings</a>.<br><br>" +
          "Have fun :)",
        target: "#insight .info .share",
        placement: "right",
        yOffset: -20
      }]
    };
    hopscotch.startTour(tour);
  })
};
$(function() {
  if (lichess.once('insight-tour')) setTimeout(lichess.startInsightTour, 1000);
});
