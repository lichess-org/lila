$(function() {

  function parseFen($elem) {
    if (!$elem || !$elem.jquery) {
      $elem = $('.parse_fen');
    }
    $elem.each(function() {
      var $this = $(this);
      var color = $this.data('color') || "white";
      var withKeys = $this.hasClass('with_keys');
      var letters = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];
      var fen = $this.data('fen').replace(/\//g, '');
      var x, y, html = '', scolor, pcolor, pclass, c, d, increment;
      var pclasses = {'p':'pawn', 'r':'rook', 'n':'knight', 'b':'bishop', 'q':'queen', 'k':'king'};
      var pregex = /(p|r|n|b|q|k)/;

      if ('white' == color) {
        var x = 8, y = 1;
        var increment = function() { y++; if(y > 8) { y = 1; x--; } };
      } else {
        var x = 1, y = 8;
        var increment = function() { y--; if(y < 1) { y = 8; x++; } };
      }
      function openSquare(x, y) {
        var scolor = (x+y)%2 ? 'white' : 'black';
        var html = '<div class="lmcs '+scolor+'" style="top:'+(24*(8-x))+'px;left:'+(24*(y-1))+'px;"';
        if (withKeys) {
          var key = 'white' == color 
            ? letters[y - 1] + x
            : letters[8 - y] + (9 - x);
          html += ' data-key="' + key + '"';
        }
        return html + '>';
      }
      function closeSquare() {
        return '</div>';
      }

      for(var fenIndex in fen) {
        c = fen[fenIndex];
        html += openSquare(x, y);
        if (!isNaN(c)) { // it is numeric
          html += closeSquare();
          increment();
          for (d=1; d<c; d++) {
            html += openSquare(x, y) + closeSquare();
            increment();
          }
        } else {
          pcolor = pregex.test(c) ? 'black' : 'white';
          pclass = pclasses[c.toLowerCase()];
          html += '<div class="lcmp '+pclass+' '+pcolor+'"></div>';
          html += closeSquare();
          increment();
        }
      }

      $this.html(html).removeClass('parse_fen');
      // attempt to free memory
      html = pclasses = increment = pregex = fen = $this = 0;
    });
  }
  parseFen();
  $('body').on('lichess.content_loaded', parseFen);

  function registerLiveGames() {
    var ids = [];
    $('a.mini_board.live').each(function() {
      ids.push($(this).data("live"));
    }).removeClass("live");
    if (ids.length > 0) {
      lichess.socket.send("liveGames", ids.join(" "));
    }
  }
  $('body').on('lichess.content_loaded', registerLiveGames);
  $('body').on('socket.open', registerLiveGames);

  lichess.socketDefaults.events.fen = function(e) {
    $('a.live_' + e.id).each(function() {
      var $this = $(this);
      parseFen($this.data("fen", e.fen).addClass("highlight"));
      setTimeout(function() {
        $this.removeClass("highlight");
      }, 200);
    });
  };

  $('div.checkmateCaptcha').each(function() {
    var $captcha = $(this);
    var $input = $captcha.find('input');
    var i1, i2;
    $captcha.find('div.lmcs').click(function() {
      var key = $(this).data('key');
      i1 = $input.val();
      i2 = i1.length > 3 ? key : i1 + " " + key;
      $input.val(i2);
    });
  });
});
