// ==ClosureCompiler==
// @compilation_level ADVANCED_OPTIMIZATIONS
// ==/ClosureCompiler==

lichess.challengeApp = (function() {
  var instance, booted;
  var $toggle = $('#challenge_notifications_tag');
  $toggle.one('mouseover click', function() {
    if (!booted) load();
  });
  var load = function(data) {
    booted = true;
    var isDev = $('body').data('dev');
    var element = document.getElementById('challenge_app');
    lichess.loadCss('/assets/stylesheets/challengeApp.css');
    lichess.loadScript("/assets/compiled/lichess.challenge" + (isDev ? '' : '.min') + '.js').done(function() {
      instance = LichessChallenge(element, {
        data: data,
        show: function() {
          if (!$(element).is(':visible')) $toggle.click();
        },
        setCount: function(nb) {
          $toggle.attr('data-count', nb);
        },
        pulse: function() {
          $toggle.addClass('pulse');
        }
      });
    });
  };
  return {
    update: function(data) {
      if (!instance) load(data);
      else instance.update(data);
    },
    open: function() {
      $toggle.click();
    }
  };
})();



lichess.topMenuIntent = function() {
  $('#topmenu.hover').removeClass('hover').hoverIntent(function() {
    $(this).toggleClass('hover');
  });
};

lichess.notifyApp = (function() {
  var instance;
  var $element = $('#notify_app');
  var $toggle = $('#site_notifications_tag');
  var isVisible = function() {
    return $element.is(':visible');
  };
  $toggle.one('mouseover click', function() {
    if (!instance) load();
  }).click(function() {
    setTimeout(function() {
      if (instance && isVisible()) instance.setVisible();
    }, 200);
  });

  var load = function(data, incoming) {
    var isDev = $('body').data('dev');
    lichess.loadCss('/assets/stylesheets/notifyApp.css');
    lichess.loadScript("/assets/compiled/lichess.notify" + (isDev ? '' : '.min') + '.js').done(function() {
      instance = LichessNotify($element[0], {
        data: data,
        incoming: incoming,
        isVisible: isVisible,
        setCount: function(nb) {
          $toggle.attr('data-count', nb);
        },
        show: function() {
          if (!isVisible()) $toggle.click();
        },
        setNotified: function() {
          lichess.socket.send('notified');
        },
        pulse: function() {
          $toggle.addClass('pulse');
        }
      });
    });
  };

  return {
    update: function(data, incoming) {
      if (!instance) load(data, incoming);
      else instance.update(data, incoming);
    }
  };
})();

