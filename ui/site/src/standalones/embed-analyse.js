function toYouTubeEmbedUrl(url) {
  if (!url) return;
  var m = url.match(
    /(?:https?:\/\/)?(?:www\.)?(?:youtube\.com|youtu\.be)\/(?:watch)?(?:\?v=)?([^"&?/ ]{11})(?:\?|&|)(\S*)/i
  );
  if (!m) return;
  var start = 1;
  m[2].split('&').forEach(function (p) {
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

$(function () {
  var domain = window.location.host;

  var studyRegex = new RegExp(domain + '/study/(?:embed/)?(\\w{8})/(\\w{8})(#\\d+)?\\b');
  var gameRegex = new RegExp(domain + '/(?:embed/)?(\\w{8})(?:(?:/(sente|gote))|\\w{4}|)(#\\d+)?\\b');
  var notGames = ['training', 'analysis', 'insights', 'practice', 'features', 'password', 'streamer'];

  var parseLink = function (a) {
    var yt = toYouTubeEmbedUrl(a.href);
    if (yt)
      return {
        type: 'youtube',
        src: yt,
      };
    var matches = a.href.match(studyRegex);
    if (matches && matches[2] && a.text.match(studyRegex))
      return {
        type: 'study',
        src: '/study/embed/' + matches[1] + '/' + matches[2] + (matches[3] || ''),
      };
    var matches = a.href.match(gameRegex);
    if (matches && matches[1] && !notGames.includes(matches[1]) && a.text.match(gameRegex)) {
      var src = '/embed/' + matches[1];
      if (matches[2]) src += '/' + matches[2]; // orientation
      if (matches[3]) src += matches[3]; // ply hash
      return {
        type: 'game',
        src: src,
      };
    }
  };

  var expandYoutube = function (a) {
    var $iframe = $('<div class="embed"><iframe src="' + a.src + '"></iframe></div>');
    $(a.element).replaceWith($iframe);
    return $iframe;
  };

  var expandYoutubes = function (as, wait) {
    var a = as.shift(),
      wait = Math.min(1500, wait || 100);
    if (a)
      expandYoutube(a)
        .find('iframe')
        .on('load', function () {
          setTimeout(function () {
            expandYoutubes(as, wait + 200);
          }, wait);
        });
  };

  var expand = function (a) {
    var $iframe = $('<iframe>')
      .addClass('analyse ' + a.type)
      .attr('src', a.src);
    $(a.element).replaceWith($('<div class="embed embed--game"></div>').html($iframe));
    return $iframe
      .on('load', function () {
        if (this.contentDocument.title.startsWith('404')) this.style.height = '100px';
      })
      .on('mouseenter', function () {
        $(this).focus();
      });
  };

  var expandStudies = function (as, wait) {
    var a = as.shift(),
      wait = Math.min(1500, wait || 100);
    if (a)
      expand(a).on('load', function () {
        setTimeout(function () {
          expandStudies(as, wait + 200);
        }, wait);
      });
  };

  function groupByParent(as) {
    var groups = [];
    var current = {
      parent: null,
      index: -1,
    };
    as.forEach(function (a) {
      if (a.parent === current.parent) groups[current.index].push(a);
      else {
        current = {
          parent: a.parent,
          index: current.index + 1,
        };
        groups[current.index] = [a];
      }
    });
    return groups;
  }

  var expandGames = function (as) {
    groupByParent(as).forEach(function (group) {
      if (group.length < 3) group.forEach(expand);
      else
        group.forEach(function (a) {
          a.element.title = 'Click to expand';
          a.element.classList.add('text');
          a.element.setAttribute('data-icon', '=');
          a.element.addEventListener('click', function (e) {
            if (e.button === 0) {
              e.preventDefault();
              expand(a);
            }
          });
        });
    });
  };

  var themes = [
    'orange',
    'natural',
    'wood',
    'wood1',
    'kaya1',
    'kaya2',
    'oak',
    'blue',
    'gray',
    'painting1',
    'painting2',
    'kinkaku',
    'space',
    'doubutsu',
    // backend doesn't support it yet
    //    'custom',
  ];

  var configureSrc = function (url) {
    if (url.includes('://')) return url; // youtube, img, etc
    const parsed = new URL(url, window.location.href);
    const theme =
      themes.find(function (theme) {
        return document.body.classList.contains(theme);
      }) ?? 'wood';
    parsed.searchParams.append('theme', theme);
    if (theme === 'custom') {
      const ct = [
        ['--c-board-color', 'boardColor'],
        ['--c-board-url', 'boardImg'],
        ['--c-grid-color', 'gridColor'],
        ['--c-hands-color', 'handsColor'],
        ['--c-hands-url', 'handsImg'],
      ];
      ct.forEach(vals => {
        const cProp = document.body.style.getPropertyValue(vals[0]);
        if (cProp) parsed.searchParams.append(vals[1], cProp);
      });
      const gridWidths = [0, 1, 2, 3],
        gridWidth = gridWidths.find(gw => document.body.classList.contains(`grid-width-${gw}`));
      if (gridWidth !== undefined) parsed.searchParams.append('gridWidth', gridWidth);
    }
    const pieceSet = document.body.dataset.pieceSet;
    if (pieceSet) parsed.searchParams.append('pieceSet', pieceSet);
    parsed.searchParams.append('bg', document.body.dataset.theme);
    return parsed.href;
  };

  var as = $('.embed_analyse a')
    .toArray()
    .map(function (el) {
      var parsed = parseLink(el);
      if (!parsed) return false;
      return {
        element: el,
        parent: el.parentNode,
        type: parsed.type,
        src: configureSrc(parsed.src),
      };
    })
    .filter(function (a) {
      return a;
    });

  expandYoutubes(
    as.filter(function (a) {
      return a.type === 'youtube';
    })
  );

  expandStudies(
    as
      .filter(function (a) {
        return a.type === 'study';
      })
      .map(function (a) {
        a.element.classList.add('embedding_analyse');
        a.element.innerHTML = lishogi.spinnerHtml;
        return a;
      })
  );

  expandGames(
    as.filter(function (a) {
      return a.type === 'game';
    })
  );
});

lishogi.startEmbeddedAnalyse = function (opts) {
  document.body.classList.toggle('supports-max-content', !!window.chrome);
  opts.socketSend = $.noop;
  opts.initialPly = 'url';
  opts.trans = lishogi.trans(opts.i18n);
  LishogiAnalyse.start(opts);
  window.addEventListener('resize', function () {
    lishogi.dispatchEvent(document.body, 'shogiground.resize');
  });
};
