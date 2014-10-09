function parseFen($elem) {
  if (!$elem || !$elem.jquery) {
    $elem = $('.parse_fen');
  }
  $elem.each(function() {
    var $this = $(this).removeClass('parse_fen');
    var lm = $this.data('lastmove');
    var lastMove = lm ? [lm[0] + lm[1], lm[2] + lm[3]] : [];
    var color = $this.data('color');
    var ground = $this.data('chessground');
    var config = {
      fen: $this.data('fen'),
      lastMove: lm ? [lm[0] + lm[1], lm[2] + lm[3]] : [],
    };
    if (color) config.orientation = color;
    if (ground) ground.set(config);
    else $this.data('chessground', Chessground($this[0], config));
  });
}
