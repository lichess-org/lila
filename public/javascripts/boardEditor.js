$(function() {
  $('#board_editor').each(function() {
    var $wrap = $(this);
    var fen = "";
    var board;
    var $input = $wrap.find('form input').on('change', function() {
      if ($input.val() != fen) board.position($input.val());
    });
    $wrap.find('form').submit(function() {
      return false;
    });

    function setFen(f) {
      fen = f;
      if ($input.val() != fen) $input.val(fen);
      $wrap.find('a.fen_link').each(function() {
        $(this).attr('href', $(this).attr('href').replace(/fen=[^#]*#/, "fen=" + fen + '#'));
      });
    }

    var board = new ChessBoard('chessboard', {
      position: $('#chessboard').data('fen') || 'start',
      draggable: true,
      dropOffBoard: 'trash',
      sparePieces: true,
      pieceTheme: '/assets/vendor/chessboard/img/chesspieces/wikipedia/{piece}.png',
      onChange: function(oldPos, newPos) {
        setFen(ChessBoard.objToFen(newPos));
      }
    });
    setFen(board.fen());

    $wrap.find('a.start').on('click', board.start);
    $wrap.find('a.clear').on('click', board.clear);
    $wrap.find('a.flip').on('click', board.flip);
    $wrap.find('a.save').on('click', function() {
      alert('Permalink: ' + $(this).data('domain') + '/editor/' + fen);
      return false;
    });
  });
});
