function parseFen($elem) {
  if (!$elem || !$elem.jquery) {
    $elem = $('.parse_fen');
  }
  $elem.each(function() {
    var $this = $(this);
    var color = $this.data('color') || "white";
    var withKeys = $this.hasClass('with_keys');
    var letters = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];
    var fen = $this.data('fen').split(' ')[0].replace(/\//g, '');
    var lm = $this.data('lastmove');
    var lastMove = lm ? [lm[0] + lm[1], lm[2] + lm[3]] : [];
    var x, y, html = '',
      pcolor, pclass, c, d, increment;
    var pclasses = {
      'p': 'pawn',
      'r': 'rook',
      'n': 'knight',
      'b': 'bishop',
      'q': 'queen',
      'k': 'king'
    };
    var pregex = /(p|r|n|b|q|k)/;

    if ('white' == color) {
      x = 8;
      y = 1;
      increment = function() {
        y++;
        if (y > 8) {
          y = 1;
          x--;
        }
      };
    } else {
      x = 1;
      y = 8;
      increment = function() {
        y--;
        if (y < 1) {
          y = 8;
          x++;
        }
      };
    }

    function openSquare(x, y) {
      var key = 'white' == color ? letters[y - 1] + x : letters[8 - y] + (9 - x);
      var html = '<div' + ((lastMove.indexOf(key) !== -1) ? ' class="moved" ' : '') + ' style="top:' + (28 * (8 - x)) + 'px;left:' + (28 * (y - 1)) + 'px;"';
      if (withKeys) {
        html += ' data-key="' + key + '"';
      }
      return html + '>';
    }

    for (var fenIndex in fen) {
      c = fen[fenIndex];
      html += openSquare(x, y);
      if (!isNaN(c)) { // it is numeric
        html += '</div>';
        increment();
        for (d = 1; d < c; d++) {
          html += openSquare(x, y) + '</div>';
          increment();
        }
      } else {
        pcolor = pregex.test(c) ? 'black' : 'white';
        pclass = pclasses[c.toLowerCase()];
        html += '<div class="lcmp ' + pclass + ' ' + pcolor + '"></div>';
        html += '</div>';
        increment();
      }
    }

    $this.html(html).removeClass('parse_fen');
    // attempt to free memory
    html = pclasses = increment = pregex = fen = $this = 0;
  });
}
