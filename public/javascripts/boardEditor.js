$(function() {
  $('#board_editor').each(function() {
    var $wrap = $(this);
    var board;
    var $string = $wrap.find('input.fen-string');
    var $color = $wrap.find('.color').on('change', onChange);
    var castles = {
      wk: 'K',
      wq: 'Q',
      bk: 'k',
      bq: 'q'
    };
    $wrap.find('.castling input').on('change', onChange);
    var assetUrl = $wrap.data('asset-url');

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

    function encodeURIFen(fen) {
      return encodeURIComponent(fen.replace(/ /g, '_')).replace(/%2F/g, '/');
    }

    function onChange() {
      var rich = getRich();
      $string.val(rich);
      $wrap.find('a.fen_link').each(function() {
        $(this).attr('href', $(this).attr('href').replace(/fen=[^#]*#/, "fen=" + rich + '#'));
      });
      $wrap.find('input.permalink').each(function() {
        $(this).val($(this).data('url').replace('xxx', encodeURIFen(rich)));
      });
    }

    var pieceTheme = assetUrl + '/assets/piece/' + $('body').data('piece-set') + '/{piece}.svg';
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
