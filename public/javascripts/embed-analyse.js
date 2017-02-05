$(function() {

  var studyRegex = /lichess\.org\/study\/(?:embed\/)?(\w{8})\/(\w{8})(#\d+)?\b/;
  var gameRegex = /lichess\.org\/(?:embed\/)?(\w{8})(?:(?:\/(white|black))|\w{4}|)(#\d+)?\b/;
  var notGames = ['training', 'analysis', 'insights', 'practice'];

  var parseLink = function(a) {
    var yt = lichess.toYouTubeEmbedUrl(a.href);
    if (yt) return {
      type: 'youtube',
      src: yt
    };
    var matches = a.href.match(studyRegex);
    if (matches && matches[2] && a.text.match(studyRegex)) return {
      type: 'study',
      src: '/study/embed/' + matches[1] + '/' + matches[2] + (matches[3] || '')
    };
    var matches = a.href.match(gameRegex);
    if (matches && matches[1] && notGames.indexOf(matches[1]) === -1 && a.text.match(gameRegex)) {
      var src = '/embed/' + matches[1];
      if (matches[2]) src += '/' + matches[2]; // orientation
      if (matches[3]) src += matches[3]; // ply hash
      return {
        type: 'game',
        src: src
      };
    }
  };

  var expandYoutube = function(a) {
    var $iframe = $('<iframe>').addClass('video ' + a.type).attr('src', a.src);
    $(a.element).replaceWith($iframe);
    return $iframe;
  };

  var expandYoutubes = function(as, wait) {
    var a = as.shift(),
      wait = Math.min(1500, wait || 100);
    if (a) expandYoutube(a).on('load', function() {
      setTimeout(function() {
        expandYoutubes(as, wait + 200);
      }, wait);
    });
  };

  var expand = function(a) {
    var $iframe = $('<iframe>').addClass('analyse ' + a.type).attr('src', a.src);
    $(a.element).replaceWith($iframe);
    return $iframe.on('load', function() {
      if (this.contentDocument.title.indexOf("404") >= 0) this.style.height = '100px';
    }).on('mouseenter', function() {
      $(this).focus();
    });
  };

  var expandStudies = function(as, wait) {
    var a = as.shift(),
      wait = Math.min(1500, wait || 100);
    if (a) expand(a).on('load', function() {
      setTimeout(function() {
        expandStudies(as, wait + 200);
      }, wait);
    });
  };

  function groupByParent(as) {
    var groups = [];
    var current = {
      parent: null,
      index: -1
    };
    as.forEach(function(a) {
      if (a.parent === current.parent) groups[current.index].push(a);
      else {
        current = {
          parent: a.parent,
          index: current.index + 1
        };
        groups[current.index] = [a];
      }
    });
    return groups;
  };

  var expandGames = function(as) {
    groupByParent(as).forEach(function(group) {
      if (group.length < 3) group.forEach(expand);
      else group.forEach(function(a) {
        a.element.title = 'Click to expand';
        a.element.classList.add('text');
        a.element.setAttribute('data-icon', '=');
        a.element.addEventListener('click', function(e) {
          if (e.button === 0) {
            e.preventDefault();
            expand(a);
          }
        });
      });
    });
  };

  var as = $('div.embed_analyse a').toArray().map(function(el) {
    var parsed = parseLink(el);
    if (!parsed) return false;
    return {
      element: el,
      parent: el.parentNode,
      type: parsed.type,
      src: parsed.src
    };
  }).filter(function(a) {
    return a;
  });

  expandYoutubes(as.filter(function(a) {
    return a.type === 'youtube'
  }));

  expandStudies(as.filter(function(a) {
    return a.type === 'study'
  }).map(function(a) {
    a.element.classList.add('embedding_analyse');
    a.element.innerHTML = lichess.spinnerHtml;
    return a;
  }));

  expandGames(as.filter(function(a) {
    return a.type === 'game'
  }));
});

lichess.startEmbeddedAnalyse = function(opts) {
  opts.socketSend = $.noop
  opts.initialPly = 'url';
  LichessAnalyse.mithril(opts);

  var board = opts.element.querySelector('.cg-board-wrap');
  var ground = opts.element.querySelector('.lichess_ground');

  var onResize = function() {
    var w = Math.max(document.documentElement.clientWidth, window.innerWidth || 0);
    var h = Math.max(document.documentElement.clientHeight, window.innerHeight || 0);
    var boardSize = h - 26;
    var gr = 1.618;
    if (boardSize > w / gr) boardSize = w / gr;
    var groundSize = Math.min(500, Math.max(120, w - boardSize));
    board.style.width = boardSize + 'px';
    board.style.height = boardSize + 'px';
    ground.style.width = groundSize + 'px';
    ground.style.maxWidth = groundSize + 'px';
    document.body.dispatchEvent(new Event('chessground.resize'));
  };
  onResize();
  window.addEventListener('resize', onResize);
};
