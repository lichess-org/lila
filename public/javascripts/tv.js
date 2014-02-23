$(function() {
  if (!window.EventSource) {
    return;
  }
  var $featured = $('#featured_game');
  var board = function() {
    return $featured.find('> .mini_board');
  };
  parseFen(board());
  var source = new EventSource($('body').data('stream-url'));
  source.addEventListener('message', function(e) {
    var data = JSON.parse(e.data);
    if (data.t == "featured") {
      $('#featured_game').html(data.d.html);
      parseFen(board());
    }
    else if (data.t == "fen") {
      parseFen(board().data("fen", data.d.fen).data("lastmove", data.d.lm));
    }
  }, false);
});
