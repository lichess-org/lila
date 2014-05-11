// these functions must remain on root namespace

function customFunctionOnPgnGameLoad() {

  $('div.lichess_goodies a.rotate_board').click(function() {
    $('#GameBoard').toggleClass('flip');
    $('#player_links div:first').appendTo($('#player_links'));
    redrawBoardMarks();
    return false;
  });
  redrawBoardMarks();
  $("#autoplayButton").click(refreshButtonset);
  $("#GameBoard td").css('background', 'none');
  $('#ShowPgnText > span').each(function() {
    $(this).text($(this).text().replace(/^([\d\.]+).+$/g, '$1'));
  });
}

function uciToSquareIds(uci) {
  if (uci.length != 4) return [];
  var square = function(pos) {
    var x = "abcdefgh".indexOf(pos[0]),
      y = 8 - parseInt(pos[1], 10);
    return "img_tcol" + x + "trow" + y;
  };
  return [square(uci.slice(0, 2)), square(uci.slice(2))];
}

function customFunctionOnMove() {
  refreshButtonset();
  var $chart = $("#adv_chart");
  if ($chart.length) {
    var chart = $chart.highcharts();
    if (chart) {
      $("#GameBoard img.bestmove").removeClass("bestmove");
      if (CurrentVar !== 0) {
        _.each(chart.getSelectedPoints(), function(point) {
          point.select(false);
        });
      } else {
        if (!isAutoPlayOn) {
          var ids = uciToSquareIds(lichess_best_moves[CurrentPly] || '');
          $.each(ids, function() {
            $("#" + this).addClass("bestmove");
          });
        }
        var index = CurrentPly - 1;
        var point = chart.series[0].data[index];
        if (typeof point != "undefined") {
          point.select();
          var adv = "Advantage: <strong>" + point.y + "</strong>";
          var title = point.name + ' ' + adv;
          chart.setTitle({
            text: title
          });
        }
      }
    }
  }
  var $gameText = $("#GameText");
  var $moveOn = $gameText.find(".moveOn:first");
  var gtHeight = $gameText.height();
  if ($moveOn.length) {
    var height = $moveOn.height();
    var y = $moveOn.position().top;
    if (y < height * 5) {
      $gameText.scrollTop($gameText.scrollTop() + y - height * 5);
    } else if (y > (gtHeight - height * 6)) {
      $gameText.scrollTop($gameText.scrollTop() + y + height * 6 - gtHeight);
    }
  }
  var fen = CurrentFEN();
  $('a.fen_link').each(function() {
    $(this).attr('href', $(this).attr('href').replace(/fen=.*$/, "fen=" + fen));
  });
  $('div.fen_pgn input.fen').val(fen);
  $('a.flip').each(function() {
    $(this).attr('href', $(this).attr('href').replace(/#\d+$/, "#" + CurrentPly));
  });
  if (!$('#GameBoard').hasClass('initialized')) {
    $('#GameBoard').addClass('initialized');
    var ply = parseInt(location.hash.replace(/#/, ''));
    if (ply) GoToMove(ply, 0);
  }
}

// hack: display captures and checks

function CleanMove(move) {
  move = move.replace(/[^a-zA-WYZ0-9#-\+\=]*/g, ''); // patch: remove/add '+' 'x' '=' chars for full chess informant style or pgn style for the game text
  if (move.match(/^[Oo0]/)) {
    move = move.replace(/[o0]/g, 'O').replace(/O(?=O)/g, 'O-');
  }
  move = move.replace(/ep/i, '');
  return move;
}

function redrawBoardMarks() {
  $.displayBoardMarks($('#GameBoard'), !$('#GameBoard').hasClass('flip'));
}

function refreshButtonset() {
  $("#autoplayButton").addClass('button');
  $('#GameButtons a[title]').each(function() {
    $(this)
    .html('<span data-icon="' + $(this).data('icon') + '"></span>')
    .removeAttr('data-icon')
    .attr('data-hint', $(this).attr('title'))
    .removeAttr('title')
    .addClass('hint--bottom');
  });
}
