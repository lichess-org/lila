// these functions must remain on root namespace 
function customFunctionOnPgnGameLoad() {
  var $text = $('#ShowPgnText');
  var html = '<table><tbody><tr>';
  $text.find('span.move, span.variation').remove();
  $text.find('>a').each(function(it) {
    if (0 == it%2) {
      html += '</tr><tr><th>' + (it/2+1) + '</th>';
    }
    html += '<td>' + this.outerHTML + '</td>';
  });
  html += '</tr></tbody></table>';
  $text.html(html).find('tr:empty').remove();

  $('div.lichess_goodies a.rotate_board').click(function() {
    $('#GameBoard').toggleClass('flip');
    $('#player_links div:first').appendTo($('#player_links'));
    redrawBoardMarks();
    return false;
  });
  redrawBoardMarks();
  $("#GameButtons table").css('width', '514px').find("input").button();
  $("#autoplayButton").click(refreshButtonset);
}

function posToSquareId(pos) {
  if (pos.length != 2) return;
  var x = "abcdefgh".indexOf(pos[0]), y = 8 - parseInt(pos[1]);
  return "img_tcol" + x + "trow" + y;
}

function customFunctionOnMove() {
  var $comment = $('#GameLastComment');
  var moves = $comment.find('.commentMove').map(function() { return $(this).text(); });
  var ids = $.map(moves, posToSquareId);
  $("#GameBoard img.bestmove").removeClass("bestmove");
  $.each(ids, function() {
    if (this) $("#" + this).addClass("bestmove");
  });
  refreshButtonset();
  var $chart = $("div.adv_chart");
  var chart = $chart.data("chart");
  if (chart) {
    try {
      var index = CurrentPly - 1;
      chart.setSelection([{ row: index, column: 1}]);
      var rows = $chart.data('rows');
      $comment.prepend($("<p>").html("White advantage: <strong>" + rows[index][1] + "</strong>"));
    } catch (e) {}
  }
  var turn = Math.round(CurrentPly / 2);
  var $gameText = $("#GameText");
  var $th = $gameText.find("th:eq(" + (turn - 1) + ")");
  if ($th.length) {
    var height = $th.height();
    var y = $th.position().top;
    if (y < height * 3) {
      $gameText.scrollTop($gameText.scrollTop() + y - height * 3);
    } else if (y > (512 - height * 4)) {
      $gameText.scrollTop($gameText.scrollTop() + y + height * 4 - 512);
    }
  }
  $('#CurrentFen').text(CurrentFEN());
}

function redrawBoardMarks() {
  $.displayBoardMarks($('#GameBoard'), !$('#GameBoard').hasClass('flip'));
}

function refreshButtonset() {
  $("#autoplayButton").addClass("ui-button ui-widget ui-state-default ui-corner-all");
}
