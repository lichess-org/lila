$(function() {

  // detect study links and convert them to iframes
  $('div.embed_study').each(function() {
    var urlRegex = /\/study\/(?:embed\/)?(\w{8})[#\/](\w{8})/;
    var width = 744;
    $(this).find('a').each(function() {
      var matches = this.href.match(urlRegex);
      if (matches && matches[2]) {
        var $iframe = $('<iframe>').addClass('study').css({
            width: width,
            height: width / 1.618
          })
          .attr('src', '/study/embed/' + matches[1] + '/' + matches[2]);
        $(this).replaceWith($iframe);
        $iframe.on('load', function() {
          if (this.contentDocument.title.indexOf("404") >= 0)
            this.style.height = '100px';
        });
      }
    });
  });
});

lichess.startEmbeddedStudy = function(opts) {
  opts.socketSend = $.noop
  var analyse = LichessAnalyse(opts);

  var board = opts.element.querySelector('.cg-board-wrap');
  var ground = opts.element.querySelector('.lichess_ground');

  var onResize = function() {
    var w = Math.max(document.documentElement.clientWidth, window.innerWidth || 0);
    var h = Math.max(document.documentElement.clientHeight, window.innerHeight || 0);
    var boardSize = h;
    var gr = 1.618;
    if (boardSize > w / gr) boardSize = w / gr;
    var groundSize = Math.min(500, Math.max(120, w - boardSize));
    board.style.width = boardSize + 'px';
    board.style.height = boardSize + 'px';
    ground.style.width = groundSize + 'px';
    ground.style.maxWidth = groundSize + 'px';
    document.body.dispatchEvent(new Event('chessground.resize'));
  };
  onResize();
  window.addEventListener('resize', onResize);
};
