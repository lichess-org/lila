$(function() {
  var $gameList = $('.game_list.playing');
  if (!$gameList) return;
  var maxGames = 21;
  var maxGamesCheck = function() {
    if ($gameList.children().length >= maxGames) {
      alert('At most ' + maxGames + ' games can be added');
      return false;
    }
    return true;
  };
  $('#submit-gameid').on('click', function() {
    var $gameId = $('#custom-gameid'),
      gameId = $gameId.val();
    if (!gameId || !maxGamesCheck()) return;
    $gameId.val('');
    $.ajax({
      method: 'get',
      url: '/' + gameId + '/mini',
      success: function(result) {
        $gameList.append('<div>' + result + '</div>');
        window.lidraughts.pubsub.emit('content_loaded')();
        $gameId.focus();
      }
    });
  });
  $('#submit-username').on('click', function() {
    var $username = $('#custom-username'),
      username = $username.val();
    if (!username || !maxGamesCheck()) return;
    $username.val('');
    $.ajax({
      method: 'get',
      url: '/games/custom/' + username,
      success: function(result) {
        $gameList.append('<div>' + result + '</div>');
        window.lidraughts.pubsub.emit('content_loaded')();
        $username.focus();
      }
    });
  });
});