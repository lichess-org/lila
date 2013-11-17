$(function() {
  $('#board_editor').each(function() {
    var $wrap = $(this);
    var board;
    var $string = $wrap.find('.fen-string');
    var $color = $wrap.find('.color').on('change', onChange);
    var castles = {wk:'K',wq:'Q',bk:'k',bq:'q'};
    $wrap.find('.castling input').on('change', onChange);

    function getRich() {
      return toRich(board.fen());
    }
    function toRich(fen) {
      var castling = _.map(castles, function(symbol, key) {
        return $('#castling-' + key).prop('checked') ? symbol : '';
      }).join('') || '-';
      return fen + ' ' + $color.val() + ' ' + castling;
    }
    function toBase(fen) {
      return fen.split(' ')[0];
    }

    function onChange() {
      var rich = getRich();
      $string.text(rich);
      $wrap.find('a.fen_link').each(function() {
        $(this).attr('href', $(this).attr('href').replace(/fen=[^#]*#/, "fen=" + rich + '#'));
      });
      $wrap.find('a.permalink').each(function() {
        $(this)
          .attr('href', $(this).data('url').replace('xxx', rich))
          .text($(this).data('url').replace('xxx', encodeURIComponent(rich)));
      });
    }

    board = new ChessBoard('chessboard', {
      position: toBase($('#chessboard').data('fen')) || 'start',
      draggable: true,
      dropOffBoard: 'trash',
      sparePieces: true,
      pieceTheme: '/assets/vendor/chessboard/img/chesspieces/wikipedia/{piece}.png',
      onChange: function() {
        setTimeout(onChange, 100);
      }
    });
    onChange();

    $wrap.find('a.start').on('click', board.start);
    $wrap.find('a.clear').on('click', board.clear);
    $wrap.find('a.flip').on('click', board.flip);
    $wrap.find('a.load').on('click', function() {
      var fen = prompt('Paste FEN position');
      window.location = $(this).data('url').replace('xxx', fen);
      return false;
    });
 });
});
