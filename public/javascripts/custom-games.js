$(function() {
  var maxGames = 21,
    trans = window.lidraughts.trans(window.lidraughts.collectionI18n),
    draughtsResult = window.lidraughts.draughtsResult,
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
  var getFinishedUserIds = function(short) {
    var userIds = [];
    $gameList.children().each(function() {
      var self = $(this),
        result = self.find('.mini-board').data('result');
      if (result && result !== '*') {
        var userId = self.find('.vstext__pl').data('userid');
        if (userId && !userIds.includes(userId)) {
          userIds.push(userId);
        }
      }
    });
    return userIds;
  }
  var getCollectionHref = function(gameIds) {
    var url = window.location.protocol + '//' + window.location.hostname + '/games/collection';
    return gameIds.length ? (url + '?games=' + encodeURIComponent(gameIds.join(','))) : url;
  }
  var updateCollection = function(noHref) {
    var hasFinished = getFinishedUserIds().length ? true : false;
    $('#links-next').toggleClass('visible', hasFinished);
    if (noHref) return;
    var gameIds = getGameIds(),
      $collectionTitle = $('.champion.collection-title');
    if ($collectionTitle.length) {
      $collectionTitle.html(gameIds.length ? trans.plural('nbGames', gameIds.length) : ' - ');
    }
    window.lidraughts.debounce(function() {
      window.history.replaceState(null, '', getCollectionHref(gameIds));
    }, 100)();
  }
  var parseUsername = function($el) {
    var html = $el.html(),
      br = html.indexOf('<br');
    return br !== -1 ? html.slice(0, br) : html;
  }
  var buildEditWrapper = function($board) {
    var removeButton = '<a class="edit-button remove-game" title="' + trans.noarg('removeGame') + '" data-icon="q"></a>',
      result = $board.data('result'), bottomWrapper;
    if (result && result !== '*') {
      var $pl = $board.parent().find('span.vstext__pl'),
        username = $pl.data('userid') && parseUsername($pl);
      if (username) {
        nextGameButton = '<a class="edit-button next-game" title="' + trans('reloadWithCurrentGameOfX', username) + '" data-icon="P"></a>';
        return removeButton + nextGameButton;
      }
    }
    return removeButton;
  }
  var setEditState = function(newState) {
    if (editState && editState == newState) {
      $gameList.find('div.edit-overlay').remove();
    } else {
      editState = newState;
    }
    if (editState) {
      $gameList.children().each(function() {
        var self = $(this),
          $board = self.find('.mini-board'),
          flipButton = '<a class="edit-button flip-game" title="' + trans.noarg('flipBoard') + '" data-icon="B"></a>';
        function bindRemoveButton() {
          self.find('a.remove-game').on('click', (ev) => {
            if (editState) {
              ev.stopPropagation();
              self.remove();
              updateCollection();
            }
          });
        };
        function bindNextGameButton() {
          self.find('a.next-game').on('click', (ev) => {
            ev.stopPropagation();
            fetchNewGames([self.find('span.vstext__pl').data('userid')], self);
          });
        };
        self.append('<div class="edit-overlay">' + flipButton + '<div class="edit-wrapper">' + buildEditWrapper($board) + '</div></div>');
        self.find('a.flip-game').on('click', (ev) => {
          var $gameLink = self.find('a:not(.edit-button)'),
            gameId = getGameId($gameLink, true);
          if (editState && $board.length && gameId) {
            ev.stopPropagation();
            var color = $board.data('color');
            if (color === 'white') color = 'black';
            else color = 'white';
            var $pl = self.find('span.vstext__pl'),
              $op = self.find('span.vstext__op');
            if ($pl.length && $op.length) {
              var plHtml = $pl.html(), plUserId = $pl.data('userid');
              $pl.html($op.html());
              $pl.data('userid', $op.data('userid') || '');
              $op.html(plHtml);
              $op.data('userid', plUserId || '');
            }
            $board.data('color', color);
            $gameLink.attr('href', '/' + gameId + (color === 'black' ? '/' + color : ''));
            $board.data('draughtsground').set({ orientation: color });
            self.find('.edit-wrapper').html(buildEditWrapper($board));
            bindRemoveButton();
            bindNextGameButton();
            updateCollection();
          }
        });
        bindRemoveButton();
        bindNextGameButton();
        self.find('.edit-overlay').on('click', () => setEditState(false));
      });
    } else {
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
  var fetchNewGames = function(userIds, parent) {
    $.ajax({
      method: 'get',
      url: '/games/collection/next?userids=' + encodeURIComponent(userIds.join(',')),
      success: (newGames) => {
        var updated = false;
        (parent || $gameList.children()).each(function() {
          var self = $(this),
            result = self.find('.mini-board').data('result');
          if (result && result !== '*') {
            var userId = self.find('.vstext__pl').data('userid'),
              newGame = userId && newGames[userId]
            if (newGame) {
              self.html(newGame);
              updated = true;
            }
          }
        });
        if (updated) {
          window.lidraughts.pubsub.emit('content_loaded');
          setEditState(editState);
          updateCollection();
        }
      }
    });
  }
  var insertBoard = function(board) {
    $gameList.append('<div>' + board + '</div>');
    window.lidraughts.pubsub.emit('content_loaded');
    setEditState(editState);
    updateCollection();
  };
  var insertResult = function(e) {
    var $board = $gameList.find('.mini-board-' + e.id), outcome;
    switch (e.res) {
      case 'w':
        outcome = draughtsResult ? '2-0' : '1-0';
        break;
      case 'b':
        outcome = draughtsResult ? '0-2' : '0-1';
        break;
      case 'd':
        outcome = draughtsResult ? '1-1' : '½-½';
        break;
    }
    if (outcome && $board.length) {
      var $vstext = $board.parent().find('span.vstext');
      $vstext.find('.vstext__clock').remove();
      $vstext.find('.vstext__pl').after('<span class="vstext__res">' + outcome + '</span>');
      setEditState(editState);
      updateCollection(true);
    }
  };

  window.lidraughts.pubsub.on('game.result', insertResult);

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
  $('#links-next').on('click', () => {
    var userIds = getFinishedUserIds();
    if (userIds.length) fetchNewGames(userIds);
  });

  updateCollection(true);
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