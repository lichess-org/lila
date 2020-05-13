$(function() {
  var maxGames = 21,
    trans = window.lidraughts.trans(window.lidraughts.collectionI18n),
    editState = false,
    $gameList = $('.page-menu__content.now-playing');
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
      $collectionTitle = $('.champion.collection-title');
    if ($collectionTitle) {
      $collectionTitle.html(gameIds.length ? trans.plural('nbGames', gameIds.length) : ' - ');
    }
    window.lidraughts.debounce(function() {
      window.history.replaceState(null, '', getCollectionHref(gameIds));
    }, 100)();
  }
  var setEditState = function(newState) {
    if (editState === newState) return;
    editState = newState;
    if (editState) {
      $gameList.children().each(function() {
        var self = $(this),
          flipButton = '<a class="edit-button flip-game" title="' + trans.noarg('flipBoard') + '" data-icon="B"></a>',
          removeButton = '<a class="edit-button remove-game" title="' + trans.noarg('removeGame') + '" data-icon="q"></a>';
        self.append('<div class="edit-overlay">' + flipButton + removeButton + '</div>');
        self.find('a.flip-game').on('click', (ev) => {
          var $board = self.find('.mini-board'),
            $gameLink = self.find('a:not(.edit-button)'),
            gameId = getGameId($gameLink, true);
          if (editState && $board && gameId) {
            ev.stopPropagation();
            var color = $board.data('color');
            if (color === 'white') color = 'black';
            else color = 'white';
            var $pl = $board.parent().find('span.vstext__pl'),
              $op = $board.parent().find('span.vstext__op');
            if ($pl.length && $op.length) {
              var plHtml = $pl.html();
              $pl.html($op.html());
              $op.html(plHtml);
            }
            $board.data('color', color);
            $gameLink.attr('href', '/' + gameId + (color === 'black' ? '/' + color : ''));
            $board.data('draughtsground').set({ orientation: color });
            updateCollection();
          }
        });
        self.find('a.remove-game').on('click', (ev) => {
          if (editState) {
            ev.stopPropagation();
            self.remove();
            updateCollection();
          }
        });
        self.find('.edit-overlay').on('click', () => setEditState(false));
      });
    } else {
      $gameList.find('a.remove-game').remove();
      $gameList.find('a.flip-game').remove();
      $gameList.find('div.edit-overlay').remove();
    }
  };
  var submitGameId = function() {
    var $gameId = $('#collection-gameid'),
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
      success: (result) => {
        if (checkExistingGames(result)) {
          insertBoard(result);
          $gameId.val('');
        }
        $gameId.focus();
      }
    });
  };
  var submitUsername = function() {
    var $username = $('#collection-recent'),
      username = $username.val();
    if (!username || !checkMaxGames()) return;
    $.ajax({
      method: 'get',
      url: '/@/' + username + '/recent',
      success: (result) => {
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
    window.lidraughts.pubsub.emit('content_loaded');
    updateCollection();
  };

  $('#submit-gameid').on('click', submitGameId);
  $('#collection-gameid').on('keypress', (ev) => {
    if (ev.keyCode === 13) submitGameId();
  });

  $('#submit-username').on('click', submitUsername);
  $('#collection-recent').on('keypress', (ev) => {
    if (ev.keyCode === 13) submitUsername();
  });

  $('#links-copy').on('click', () => {
    setEditState(false);
    copyTextToClipboard(getCollectionHref(getGameIds()));
  });
  $('#links-edit').on('click', () => {
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