// these functions must remain on root namespace

function customFunctionOnPgnGameLoad() {

  $('div.lichess_goodies a.rotate_board').click(function() {
    $('#GameBoard').toggleClass('flip');
    $('#player_links div:first').appendTo($('#player_links'));
    redrawBoardMarks();
    return false;
  });
  redrawBoardMarks();
  $("#GameButtons table").css('width', '514px').find("input").button();
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
  var turn = Math.round(CurrentPly / 2);
  var $gameText = $("#GameText");
  var $moveOn = $gameText.find(".moveOn:first");
  if ($moveOn.length) {
    var height = $moveOn.height();
    var y = $moveOn.position().top;
    if (y < height * 5) {
      $gameText.scrollTop($gameText.scrollTop() + y - height * 5);
    } else if (y > (512 - height * 6)) {
      $gameText.scrollTop($gameText.scrollTop() + y + height * 6 - 512);
    }
  }
  var fen = CurrentFEN();
  $('div.undergame_box a.fen_link').each(function() {
    $(this).attr('href', $(this).attr('href').replace(/fen=.*$/, "fen=" + fen));
  });
  // override normal round fen link
  $("a.view_fen").off('click').on('click', function() {
    alert(fen);
    return false;
  });
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
  $("#autoplayButton").addClass("ui-button ui-widget ui-state-default ui-corner-all");
}
