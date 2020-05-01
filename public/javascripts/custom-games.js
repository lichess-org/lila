$(function() {
  var maxGames = 21,
    trans = window.lidraughts.trans(window.lidraughts.collectionI18n),
    editState = false,
    $gameList = $('.game_list.playing');
  if (!$gameList) return;

  var checkMaxGames = function() {
    if ($gameList.children().length >= maxGames) {
      alert('At most ' + maxGames + ' games can be added!');
      return false;
    }
    return true;
  };
  var checkExistingGames = function(boardHtml) {
    var href = boardHtml.indexOf('href="');
    if (href === -1) return false;
    href += 6;
    var gameId = boardHtml.slice(href, boardHtml.indexOf('"', href));
    if (gameId[0] === '/') gameId = gameId.slice(1);
    if (gameId.indexOf('/') !== -1) {
      gameId = gameId.slice(0, gameId.indexOf('/'));
    }
    var gameIds = getGameIds(true);
    if (gameIds.indexOf(gameId) !== -1) {
      alert('This game is already in the collection!');
      return false;
    }
    return true;
  }
  var getGameId = function($elm, short) {
    var href = $elm.attr('href');
    if (href && href[0] === '/') {
      href = href.slice(1);
      if (short && href.indexOf('/') !== -1) {
        href = href.slice(0, href.indexOf('/'));
      }
    }
    return href;
  }
  var getGameIds = function(short) {
    var gameIds = [];
    $gameList.children().each(function() {
      var gameId = getGameId($(this).children().first(), short);
      if (gameId) gameIds.push(gameId);
    });
    return gameIds;
  }
  var getCollectionHref = function(gameIds) {
    var url = window.location.protocol + '//' + window.location.hostname + '/games/collection';
    return gameIds.length ? (url + '?games=' + encodeURIComponent(gameIds.join(','))) : url;
  }
  var updateCollection = function() {
    var gameIds = getGameIds(),
      $collectionTitle = $('#collection-title');
    if ($collectionTitle) {
      $collectionTitle.html(gameIds.length ? trans.plural('nbGames', gameIds.length) : ' - ');
    }
    window.lidraughts.fp.debounce(function() {
      window.history.replaceState(null, '', getCollectionHref(gameIds));
    }, 100)();
  }
  var setEditState = function(newState) {
    if (editState === newState) return;
    editState = newState;
    if (editState) {
      $gameList.children().each(function() {
        var self = $(this);
        self.append('<div class="edit-overlay"></div>');
        self.append('<a class="edit-button flip-game" title="' + trans.noarg('flipBoard') + '" data-icon="B"></a>');
        self.find('a.flip-game').on('click', function(el) {
          var $board = self.find('a.mini_board');
          if (editState && $board) {
            var color = $board.attr('data-color'),
              gameId = getGameId($board, true)
            if (color === 'white') color = 'black';
            else color = 'white';
            $board.attr('data-color', color);
            $board.attr('href', '/' + gameId + (color === 'black' ? '/' + color : ''));
            $board.data('draughtsground').set({ orientation: color });
            updateCollection();
          }
        });
        self.append('<a class="edit-button remove-game" title="' + trans.noarg('removeGame') + '" data-icon="q"></a>');
        self.find('a.remove-game').on('click', function(el) {
          if (editState) {
            self.remove();
            updateCollection();
          }
        });
      });
    } else {
      $gameList.find('a.remove-game').remove();
      $gameList.find('a.flip-game').remove();
      $gameList.find('div.edit-overlay').remove();
    }
  };
  var submitGameId = function() {
    var $gameId = $('#custom-gameid'),
      gameId = $gameId.val();
    if (!gameId || !checkMaxGames()) return;
    var urlStart = window.location.hostname + '/',
      urlIndex = gameId.indexOf(urlStart);
    if (urlIndex !== -1) {
      gameId = gameId.slice(urlIndex + urlStart.length);
    }
    if (gameId.length < 8) return;
    $.ajax({
      method: 'get',
      url: '/' + gameId + '/mini',
      success: function(result) {
        if (checkExistingGames(result)) {
          insertBoard(result);
          $gameId.val('');
        }
        $gameId.focus();
      }
    });
  };
  var submitUsername = function() {
    var $username = $('#custom-username'),
      username = $username.val();
    if (!username || !checkMaxGames()) return;
    $.ajax({
      method: 'get',
      url: '/@/' + username + '/recent',
      success: function(result) {
        if (checkExistingGames(result)) {
          insertBoard(result);
          $username.typeahead('val', '');
        }
        $username.focus();
      }
    });
  };
  var insertBoard = function(board) {
    setEditState(false);
    $gameList.append('<div>' + board + '</div>');
    window.lidraughts.pubsub.emit('content_loaded')();
    updateCollection();
  };

  $('#submit-gameid').on('click', submitGameId);
  $('#custom-gameid').on('keypress', function(ev) {
    if (event.keyCode === 13) submitGameId();
  });

  $('#submit-username').on('click', submitUsername);
  $('#custom-username').on('keypress', function(ev) {
    if (event.keyCode === 13) submitUsername();
  });

  $('#links-copy').on('click', function() {
    setEditState(false);
    copyTextToClipboard(getCollectionHref(getGameIds()));
  });
  $('#links-edit').on('click', function() {
    setEditState(!editState);
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