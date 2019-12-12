function toYouTubeEmbedUrl(url) {
  if (!url) return;
  var m = url.match(/(?:https?:\/\/)?(?:www\.)?(?:youtube\.com|youtu\.be)\/(?:watch)?(?:\?v=)?([^"&?/ ]{11})(?:\?|&|)(\S*)/i);
  if (!m) return;
  var start = 1;
  m[2].split('&').forEach(function(p) {
    var s = p.split('=');
    if (s[0] === 't' || s[0] === 'start') {
      if (s[1].match(/^\d+$/)) start = parseInt(s[1]);
      else {
        var n = s[1].match(/(?:(\d+)h)?(?:(\d+)m)?(?:(\d+)s)?/);
        start = (parseInt(n[1]) || 0) * 3600 + (parseInt(n[2]) || 0) * 60 + (parseInt(n[3]) || 0);
      }
    }
  });
  var params = 'modestbranding=1&rel=0&controls=2&iv_load_policy=3&start=' + start;
  return 'https://www.youtube.com/embed/' + m[1] + '?' + params;
}

$(function() {

  var domain = window.location.host;

  var studyRegex = new RegExp(domain + '/study/(?:embed/)?(\\w{8})/(\\w{8})(#\\d+)?\\b');
  var gameRegex = new RegExp(domain + '/(?:embed/)?(\\w{8})(?:(?:/(white|black))|\\w{4}|)(#\\d+)?\\b');
  var notGames = ['training', 'analysis', 'insights', 'practice', 'features', 'password', 'streamer'];

  var parseLink = function(a) {
    var yt = toYouTubeEmbedUrl(a.href);
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
    if (matches && matches[1] && !notGames.includes(matches[1]) && a.text.match(gameRegex)) {
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
    var $iframe = $('<div class="embed"><iframe src="' + a.src + '"></iframe></div>');
    $(a.element).replaceWith($iframe);
    return $iframe;
  };

  var expandYoutubes = function(as, wait) {
    var a = as.shift(),
      wait = Math.min(1500, wait || 100);
    if (a) expandYoutube(a).find('iframe').on('load', function() {
      setTimeout(function() {
        expandYoutubes(as, wait + 200);
      }, wait);
    });
  };

  var expand = function(a) {
    var $iframe = $('<iframe>').addClass('analyse ' + a.type).attr('src', a.src);
    $(a.element).replaceWith($('<div class="embed"></div>').html($iframe));
    return $iframe.on('load', function() {
      if (this.contentDocument.title.startsWith("404")) this.style.height = '100px';
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
  }

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

  var themes = ['blue', 'blue2', 'blue3', 'canvas', 'wood', 'wood2', 'wood3', 'maple', 'green', 'marble', 'brown', 'leather', 'grey', 'metal', 'olive', 'purple'];

  var configureSrc = function(url) {
    if (url.includes('://')) return url; // youtube, img, etc
    var parsed = new URL(url, window.location.href);
    parsed.searchParams.append('theme', themes.find(function (theme) {
      return document.body.classList.contains(theme);
    }));
    parsed.searchParams.append('bg', document.body.getAttribute('data-theme'));
    return parsed.href;
  }

  var as = $('.embed_analyse a').toArray().map(function(el) {
    var parsed = parseLink(el);
    if (!parsed) return false;
    return {
      element: el,
      parent: el.parentNode,
      type: parsed.type,
      src: configureSrc(parsed.src)
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
  document.body.classList.toggle('supports-max-content', !!window.chrome);
  opts.socketSend = $.noop
  opts.initialPly = 'url';
  opts.trans = lichess.trans(opts.i18n);
  LichessAnalyse.start(opts);
  window.addEventListener('resize', function() {
    lichess.dispatchEvent(document.body, 'chessground.resize');
  });
}
