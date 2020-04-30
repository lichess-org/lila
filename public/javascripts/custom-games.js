$(function() {
  var maxGames = 21,
    trans = window.lidraughts.trans(window.lidraughts.customI18n),
    removingState = false,
    $gameList = $('.game_list.playing');
  if (!$gameList) return;

  var maxGamesCheck = function() {
    if ($gameList.children().length >= maxGames) {
      alert('At most ' + maxGames + ' games can be added');
      return false;
    }
    return true;
  };
  var getGameIds = function() {
    var gameIds = [];
    $gameList.children().each(function() {
      var href = $(this).children().first().attr('href');
      if (href && href[0] === '/') {
        gameIds.push(href.slice(1));
      }
    });
    return gameIds;
  }
  var getCollectionHref = function(gameIds) {
    var url = window.location.protocol + '//' + window.location.hostname + '/games/custom';
    return gameIds.length ? (url + '?games=' + encodeURIComponent(gameIds.join(','))) : url;
  }
  var updateCollection = function() {
    var gameIds = getGameIds(),
      $collectionDesc = $('#collection-desc');
    if ($collectionDesc) {
      $collectionDesc.html(gameIds.length ? trans.plural('nbGames', gameIds.length) : ' - ');
    }
    window.lidraughts.fp.debounce(function() {
      window.history.replaceState(null, '', getCollectionHref(gameIds));
    }, 100)();
  }
  var setRemovingState = function(newState) {
    if (removingState === newState) return;
    removingState = newState;
    if (removingState) {
      $gameList.children().each(function() {
        var self = $(this);
        self.append('<a class="remove-game" title="Remove game" data-icon="q"></a>');
        self.find('a.remove-game').on('click', function(el) {
          if (removingState) {
            self.remove();
            updateCollection();
          }
        });
      });
    } else {
      $gameList.find('a.remove-game').remove();
    }
  };
  var insertBoard = function(board) {
    setRemovingState(false);
    $gameList.append('<div>' + board + '</div>');
    window.lidraughts.pubsub.emit('content_loaded')();
    updateCollection();
  };

  $('#links-copy').on('click', function() {
    copyTextToClipboard(getCollectionHref(getGameIds()));
  });
  $('#links-remove').on('click', function() {
    setRemovingState(!removingState);
  });
  $('#submit-gameid').on('click', function() {
    var $gameId = $('#custom-gameid'),
      gameId = $gameId.val();
    if (!gameId || gameId.length < 8 || !maxGamesCheck()) return;
    $.ajax({
      method: 'get',
      url: '/' + gameId + '/mini',
      success: function(result) {
        insertBoard(result);
        $gameId.val('').focus();
      }
    });
  });
  $('#submit-username').on('click', function() {
    var $username = $('#custom-username'),
      username = $username.val();
    if (!username || !maxGamesCheck()) return;
    $.ajax({
      method: 'get',
      url: '/games/custom/' + username,
      success: function(result) {
        insertBoard(result);
        $username.typeahead('val', '').focus();
      }
    });
  });
});

function copyTextToClipboard(text) {
  if (navigator.clipboard) {
    navigator.clipboard.writeText(text);
    return;
  }
  var textArea = document.createElement('textarea');
  textArea.value = text;
  textArea.style.top = '0';
  textArea.style.left = '0';
  textArea.style.position = 'fixed';
  document.body.appendChild(textArea);
  textArea.focus();
  textArea.select();
  document.execCommand('copy');
  document.body.removeChild(textArea);
}