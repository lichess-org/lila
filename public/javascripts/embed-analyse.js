$(function() {

  var urlRegex = /\/study\/(?:embed\/)?(\w{8})[#\/](\w{8})/;
  var wait = 100;

  var expand = function(as) {
    var a = as.shift();
    if (!a) return;
    var matches = a.href.match(urlRegex);
    if (matches && matches[2]) {
      var $iframe = $('<iframe>').addClass('analyse')
        .attr('src', '/study/embed/' + matches[1] + '/' + matches[2]);
      $(a).replaceWith($iframe);
      $iframe.on('load', function() {
        if (this.contentDocument.title.indexOf("404") >= 0)
          this.style.height = '100px';
      }).on('mouseenter', function() {
        $(this).focus();
      });
      wait = Math.min(2000, wait *= 2);
      setTimeout(function() {
        expand(as);
      }, wait);
    } else expand(as);
  };

  // detect study links and convert them to iframes
  expand($('div.embed_analyse a').filter(function() {
    return !!this.href.match(urlRegex);
  }).addClass('embedding_analyse').html(lichess.spinnerHtml).toArray());
});

lichess.startEmbeddedAnalyse = function(opts) {
  opts.socketSend = $.noop
  var analyse = LichessAnalyse(opts);

  var board = opts.element.querySelector('.cg-board-wrap');
  var ground = opts.element.querySelector('.lichess_ground');

  var onResize = function() {
    var w = Math.max(document.documentElement.clientWidth, window.innerWidth || 0);
    var h = Math.max(document.documentElement.clientHeight, window.innerHeight || 0);
    var boardSize = h - 26;
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
