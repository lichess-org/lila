function parseFen($elem) {
  if (!$elem || !$elem.jquery) {
    $elem = $('.parse_fen');
  }
  $elem.each(function() {
    var $this = $(this).removeClass('parse_fen');
    var lm = $this.data('lastmove');
    var lastMove = lm ? [lm[0] + lm[1], lm[2] + lm[3]] : [];

    Chessground($this[0], {
      fen: $this.data('fen'),
      lastMove: lm ? [lm[0] + lm[1], lm[2] + lm[3]] : [],
      orientation: $this.data('color') || "white"
    });
  });
}
