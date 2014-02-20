$(function() {
  $('#board_editor').each(function() {
    var $wrap = $(this);
    var board;
    var $string = $wrap.find('.fen-string');
    var $color = $wrap.find('.color').on('change', onChange);
    var castles = {
      wk: 'K',
      wq: 'Q',
      bk: 'k',
      bq: 'q'
    };
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

    var pieceTheme = 'http://' + document.domain.replace(/^\w+/, 'static') + '/assets/images/piece/{piece}.svg';
    board = new ChessBoard('chessboard', {
      position: toBase($('#chessboard').data('fen')) || 'start',
      draggable: true,
      dropOffBoard: 'trash',
      sparePieces: true,
      showNotation: false,
      pieceTheme: pieceTheme,
      onChange: function() {
        setTimeout(onChange, 100);
      }
    });
    $wrap.find('div.spare-pieces-7492f').addClass('onbg');

    onChange();
    
    var displayMarks = function() {
      $.displayBoardMarks($('#chessboard .board-b72b1'), board.orientation() == "white");
    };

    displayMarks();

    $wrap.on('click', 'a.start', board.start);
    $wrap.on('click', 'a.clear', board.clear);
    $wrap.on('click', 'a.flip', function() {
      board.flip();
      displayMarks();
    });
    $wrap.on('click', 'a.load', function() {
      var fen = prompt('Paste FEN position');
      if (fen) window.location = $(this).data('url').replace('xxx', fen);
      return false;
    });
  });
});
