$(function() {
  $('#board_editor').each(function() {
    var $wrap = $(this);
    var fen = "";

    function setFen(f) {
      fen = f;
      $wrap.find('.fen_string').text(fen);
      $wrap.find('a.fen_link').each(function() {
        $(this).attr('href', $(this).attr('href').replace(/fen=[^#]*#/, "fen=" + fen + '#'));
      });
    }

    var board = new ChessBoard('chessboard', {
      draggable: true,
      dropOffBoard: 'trash',
      position: 'start',
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
  });
});