(function() {

  /////////////
  // ctrl.js //
  /////////////
  $.ajaxSetup({
    cache: false
  });
  $.userLink = function(u) {
    return $.userLinkLimit(u, false);
  };
  $.userLinkLimit = function(u, limit, klass) {
    var split = u.split(' ');
    var id = split.length == 1 ? split[0] : split[1];
    return (u || false) ? '<a class="user_link ulpt ' + (klass || '') + '" href="/@/' + id + '">' + ((limit || false) ? u.substring(0, limit) : u) + '</a>' : 'Anonymous';
  };
  $.redirect = function(obj) {
    var url;
    if (typeof obj == "string") url = obj;
    else {
      url = obj.url;
      if (obj.cookie) {
        var domain = document.domain.replace(/^.+(\.[^\.]+\.[^\.]+)$/, '$1');
        var cookie = [
          encodeURIComponent(obj.cookie.name) + '=' + obj.cookie.value,
          '; max-age=' + obj.cookie.maxAge,
          '; path=/',
          '; domain=' + domain
        ].join('');
        document.cookie = cookie;
      }
    }
    var href = '//' + location.hostname + '/' + url.replace(/^\//, '');
    lichess.redirectInProgress = href;
    location.href = href;
  };
  $.fp = {};
  $.fp.contains = function(list, needle) {
    return list.indexOf(needle) !== -1;
  };
  $.fp.debounce = function(func, wait, immediate) {
    var timeout;
    return function() {
      var context = this,
        args = arguments;
      var later = function() {
        timeout = null;
        if (!immediate) func.apply(context, args);
      };
      var callNow = immediate && !timeout;
      clearTimeout(timeout);
      timeout = setTimeout(later, wait);
      if (callNow) func.apply(context, args);
    };
  };

  lichess.socket = null;
  $.extend(true, lichess.StrongSocket.defaults, {
    events: {
      following_onlines: function(d, all) {
        $('#friend_box').friends("set", all.d, all.playing, all.patrons);
      },
      following_enters: function(d, all) {
        $('#friend_box').friends('enters', all.d, all.playing, all.patron);
      },
      following_leaves: function(name) {
        $('#friend_box').friends('leaves', name);
      },
      following_playing: function(name) {
        $('#friend_box').friends('playing', name);
      },
      following_stopped_playing: function(name) {
        $('#friend_box').friends('stopped_playing', name);
      },
      new_notification: function(e) {
        $('#site_notifications_tag').attr('data-count', e.unread || 0);
        $.sound.newPM();
      },
      mlat: function(e) {
        var $t = $('#top .server strong');
        if ($t.is(':visible')) {
          $t.text(e);
          var l = parseInt(e || 0) + parseInt(lichess.socket.options.lagTag.text()) - 100;
          var ratio = Math.max(Math.min(l / 1200, 1), 0);
          var hue = ((1 - ratio) * 120).toString(10);
          var color = ['hsl(', hue, ',100%,40%)'].join('');
          $('#top .status .led').css('background', color);
        }
      },
      redirect: function(o) {
        setTimeout(function() {
          lichess.hasToReload = true;
          $.redirect(o);
        }, 300);
      },
      deployPost: function(html) {
        $('#notifications').append(
          '<div id="deploy_post" class="notification">' +
          '<div class="inner"><p data-icon="j" class="is3 text">Site update in progress...</p></div>' +
          '</div>');
        lichess.socket.disconnect(function() {
          $('#deploy_post').remove();
          $('#notifications').append(
            '<div id="deploy_done" class="notification">' +
            '<div class="inner"><p data-icon="E" class="is3 is-green text">Site update complete.</p></div>' +
            '</div>');
          setTimeout(function() {
            $('#deploy_done').fadeOut(1000).remove();
          }, 6000);
        });
      },
      tournamentReminder: function(data) {
        if ($('#tournament_reminder').length || $('body').data("tournament-id") == data.id) return;
        var url = '/tournament/' + data.id;
        $('#notifications').append(
          '<div id="tournament_reminder" class="notification glowed">' +
          '<div class="inner">' +
          '<a data-icon="g" class="text" href="' + url + '">' + data.name + '</a> in progress!' +
          '<div class="actions">' +
          '<a class="withdraw text" href="' + url + '/withdraw" data-icon="b">Withdraw</a>' +
          '<a class="text" href="' + url + '" data-icon="G">Join</a>' +
          '</div>' +
          '</div>' +
          '</div>'
        ).find("a.withdraw").click(function() {
          $.post($(this).attr("href"));
          $('#tournament_reminder').remove();
          return false;
        });
      },
      simulEnd: function(simul) {
        $.modal($(
          '<p>Simul complete!</p><br /><br />' +
          '<a class="button" href="/simul/' + simul.id + '">Back to ' + simul.name + ' simul</a>'
        ));
      }
    },
    params: {},
    options: {
      name: "site",
      lagTag: null,
      debug: location.search.indexOf('debug-ws') != -1,
      prodPipe: location.search.indexOf('prod-ws') != -1,
      resetUrl: location.search.indexOf('reset-ws') != -1
    }
  });

  lichess.readServerFen = function(t) {
    return atob(t.split("").reverse().join(""));
  };

  lichess.openInMobileApp = function(path) {
    if (!/android.+mobile|ipad|iphone|ipod/i.test(navigator.userAgent || navigator.vendor)) return;
    var storage = lichess.storage.make('deep-link');
    var stored = storage.get();
    if (stored > 0) storage.set(stored - 1);
    else {
      $('#deeplink').remove();
      var pane = $('<div id="deeplink">' +
        '<h1>Open with...</h1>' +
        '<a href="lichess://' + path + '">Mobile <strong>app</strong></a>' +
        '<a><strong>Web</strong> browser</a>' +
        '</div>'
      ).find('a').click(function() {
        $('#deeplink').remove();
        document.body.dispatchEvent(new Event('chessground.resize'));
        if ($(this).attr('href')) storage.remove();
        else storage.set(10);
        return true;
      }).end();
      $('body').prepend(pane);
    }
  };

  lichess.userAutocomplete = function($input, opts) {
    opts = opts || {};
    lichess.loadCss('/assets/stylesheets/autocomplete.css');
    lichess.loadScript('/assets/javascripts/vendor/typeahead.jquery.min.js').done(function() {
      $input.typeahead(null, {
        minLength: 3,
        hint: true,
        highlight: false,
        source: function(query, sync, async) {
          $.ajax({
            method: 'get',
            url: '/player/autocomplete',
            data: {
              term: query,
              friend: opts.friend ? 1 : 0
            },
            success: function(res) {
              // hack to fix typeahead limit bug
              if (res.length === 10) res.push(null);
              async(res);
            }
          });
        },
        limit: 10,
        templates: {
          empty: '<div class="empty">No player found</div>',
          pending: lichess.spinnerHtml,
          suggestion: function(a) {
            return '<span class="ulpt" data-href="/@/' + a + '">' + a + '</span>';
          }
        }
      }).bind('typeahead:render', function() {
        lichess.pubsub.emit('content_loaded')();
      });
      if (opts.focus) $input.focus();
      if (opts.onSelect) $input.bind('typeahead:select', function(ev, sel) {
        opts.onSelect(sel);
      }).keypress(function(e) {
        if (e.which == 10 || e.which == 13) opts.onSelect($(this).val());
      });
    });
  };

  lichess.parseFen = (function() {
    var doParseFen = function($elem) {
      if (!$elem || !$elem.jquery) {
        $elem = $('.parse_fen');
      }
      $elem.each(function() {
        var $this = $(this).removeClass('parse_fen');
        var lm = $this.data('lastmove');
        var lastMove = lm ? [lm[0] + lm[1], lm[2] + lm[3]] : null;
        var color = $this.data('color') || lichess.readServerFen($(this).data('y'));
        var ground = $this.data('chessground');
        var playable = !!$this.data('playable');
        var resizable = !!$this.data('resizable');
        var config = {
          coordinates: false,
          viewOnly: !playable,
          resizable: resizable,
          fen: $this.data('fen') || lichess.readServerFen($this.data('z')),
          lastMove: lastMove
        };
        if (color) config.orientation = color;
        if (ground) ground.set(config);
        else $this.data('chessground', Chessground($this[0], config));
      });
    };
    // debounce the first parseFen at first, then process them immediately
    // because chessground initial display does a DOM read (board dimensions)
    // and the play page can have 6 miniboards to display (ongoing games)
    if (document.getElementById('now_playing')) {
      var fun = $.fp.debounce(doParseFen, 400, false);
      setTimeout(function() {
        fun = doParseFen;
      }, 1000);
      return function($elem) {
        fun($elem);
      };
    } else return doParseFen;
  })();

  $(function() {
    if (lichess.lobby) LichessLobby.legacy(document.getElementById('hooks_wrap'), lichess.lobby);
    else if (lichess.analyse) LichessAnalyse.legacy(document.getElementById('lichess'), lichess.analyse);
    else if (lichess.user_analysis) startUserAnalysis(document.getElementById('lichess'), lichess.user_analysis);
    else if (lichess.study) startStudy(document.getElementById('lichess'), lichess.study);
    else if (lichess.practice) startPractice(document.getElementById('lichess'), lichess.practice);
    else if (lichess.puzzle) startPuzzle(lichess.puzzle);
    else if (lichess.tournament) startTournament(document.getElementById('tournament'), lichess.tournament);
    else if (lichess.simul) startSimul(document.getElementById('simul'), lichess.simul);

    // delay so round starts first (just for perceived perf)
    setTimeout(function() {

      $('#friend_box').friends();

      $('#lichess').on('click', '.autoselect', function() {
        $(this).select();
      });

      $('#lichess').on('click', 'button.copy', function() {
        var prev = $('#' + $(this).data('rel'));
        if (!prev) return;
        var usePrompt = function() {
          prompt('Your browser does not support automatic copying. Copy this text manually with Ctrl + C:', prev.val());
        };
        try {
          if (document.queryCommandSupported('copy')) {
            // Awesome! Done in five seconds, can go home.
            prev.select();
            document.execCommand('copy');
          } else if (window.clipboardData) {
            // For a certain specific Internet Explorer version *cough cough IE8*
            window.clipboardData.setData('Text', prev.val());
          } else throw 'nope';
          $(this).attr('data-icon', 'E');
        } catch (e) {
          usePrompt();
        }
      });

      $('body').on('click', '.relation_actions a.relation', function() {
        var $a = $(this).addClass('processing');
        $.ajax({
          url: $a.attr('href'),
          type: 'post',
          success: function(html) {
            $a.parent().html(html);
          }
        });
        return false;
      });

      $('.mselect .button').on('click', function() {
        var $p = $(this).parent();
        $p.toggleClass('shown');
        setTimeout(function() {
          var handler = function(e) {
            if ($.contains($p[0], e.target)) return;
            $p.removeClass('shown');
            $('html').off('click', handler);
          };
          $('html').on('click', handler);
        }, 10);
      });

      document.body.addEventListener('mouseover', lichess.powertip.mouseover);

      function setMoment() {
        $("time.moment").removeClass('moment').each(function() {
          var parsed = moment(this.getAttribute('datetime'));
          var format = this.getAttribute('data-format');
          this.textContent = format === 'calendar' ? parsed.calendar(null, {
            sameElse: 'DD/MM/YYYY HH:mm'
          }) : parsed.format(format);
        });
      }
      setMoment();
      lichess.pubsub.on('content_loaded', setMoment);

      function setMomentFromNow() {
        $("time.moment-from-now").each(function() {
          this.textContent = moment(this.getAttribute('datetime')).fromNow();
        });
      }
      setMomentFromNow();
      lichess.pubsub.on('content_loaded', setMomentFromNow);
      setInterval(setMomentFromNow, 2000);

      if ($('body').hasClass('blind_mode')) {
        var setBlindMode = function() {
          $('[data-hint]').each(function() {
            $(this).attr('aria-label', $(this).data('hint'));
          });
        };
        setBlindMode();
        lichess.pubsub.on('content_loaded', setBlindMode);
      }

      setTimeout(function() {
        if (lichess.socket === null) lichess.socket = lichess.StrongSocket("/socket", false);
      }, 300);

      // themepicker
      $('#themepicker_toggle').one('mouseover', function() {
        var applyBackground = function(v) {
          var bgData = document.getElementById('bg-data');
          bgData ? bgData.innerHTML = 'body.transp::before{background-image:url(' + v + ');}' :
            $('head').append('<style id="bg-data">body.transp::before{background-image:url(' + v + ');}</style>');
        };
        var $themepicker = $('#themepicker');
        var findInBodyClasses = function(choices) {
          var list = document.body.classList;
          for (var i in list)
            if ($.fp.contains(choices, list[i])) return list[i];
        };
        $.ajax({
          url: $(this).data('url'),
          success: function(html) {
            $themepicker.append(html);
            var $body = $('body');
            var $content = $body.children('.content');
            var $dropdown = $themepicker.find('.dropdown');
            var $pieceSprite = $('#piece-sprite');
            var themes = $dropdown.data('themes').split(' ');
            var theme = findInBodyClasses(themes);
            var set = $body.data('piece-set');
            var theme3ds = $dropdown.data('theme3ds').split(' ');
            var theme3d = findInBodyClasses(theme3ds);
            var set3ds = $dropdown.data('set3ds').split(' ');
            var set3d = findInBodyClasses(set3ds);
            var background = $body.data('bg');
            var is3d = $content.hasClass('is3d');
            $themepicker.find('.is2d div.theme').hover(function() {
              $body.removeClass(themes.join(' ')).addClass($(this).data("theme"));
            }, function() {
              $body.removeClass(themes.join(' ')).addClass(theme);
            }).click(function() {
              theme = $(this).data("theme");
              $.post($(this).parent().data("href"), {
                theme: theme
              }, lichess.reloadOtherTabs);
            });
            $themepicker.find('.is2d div.no-square').hover(function() {
              var s = $(this).data("set");
              $pieceSprite.attr('href', $pieceSprite.attr('href').replace(/\w+\.css/, s + '.css'));
            }, function() {
              $pieceSprite.attr('href', $pieceSprite.attr('href').replace(/\w+\.css/, set + '.css'));
            }).click(function() {
              set = $(this).data("set");
              $.post($(this).parent().data("href"), {
                set: set
              }, lichess.reloadOtherTabs);
            });
            $themepicker.find('.is3d div.theme').hover(function() {
              $body.removeClass(theme3ds.join(' ')).addClass($(this).data("theme"));
            }, function() {
              $body.removeClass(theme3ds.join(' ')).addClass(theme3d);
            }).click(function() {
              theme3d = $(this).data("theme");
              $.post($(this).parent().data("href"), {
                theme: theme3d
              }, lichess.reloadOtherTabs);
            });
            $themepicker.find('.is3d div.no-square').hover(function() {
              $body.removeClass(set3ds.join(' ')).addClass($(this).data("set"));
            }, function() {
              $body.removeClass(set3ds.join(' ')).addClass(set3d);
            }).click(function() {
              set3d = $(this).data("set");
              $.post($(this).parent().data("href"), {
                set: set3d
              }, lichess.reloadOtherTabs);
            });
            var showBg = function(bg) {
              $body.removeClass('light dark transp')
                .addClass(bg === 'transp' ? 'transp dark' : bg);
              if ((bg === 'dark' || bg === 'transp') && !$('link[href*="dark.css"]').length)
                $('link[href*="common.css"]').clone().each(function() {
                  $(this).attr('href', $(this).attr('href').replace(/common\.css/, 'dark.css')).appendTo('head');
                });
              if (bg === 'transp' && !$('link[href*="transp.css"]').length) {
                $('link[href*="common.css"]').clone().each(function() {
                  $(this).attr('href', $(this).attr('href').replace(/common\.css/, 'transp.css')).appendTo('head');
                });
                applyBackground($themepicker.find('input.background_image').val());
              }
            };
            var showDimensions = function(is3d) {
              $content.add('#top').removeClass('is2d is3d').addClass(is3d ? 'is3d' : 'is2d');
              if (is3d && !$('link[href*="board-3d.css"]').length)
                $('link[href*="board.css"]').clone().each(function() {
                  $(this).attr('href', $(this).attr('href').replace(/board\.css/, 'board-3d.css')).appendTo('head');
                });
              setZoom(getZoom());
            };
            $themepicker.find('.background a').click(function() {
              background = $(this).data('bg');
              $.post($(this).parent().data('href'), {
                bg: background
              }, function() {
                if (window.Highcharts) lichess.reload();
                lichess.reloadOtherTabs();
              });
              $(this).addClass('active').siblings().removeClass('active');
              return false;
            }).hover(function() {
              showBg($(this).data('bg'));
            }, function() {
              showBg(background);
            }).filter('.' + background).addClass('active');
            $themepicker.find('.dimensions a').click(function() {
              is3d = $(this).data('is3d');
              $.post($(this).parent().data('href'), {
                is3d: is3d
              }, lichess.reloadOtherTabs);
              $(this).addClass('active').siblings().removeClass('active');
              return false;
            }).hover(function() {
              showDimensions($(this).data('is3d'));
            }, function() {
              showDimensions(is3d);
            }).filter('.' + (is3d ? 'd3' : 'd2')).addClass('active');
            lichess.slider().done(function() {
              $themepicker.find('.slider').slider({
                orientation: "horizontal",
                min: 1,
                max: 2,
                range: 'min',
                step: 0.01,
                value: getZoom(),
                slide: function(e, ui) {
                  manuallySetZoom(ui.value);
                }
              });
            });
            $themepicker.find('input.background_image')
              .on('change keyup paste', $.fp.debounce(function() {
                var v = $(this).val();
                $.post($(this).data("href"), {
                  bgImg: v
                }, lichess.reloadOtherTabs);
                applyBackground(v);
              }, 200));
          }
        });
      });

      // Zoom
      var getZoom = function() {
        return lichess.isTrident ? 1 : (lichess.storage.get('zoom') || 1);
      };
      var setZoom = function(zoom) {
        lichess.storage.set('zoom', zoom);

        var $lichessGame = $('.lichess_game, .board_and_ground');
        var $boardWrap = $lichessGame.find('.cg-board-wrap').not('.mini_board .cg-board-wrap');
        var px = function(i) {
          return Math.round(i) + 'px';
        };

        $('.underboard').css("width", px(512 * zoom + 242 + 15));
        $boardWrap.add($('.underboard .center, .progress_bar_container')).css("width", px(512 * zoom));

        if ($('body > .content').hasClass('is3d')) {
          $boardWrap.css("height", px(479.08572 * zoom));
          $lichessGame.css({
            height: px(479.08572 * zoom),
            paddingTop: px(50 * (zoom - 1))
          });
          $('#chat').css("height", px(300 + 529 * (zoom - 1)));
        } else {
          $boardWrap.css("height", px(512 * zoom));
          $lichessGame.css({
            height: px(512 * zoom),
            paddingTop: px(0)
          });
          $('#chat').css("height", px(335 + 510 * (zoom - 1)));
        }

        $('#trainer .overlay_container').css({
          top: px((zoom - 1) * 250),
          left: px((zoom - 1) * 250)
        });
        // doesn't vertical center score at the end, close enough
        $('#trainer .score_container').css("top", px((zoom - 1) * 250));

        if ($lichessGame.length) {
          // if on a board with a game
          $('body > .content').css("margin-left", 'calc(50% - ' + px(246.5 + 256 * zoom) + ')');
        }

        // reflow charts
        window.dispatchEvent(new Event('resize'));

        document.body.dispatchEvent(new Event('chessground.resize'));
      };

      var manuallySetZoom = $.fp.debounce(setZoom, 10);
      if (getZoom() > 1) setZoom(getZoom()); // Instantiate the page's zoom
      lichess.pubsub.on('reset_zoom', function() {
        if (getZoom() > 1) setZoom(getZoom());
      });

      function translateTexts() {
        $('.trans_me').each(function() {
          var t = $(this).removeClass('trans_me');
          if (t.val()) t.val($.trans(t.val()));
          else t.text($.trans(t.text()));
        });
      }
      translateTexts();
      lichess.pubsub.on('content_loaded', translateTexts);

      $('input.user-autocomplete').each(function() {
        var opts = {
          focus: 1,
          friend: $(this).data('friend')
        };
        if ($(this).attr('autofocus')) lichess.userAutocomplete($(this), opts);
        else $(this).one('focus', function() {
          lichess.userAutocomplete($(this), opts);
        });
      });

      $('.infinitescroll:has(.pager a)').each(function() {
        $(this).infinitescroll({
          navSelector: ".pager",
          nextSelector: ".pager a:last",
          itemSelector: ".infinitescroll .paginated_element",
          errorCallback: function() {
            $("#infscr-loading").remove();
          },
          loading: {
            msg: $('<div id="infscr-loading">').html(lichess.spinnerHtml)
          }
        }, function() {
          $("#infscr-loading").remove();
          lichess.pubsub.emit('content_loaded')();
        }).find('div.pager').hide();
      });

      $('#top').on('click', 'a.toggle', function() {
        this.removeAttribute('data-hint');
        $(this).find('span').each(function() {
          this.removeAttribute('data-hint');
        });
        var $p = $(this).parent();
        $p.toggleClass('shown');
        $p.siblings('.shown').removeClass('shown');
        setTimeout(function() {
          var handler = function(e) {
            if ($.contains($p[0], e.target)) return;
            $p.removeClass('shown');
            $('html').off('click', handler);
          };
          $('html').on('click', handler);
        }, 10);
        if ($p.hasClass('auth')) {
          lichess.socket.send('moveLat', true);
          lichess.socket.options.lagTag = $('#top .ping strong');
        }
        return false;
      });

      $('#top .lichess_language').one('mouseover', function() {
        var $links = $(this).find('.language_links'),
          langs = $('body').data('accept-languages').split(',');
        $.ajax({
          url: $links.data('url'),
          success: function(list) {
            $links.find('ul').prepend(list.map(function(lang) {
              var klass = $.fp.contains(langs, lang[0]) ? 'class="accepted"' : '';
              return '<li><button type="submit" ' + klass + ' name="lang" value="' + lang[0] + '">' + lang[1] + '</button></li>';
            }).join(''));
          }
        });
      });

      $('#translation_call .close').click(function() {
        $.post($(this).data("href"));
        $(this).parent().fadeOut(500);
        return false;
      });

      $('a.delete, input.delete').click(function() {
        return confirm('Delete?');
      });
      $('input.confirm, button.confirm').click(function() {
        return confirm('Confirm this action?');
      });

      $('div.content').on('click', 'a.bookmark', function() {
        var t = $(this).toggleClass("bookmarked");
        $.post(t.attr("href"));
        var count = (parseInt(t.text(), 10) || 0) + (t.hasClass("bookmarked") ? 1 : -1);
        t.find('span').html(count > 0 ? count : "");
        return false;
      });

      // minimal touchscreen support for topmenu
      if ('ontouchstart' in window)
        $('#topmenu').on('click', '> section > a', function() {
          return false;
        });

      $('#ham-plate').click(function() {
        document.body.classList.toggle('fpmenu');
      });

      // still bind esc even in form fields
      Mousetrap.prototype.stopCallback = function(e, el, combo) {
        return combo !== 'esc' && (el.tagName === 'INPUT' || el.tagName === 'SELECT' || el.tagName === 'TEXTAREA');
      };
      Mousetrap.bind('esc', function() {
        var $oc = $('.lichess_overboard .close');
        if ($oc.length) $oc.click();
        else $('#ham-plate').click();
        return false;
      });
      Mousetrap.bind('g h', function() {
        location.href = '/';
      });
      Mousetrap.bind(': k a p p a', function() {
        $('body').toggleClass('kappa');
      });
      Mousetrap.bind(': d o g g y', function() {
        $('body').toggleClass('doggy');
      });

      if (window.Fingerprint2) setTimeout(function() {
        var t = +new Date();
        new Fingerprint2({
          excludeJsFonts: true
        }).get(function(res) {
          var time = (+new Date()) - t;
          $.post('/set-fingerprint/' + res + '/' + time);
        });
      }, 500);
    }, 50);
  });


  $.sound = (function() {
    var version = 1;
    var baseUrl = lichess.assetUrl('/assets/sound', true);
    var soundSet = $('body').data('sound-set');
    var volumeStorage = lichess.storage.make('sound-volume');
    var defaultVolume = 0.7;

    var memoize = function(factory) {
      var loaded = {};
      var f = function(key) {
        if (!loaded[key]) loaded[key] = factory(key);
        return loaded[key];
      };
      f.clear = function() {
        loaded = {};
      };
      return f;
    };

    var names = {
      genericNotify: 'GenericNotify',
      move: 'Move',
      capture: 'Capture',
      explode: 'Explosion',
      lowtime: 'LowTime',
      victory: 'Victory',
      defeat: 'Defeat',
      draw: 'Draw',
      tournament1st: 'Tournament1st',
      tournament2nd: 'Tournament2nd',
      tournament3rd: 'Tournament3rd',
      tournamentOther: 'TournamentOther',
      berserk: 'Berserk',
      check: 'Check',
      newChallenge: 'NewChallenge',
      newPM: 'NewPM',
      confirmation: 'Confirmation'
    };
    for (var i = 0; i <= 10; i++) names['countDown' + i] = 'CountDown' + i;

    var volumes = {
      lowtime: 0.5,
      explode: 0.35,
      confirmation: 0.5
    };
    var collection = new memoize(function(k) {
      var set = soundSet;
      if (set === 'music') {
        if ($.fp.contains(['move', 'capture', 'check'], k)) return {
          play: $.noop
        };
        set = 'standard';
      }
      return new Howl({
        src: ['ogg', 'mp3'].map(function(ext) {
          return [baseUrl, set, names[k] + '.' + ext + '?v=' + version].join('/');
        }),
        volume: volumes[k] || 1
      });
    });
    var $control = $('#sound_control');
    var $toggle = $('#sound_state');
    var enabled = function() {
      return soundSet !== 'silent';
    };
    $toggle.toggleClass('sound_state_on', enabled());
    var play = {};
    Object.keys(names).forEach(function(name) {
      play[name] = function() {
        if (!enabled()) return;
        Howler.volume(volumeStorage.get() || defaultVolume);
        collection(name).play();
      }
    });
    play.load = function(name) {
      if (enabled() && name in names) collection(name);
    };
    var setVolume = function(v) {
      volumeStorage.set(v);
      Howler.volume(v);
    };
    var manuallySetVolume = $.fp.debounce(function(v) {
      setVolume(v);
      play.move(true);
    }, 50);
    var publish = function() {
      lichess.pubsub.emit('sound_set')(soundSet);
    };
    setTimeout(publish, 500);
    $toggle.one('mouseover', function() {
      lichess.slider().done(function() {
        $toggle.parent().find('.slider').slider({
          orientation: "vertical",
          min: 0,
          max: 1,
          range: 'min',
          step: 0.01,
          value: volumeStorage.get() || defaultVolume,
          slide: function(e, ui) {
            manuallySetVolume(ui.value);
          }
        });
      });
      var $selector = $toggle.parent().find('form');
      $selector.find('input').on('change', function() {
        soundSet = $(this).val();
        collection.clear();
        play.genericNotify();
        $.post($selector.attr('action'), {
          set: soundSet
        }, lichess.reloadOtherTabs);
        $toggle.toggleClass('sound_state_on', enabled());
        publish();
        return false;
      });
    });

    play.set = function() {
      return soundSet;
    };
    return play;
  })();

  $.trans = function() {
    var str = lichess_translations[arguments[0]];
    if (!str) return arguments[0];
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };

  function startTournamentClock() {
    $("div.game_tournament div.clock").each(function() {
      $(this).clock({
        time: parseFloat($(this).data("time"))
      });
    });
  }

  /////////////
  // game.js //
  /////////////

  lichess.startRound = function(element, cfg) {
    var data = cfg.data;
    lichess.openInMobileApp(data.game.id);
    var round, chat;
    if (data.tournament) $('body').data('tournament-id', data.tournament.id);
    lichess.socket = lichess.StrongSocket(
      data.url.socket,
      data.player.version, {
        options: {
          name: "round"
        },
        params: {
          userTv: $('.user_tv').data('user-tv')
        },
        receive: function(t, d) {
          round.socketReceive(t, d);
        },
        events: {
          crowd: function(e) {
            $watchers.watchers("set", e.watchers);
          },
          tvSelect: function(o) {
            if (data.tv && data.tv.channel == o.channel) lichess.reload();
            else $('#tv_channels a.' + o.channel + ' span').html(
              o.player ? [
                o.player.title,
                o.player.name,
                '(' + o.player.rating + ')'
              ].filter(function(x) {
                return x;
              }).join('&nbsp') : 'Anonymous');
          },
          end: function() {
            var url = '/' + (data.tv ? ['tv', data.tv.channel, data.game.id, data.player.color, 'sides'] : [data.game.id, data.player.color, 'sides', data.player.spectator ? 'watcher' : 'player']).join('/');
            $.ajax({
              url: url,
              success: function(html) {
                var $html = $(html);
                $('#site_header div.side').replaceWith($html.find('>.side'));
                $('#lichess div.crosstable').replaceWith($html.find('>.crosstable'));
                lichess.pubsub.emit('content_loaded')();
                startTournamentClock();
              }
            });
          },
          tournamentStanding: function(id) {
            if (data.tournament && id === data.tournament.id) $.ajax({
              url: '/tournament/' + id + '/game-standing',
              success: function(html) {
                $('#site_header div.game_tournament').replaceWith(html);
                startTournamentClock();
              }
            });
          }
        }
      });
    var getPresetGroup = function(d) {
      if (d.player.spectator) return null;
      if (d.steps.length < 4) return 'start';
      else if (d.game.status.id >= 30) return 'end';
      return null;
    };
    cfg.element = element.querySelector('.round');
    cfg.socketSend = lichess.socket.send;
    cfg.onChange = function(d) {
      if (chat) chat.preset.setGroup(getPresetGroup(d));
    };
    // cfg.isGuineaPig = $('body').data('guineapig');
    round = LichessRound(cfg);
    if (cfg.chat) {
      cfg.chat.preset = getPresetGroup(cfg.data);
      cfg.chat.parseMoves = true;
      lichess.makeChat('chat', cfg.chat, function(c) {
        chat = c;
      });
    }
    $('.crosstable', element).prependTo($('.underboard .center', element)).removeClass('none');
    var $watchers = $('#site_header div.watchers').watchers();
    var $nowPlaying = $('#now_playing');
    startTournamentClock();
    var loadPlaying = function() {
      var $moveOn = $nowPlaying.find('.move_on input').change(function() {
        round.moveOn.toggle();
      }).prop('checked', round.moveOn.get());
    };
    loadPlaying();
    $nowPlaying.on('click', '>a', function() {
      lichess.hasToReload = true;
      return true;
    });
    if (location.pathname.lastIndexOf('/round-next/', 0) === 0)
      history.replaceState(null, null, '/' + data.game.id);
    if (!data.player.spectator && data.game.status.id < 25) lichess.topMenuIntent();
  };

  lichess.widget("watchers", {
    _create: function() {
      this.list = this.element.find("span.list");
      this.number = this.element.find("span.number");
    },
    set: function(data) {
      var self = this;
      if (!data) {
        self.element.addClass('hidden');
        return;
      }
      if (self.number.length) self.number.text(data.nb);
      if (data.users) {
        var tags = data.users.map($.userLink);
        if (data.anons === 1) tags.push('Anonymous');
        else if (data.anons) tags.push('Anonymous(' + data.anons + ')');
        self.list.html(tags.join(', '));
      } else if (!self.number.length) self.list.html(data.nb + ' players in the chat');

      self.element.removeClass('hidden');
    }
  });

  lichess.widget("friends", (function() {
    var isSameUser = function(userId, user) {
      var id = $.fp.contains(user.name, ' ') ? user.name.split(' ')[1] : user.name;
      return id.toLowerCase() === userId;
    };
    return {
      _create: function() {
        var self = this;
        var hideStorage = lichess.storage.make('friends-hide');
        self.$list = self.element.find("div.list");
        var $title = self.element.find('.title').click(function() {
          self.element.find('.content_wrap').toggle(100, function() {
            hideStorage.set($(this).is(':visible') ? 0 : 1);
          });
        });
        if (hideStorage.get() == 1) self.element.find('.content_wrap').addClass('none');
        self.$nbOnline = $title.find('.online');
        self.$nobody = self.element.find("div.nobody");

        var users = self.element.data('preload').split(',');
        var playings = self.element.data('playing').split(',');
        var patrons = self.element.data('patrons').split(',');
        self.set(users, playings, patrons);
      },
      _findByUsername: function(n) {
        return this.users.filter(function(u) {
          return isSameUser(n.toLowerCase(), u);
        })[0];
      },
      _makeUser: function(name, playing, patron) {
        return {
          'name': name,
          'playing': !!playing,
          'patron': !!patron
        }
      },
      _uniqueUsers: function(users) {
        var usersEncountered = [];
        return users.filter(function(u) {
          if (usersEncountered.indexOf(u.name) !== -1) {
            return false;
          } else {
            usersEncountered.push(u.name);
            return true;
          }
        })
      },
      repaint: function() {
        lichess.raf(function() {
          this.users = this._uniqueUsers(this.users.filter(function(u) {
            return u.name !== '';
          }));
          this.$nbOnline.text(this.users.length);
          this.$nobody.toggleClass('none', this.users.length > 0);
          this.$list.html(this.users.sort(function(a, b) {
            return a.name.toLowerCase() < b.name.toLowerCase() ? -1 : 1;
          }).map(this._renderUser).join(""));
        }.bind(this));
      },
      set: function(us, playings, patrons) {
        this.users = us.map(function(user) {
          return this._makeUser(user, false, false);
        }.bind(this));
        for (user in playings) this._setPlaying(playings[user], true);
        for (user in patrons) this._setPatron(patrons[user], true);
        this.repaint();
      },
      enters: function(userName, playing, patron) {
        var user = this._makeUser(userName, playing, patron);
        this.users.push(user);
        this.repaint();
      },
      leaves: function(userName) {
        this.users = this.users.filter(function(u) {
          return u.name != userName
        });
        this.repaint();
      },
      _setPlaying: function(userName, playing) {
        var user = this._findByUsername(userName);
        if (user) user.playing = playing;
      },
      _setPatron: function(userName, patron) {
        var user = this._findByUsername(userName);
        if (user) user.patron = patron;
      },
      playing: function(userName) {
        this._setPlaying(userName, true);
        this.repaint();
      },
      stopped_playing: function(userName) {
        this._setPlaying(userName, false);
        this.repaint();
      },
      _renderUser: function(user) {
        var icon = '<i class="is-green line' + (user.patron ? ' patron' : '') + '"></i>';
        var name = $.fp.contains(user.name, ' ') ? user.name.split(' ')[1] : user.name;
        var url = '/@/' + name;
        var tvButton = user.playing ? '<a data-icon="1" class="tv is-green ulpt" data-pt-pos="nw" href="' + url + '/tv" data-href="' + url + '"></a>' : '';

        return '<div><a class="user_link ulpt" data-pt-pos="nw" href="' + url + '">' + icon + user.name + '</a>' + tvButton + '</div>';
      }
    };
  })());

  lichess.widget("clock", {
    _create: function() {
      var self = this;
      // this.options.time: seconds Integer
      this.time = Math.max(0, this.options.time) * 1000;
      this.timeEl = this.element.find('>.time')[0];
      var tick = function() {
        self.time = Math.max(0, self.time - 1000);
        if (self.time <= 0) clearInterval(self.interval);
        self._show();
      };
      setTimeout(function() {
        tick();
        self.interval = setInterval(tick, 1000);
      }, 1000 - (new Date().getTime()) % 1000);
      self._show();
    },
    _show: function() {
      if (this.time < 0) return;
      this.timeEl.innerHTML = this._formatDate(new Date(this.time));
    },
    _formatDate: function(date) {
      var prefixInt = function(num, length) {
        return (num / Math.pow(10, length)).toFixed(length).substr(2);
      };
      var minutes = prefixInt(date.getUTCMinutes(), 2);
      var seconds = prefixInt(date.getSeconds(), 2);
      if (this.time >= 3600000) {
        var hours = prefixInt(date.getUTCHours(), 2);
        return hours + ':' + minutes + ':' + seconds;
      } else return minutes + ':' + seconds;
    }
  });

  /////////////////
  // gamelist.js //
  /////////////////

  $(function() {
    lichess.pubsub.on('content_loaded', lichess.parseFen);

    var socketOpened = false;

    function startWatching() {
      if (!socketOpened) return;
      var ids = [];
      $('.mini_board.live').removeClass("live").each(function() {
        ids.push(this.getAttribute("data-live"));
      });
      if (ids.length) {
        lichess.socket.send("startWatching", ids.join(" "));
      }
    }
    lichess.pubsub.on('content_loaded', startWatching);
    lichess.pubsub.on('socket.open', function() {
      socketOpened = true;
      startWatching();
    });

    setTimeout(function() {
      lichess.parseFen();
      setTimeout(function() {
        $('div.checkmateCaptcha').each(function() {
          var $captcha = $(this);
          var $board = $captcha.find('.mini_board');
          var $input = $captcha.find('input').val('');
          var cg = $board.data('chessground');
          var dests = JSON.parse(lichess.readServerFen($board.data('x')));
          for (var k in dests) dests[k] = dests[k].match(/.{2}/g);
          cg.set({
            turnColor: cg.getOrientation(),
            movable: {
              free: false,
              dests: dests,
              color: cg.getOrientation(),
              coordinates: false,
              events: {
                after: function(orig, dest) {
                  $captcha.removeClass("success failure");
                  submit(orig + ' ' + dest);
                }
              }
            },
            disableContextMenu: true
          });

          var submit = function(solution) {
            $input.val(solution);
            $.ajax({
              url: $captcha.data('check-url'),
              data: {
                solution: solution
              },
              success: function(data) {
                $captcha.toggleClass('success', data == 1);
                $captcha.toggleClass('failure', data != 1);
                if (data == 1) $board.data('chessground').stop();
                else setTimeout(function() {
                  lichess.parseFen($board);
                  $board.data('chessground').set({
                    turnColor: cg.getOrientation(),
                    movable: {
                      dests: dests
                    }
                  });
                }, 300);
              }
            });
          };
        });
      }, 1000);
    }, 200);
  });

  ///////////////////
  // tournament.js //
  ///////////////////

  function startTournament(element, cfg) {
    $('body').data('tournament-id', cfg.data.id);
    var $watchers = $("div.watchers").watchers();
    var tournament;
    lichess.socket = lichess.StrongSocket(
      '/tournament/' + cfg.data.id + '/socket/v1', cfg.data.socketVersion, {
        receive: function(t, d) {
          return tournament.socketReceive(t, d);
        },
        events: {
          crowd: function(data) {
            $watchers.watchers("set", data);
          }
        },
        options: {
          name: "tournament"
        }
      });
    cfg.socketSend = lichess.socket.send;
    tournament = LichessTournament(element, cfg);
    if (cfg.chat) lichess.makeChat('chat', cfg.chat);
  };

  ///////////////////
  // simul.js //
  ///////////////////

  $(function() {

    var $simulList = $('#simul_list');
    if ($simulList.length) {
      // handle simul list
      lichess.StrongSocket.defaults.params.flag = "simul";
      lichess.StrongSocket.defaults.events.reload = function() {
        $simulList.load($simulList.data("href"), function() {
          lichess.pubsub.emit('content_loaded')();
        });
      };
      $('#site_header .help a.more').click(function() {
        $.modal($(this).parent().find('div.more')).addClass('card');
      });
      return;
    }
  });

  function startSimul(element, cfg) {
    $('body').data('simul-id', cfg.data.id);
    var $watchers = $("div.watchers").watchers();
    var simul;
    lichess.socket = lichess.StrongSocket(
      '/simul/' + cfg.data.id + '/socket/v1', cfg.socketVersion, {
        receive: function(t, d) {
          simul.socketReceive(t, d);
        },
        events: {
          crowd: function(data) {
            $watchers.watchers("set", data);
          }
        },
        options: {
          name: "simul"
        }
      });
    cfg.socketSend = lichess.socket.send;
    simul = LichessSimul(element, cfg);
    if (cfg.chat) lichess.makeChat('chat', cfg.chat);
  };

  ////////////////
  // user_analysis.js //
  ////////////////

  function startUserAnalysis(element, cfg) {
    var analyse;
    cfg.initialPly = 'url';
    cfg.element = element.querySelector('.analyse');
    lichess.socket = lichess.StrongSocket('/analysis/socket', false, {
      options: {
        name: "analyse"
      },
      receive: function(t, d) {
        analyse.socketReceive(t, d);
      }
    });
    cfg.socketSend = lichess.socket.send;
    analyse = LichessAnalyse.mithril(cfg);
    lichess.topMenuIntent();
  }

  ////////////////
  // study.js //
  ////////////////

  function startStudy(element, cfg) {
    var $watchers = $("div.watchers").watchers();
    var analyse;
    cfg.initialPly = 'url';
    cfg.element = element.querySelector('.analyse');
    cfg.sideElement = document.querySelector('#site_header .side_box');
    lichess.socket = lichess.StrongSocket(cfg.socketUrl, cfg.socketVersion, {
      options: {
        name: "study"
      },
      receive: function(t, d) {
        analyse.socketReceive(t, d);
      },
      events: {
        crowd: function(e) {
          $watchers.watchers("set", e);
        }
      }
    });
    cfg.socketSend = lichess.socket.send;
    analyse = LichessAnalyse.mithril(cfg);
    if (cfg.chat) {
      lichess.pubsub.on('chat.enabled', function(v) {
        $('#site_header .board_left').toggleClass('no_chat', !v);
      });
      lichess.makeChat('chat', cfg.chat);
    }
    lichess.topMenuIntent();
  }

  ////////////////
  // practice.js //
  ////////////////

  function startPractice(element, cfg) {
    var analyse;
    cfg.element = element.querySelector('.analyse');
    cfg.sideElement = document.querySelector('#site_header .side_box');
    lichess.socket = lichess.StrongSocket('/analysis/socket', false, {
      options: {
        name: "practice"
      },
      receive: function(t, d) {
        analyse.socketReceive(t, d);
      }
    });
    cfg.socketSend = lichess.socket.send;
    analyse = LichessAnalyse.mithril(cfg);
    lichess.topMenuIntent();
  }

  ////////////////
  // puzzle.js //
  ////////////////

  function startPuzzle(cfg) {
    var puzzle;
    cfg.element = document.querySelector('#puzzle');
    cfg.sideElement = document.querySelector('#site_header .puzzle_side');
    lichess.socket = lichess.StrongSocket('/socket', false, {
      options: {
        name: "puzzle"
      },
      receive: function(t, d) {
        puzzle.socketReceive(t, d);
      }
    });
    cfg.socketSend = lichess.socket.send;
    puzzle = LichessPuzzle(cfg);
    lichess.topMenuIntent();
  }

})();
