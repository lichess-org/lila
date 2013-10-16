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

function posToSquareId(pos) {
  if (pos.length != 2) return;
  var x = "abcdefgh".indexOf(pos[0]),
    y = 8 - parseInt(pos[1], 10);
  return "img_tcol" + x + "trow" + y;
}

function customFunctionOnMove() {
  refreshButtonset();
  $("div.adv_chart").each(function($chart) {
    var chart = $chart.highcharts();
    if (CurrentVar !== 0) {
      _.each(chart.getSelectedPoints(), function(point) {
        point.select(false);
      });
    } else {
      try {
        var index = CurrentPly - 1;
        var point = chart.series[0].data[index];
        point.select();
        var adv = "Advantage: <strong>" + point.y + "</strong>";
        var title = point.name + ' ' + adv;
        chart.setTitle({
          text: title
        });
      } catch (e) {
        console.debug(e);
      }
    }
  });
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
  var fen = CurrentFEN(); $('div.undergame_box a.fen_link').each(function() {
    $(this).attr('href', $(this).attr('href').replace(/fen=.*$/, "fen=" + fen));
  });
  // override normal round fen link
  $("a.view_fen").off('click').on('click', function() {
    alert(fen);
    return false;
  });
  }

  function redrawBoardMarks() {
    $.displayBoardMarks($('#GameBoard'), !$('#GameBoard').hasClass('flip'));
  }

  function refreshButtonset() {
    $("#autoplayButton").addClass("ui-button ui-widget ui-state-default ui-corner-all");
  }
