$(function() {
  SetImagePath("/assets/vendor/pgn4web/lichess/64"); // use "" path if images are in the same folder as this javascript file
  SetImageType("png");
  var $game = $("#GameBoard");
  var $chat = $("div.lichess_chat").chat();
  var $watchers = $("div.watchers").watchers();

  lichess.socket = new $.websocket(
    lichess.socketUrl + $game.data("socket-url"),
    parseInt($game.data("version")),
    $.extend(true, lichess.socketDefaults, {
      options: {
        name: "analyse",
        ignoreUnknownMessages: true
      },
      events: {
        message: function(event) {
          $chat.chat("append", event);
        },
        crowd: function(event) {
          $watchers.watchers("set", event.watchers);
        }
      }
    }));
});

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
  $("#GameButtons table").css('width', '514px').buttonset();
  $("#autoplayButton").click(refreshButtonset);
}

function customFunctionOnMove() {
  $('#GameLastComment').toggle($('#GameLastComment > .comment').text().length > 0);
  refreshButtonset();
  var chart = $("div.adv_chart").data("chart");
  if (chart) {
    var index = CurrentPly - 1;
    chart.setSelection([{ row: index, column: 1}]);
  }
}

function redrawBoardMarks() {
  $.displayBoardMarks($('#GameBoard'), ! $('#GameBoard').hasClass('flip'));
}

function refreshButtonset() {
  $("#autoplayButton").addClass("ui-button ui-widget ui-state-default");
}
