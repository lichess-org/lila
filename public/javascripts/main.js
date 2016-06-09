// ==ClosureCompiler==
// @compilation_level ADVANCED_OPTIMIZATIONS
// ==/ClosureCompiler==

lichess.challengeApp = (function() {
  var instance;
  var $toggle = $('#challenge_notifications_tag');
  $toggle.one('mouseover click', function() {
    if (!instance) load();
  });
  var load = function(data) {
    var isDev = $('body').data('dev');
    lichess.loadCss('/assets/stylesheets/challengeApp.css');
    lichess.loadScript("/assets/compiled/lichess.challenge" + (isDev ? '' : '.min') + '.js').done(function() {
      instance = LichessChallenge(document.getElementById('challenge_app'), {
        data: data,
        show: function() {
          if (!$(element).is(':visible')) $toggle.click();
        },
        setCount: function(nb) {
          $toggle.attr('data-count', nb);
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
    $.redirect.inProgress = href;
    location.href = href;
  };
  $.fp = {};
  $.fp.range = function(to) {
    return Array.apply(null, Array(to)).map(function(_, i) {
      return i;
    });
  };
  $.fp.contains = function(list, needle) {
    return list.indexOf(needle) !== -1;
  };
  $.fp.find = function(list, pred) {
    for (var i = 0, len = list.length; i < len; i++) {
      if (pred(list[i])) return list[i];
    }
    return undefined;
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
  $.spreadNumber = function(el, nbSteps, getDuration) {
    var previous, displayed;
    var display = function(prev, cur, it) {
      var val = lichess.numberFormat(Math.round(((prev * (nbSteps - 1 - it)) + (cur * (it + 1))) / nbSteps));
      if (val !== displayed) {
        el.textContent = val;
        displayed = val;
      }
    };
    var timeouts = [];
    return function(nb) {
      if (!el || !nb) return;
      timeouts.forEach(clearTimeout);
      timeouts = [];
      var prev = previous || nb;
      previous = nb;
      var interv = getDuration() / nbSteps;
      for (var i = 0; i < nbSteps; i++) {
        timeouts.push(setTimeout(display.bind(null, prev, nb, i), Math.round(i * interv)));
      }
    };
  };

  lichess.socket = null;
  lichess.idleTime = 20 * 60 * 1000;
  $.extend(true, lichess.StrongSocket.defaults, {
    events: {
      following_onlines: function(us) {
        $('#friend_box').friends("set", us);
      },
      following_enters: function(name) {
        $('#friend_box').friends('enters', name);
      },
      following_leaves: function(name) {
        $('#friend_box').friends('leaves', name);
      },
      message: function(msg) {
        $('#chat').chat("append", msg);
      },
      new_notification: function(e) {
        var notification = e.notification;

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
      lagTag: $('#top .ping strong'),
      debug: location.search.indexOf('debug-ws') != -1,
      prodPipe: location.search.indexOf('prod-ws') != -1,
      resetUrl: location.search.indexOf('reset-ws') != -1
    }
  });

  lichess.hasToReload = false;
  lichess.reload = function() {
    if ($.redirect.inProgress) return;
    lichess.hasToReload = true;
    if (window.location.hash) location.reload();
    else location.href = location.href;
  };

  lichess.readServerFen = function(t) {
    return atob(t.split("").reverse().join(""));
  };

  lichess.openInMobileApp = function(gameId) {
    if (!/android.+mobile|ipad|iphone|ipod/i.test(navigator.userAgent || navigator.vendor)) return false;
    var storageKey = 'open-game-in-mobile';
    var open = function(v) {
      if (v > 0) {
        lichess.storage.set(storageKey, v - 1);
        location.href = 'lichess://' + gameId;
        return true;
      }
      lichess.storage.set(storageKey, v + 1);
      return false;
    };
    var stored = parseInt(lichess.storage.get(storageKey));
    if (stored) return open(stored);
    return open(confirm('Open in lichess mobile app?') ? 10 : -10);
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
            url: '/player/autocomplete?term=' + query,
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
        $('body').trigger('lichess.content_loaded');
      });
      if (opts.focus) $input.focus();
      if (opts.onSelect) $input.bind('typeahead:select', function(ev, sel) {
        opts.onSelect(sel);
      }).keypress(function(e) {
        if (e.which == 10 || e.which == 13) opts.onSelect($(this).val());
      });
    });
  };

  lichess.parseFen = function($elem) {
    if (!$elem || !$elem.jquery) {
      $elem = $('.parse_fen');
    }
    $elem.each(function() {
      var $this = $(this).removeClass('parse_fen');
      var lm = $this.data('lastmove');
      var lastMove = [];
      if (lm) {
        if (lm[1] === '@') lastMove = [lm.slice(2), lm.slice(2)];
        else lastMove = [lm[0] + lm[1], lm[2] + lm[3]];
      }
      var color = $this.data('color') || lichess.readServerFen($(this).data('y'));
      var ground = $this.data('chessground');
      var playable = $this.data('playable');
      var config = {
        coordinates: false,
        viewOnly: !playable,
        minimalDom: !playable,
        resizable: false,
        fen: $this.data('fen') || lichess.readServerFen($this.data('z')),
        lastMove: lastMove
      };
      if (color) config.orientation = color;
      if (ground) ground.set(config);
      else $this.data('chessground', Chessground($this[0], config));
    });
  }

  $(function() {
    if (!lichess.StrongSocket.available) {
      $('#lichess').on('mouseover', function() {
        $('#lichess').off('mouseover');
        var inUrFaceUrl = window.opera ? '/assets/opera-websocket.html' : '/assets/browser.html';
        $.ajax(inUrFaceUrl, {
          success: function(html) {
            $('body').prepend(html);
          }
        });
      });
    }

    if (lichess.analyse) startAnalyse(document.getElementById('lichess'), lichess.analyse);
    else if (lichess.user_analysis) startUserAnalysis(document.getElementById('lichess'), lichess.user_analysis);
    else if (lichess.study) startStudy(document.getElementById('lichess'), lichess.study);
    else if (lichess.lobby) startLobby(document.getElementById('hooks_wrap'), lichess.lobby);
    else if (lichess.tournament) startTournament(document.getElementById('tournament'), lichess.tournament);
    else if (lichess.simul) startSimul(document.getElementById('simul'), lichess.simul);

    // delay so round starts first (just for perceived perf)
    setTimeout(function() {

      $('#lichess').on('click', '.socket-link:not(.disabled)', function() {
        lichess.socket.send($(this).data('msg'), $(this).data('data'));
      });

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

      lichess.userPowertip = function($els, placement) {
        $els.removeClass('ulpt').powerTip({
          intentPollInterval: 200,
          fadeInTime: 100,
          fadeOutTime: 100,
          placement: placement,
          mouseOnToPopup: true,
          closeDelay: 200
        }).on({
          powerTipPreRender: function() {
            $.ajax({
              url: ($(this).attr('href') || $(this).data('href')).replace(/\?.+$/, '') + '/mini',
              success: function(html) {
                $('#powerTip').html(html);
                $('body').trigger('lichess.content_loaded');
              }
            });
          }
        }).data('powertip', lichess.spinnerHtml);
      };

      function gamePowertip($els, placement) {
        $els.removeClass('glpt').powerTip({
          intentPollInterval: 200,
          fadeInTime: 100,
          fadeOutTime: 100,
          placement: placement,
          smartPlacement: true,
          mouseOnToPopup: true,
          closeDelay: 200,
          popupId: 'miniGame'
        }).on({
          powerTipPreRender: function() {
            $.ajax({
              url: ($(this).attr('href') || $(this).data('href')).replace(/\?.+$/, '') + '/mini',
              success: function(html) {
                $('#miniGame').html(html);
                $('body').trigger('lichess.content_loaded');
              }
            });
          }
        }).data('powertip', lichess.spinnerHtml);
      }

      function updatePowertips() {
        lichess.userPowertip($('#site_header .ulpt'), 'e');
        lichess.userPowertip($('#friend_box .ulpt'), 'nw');
        lichess.userPowertip($('.ulpt'), 'w');
        gamePowertip($('.glpt'), 'w');
      }
      setTimeout(updatePowertips, 600);
      $('body').on('lichess.content_loaded', updatePowertips);

      function setMoment() {
        $("time.moment").removeClass('moment').each(function() {
          var parsed = moment(this.getAttribute('datetime'));
          var format = this.getAttribute('data-format');
          this.textContent = format === 'calendar' ? parsed.calendar() : parsed.format(format);
        });
      }
      setMoment();
      $('body').on('lichess.content_loaded', setMoment);

      function setMomentFromNow() {
        $("time.moment-from-now").each(function() {
          this.textContent = moment(this.getAttribute('datetime')).fromNow();
        });
      }
      setMomentFromNow();
      $('body').on('lichess.content_loaded', setMomentFromNow);
      setInterval(setMomentFromNow, 2000);

      if ($('body').hasClass('blind_mode')) {
        var setBlindMode = function() {
          $('[data-hint]').each(function() {
            $(this).attr('aria-label', $(this).data('hint'));
          });
        };
        setBlindMode();
        $('body').on('lichess.content_loaded', setBlindMode);
      }

      setTimeout(function() {
        if (lichess.socket === null) {
          lichess.socket = lichess.StrongSocket("/socket", 0);
        }
        $.idleTimer(lichess.idleTime, lichess.socket.destroy, lichess.socket.connect);
      }, 200);

      // themepicker
      $('#themepicker_toggle').one('mouseover', function() {
        var applyBackground = function(v) {
          var bgData = document.getElementById('bg-data');
          bgData ? bgData.innerHTML = 'body.transp::before{background-image:url(' + v + ');}' :
            $('head').append('<style id="bg-data">body.transp::before{background-image:url(' + v + ');}</style>');
        };
        var $themepicker = $('#themepicker');
        $.ajax({
          url: $(this).data('url'),
          success: function(html) {
            $themepicker.append(html);
            var $body = $('body');
            var $content = $body.children('.content');
            var $dropdown = $themepicker.find('.dropdown');
            var $pieceSprite = $('#piece-sprite');
            var themes = $dropdown.data('themes').split(' ');
            var theme = $.fp.find(document.body.classList, function(a) {
              return $.fp.contains(themes, a);
            });
            var set = $body.data('piece-set');
            var theme3ds = $dropdown.data('theme3ds').split(' ');
            var theme3d = $.fp.find(document.body.classList, function(a) {
              return $.fp.contains(theme3ds, a);
            });
            var set3ds = $dropdown.data('set3ds').split(' ');
            var set3d = $.fp.find(document.body.classList, function(a) {
              return $.fp.contains(set3ds, a);
            });
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
              });
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
              });
            });
            $themepicker.find('.is3d div.theme').hover(function() {
              $body.removeClass(theme3ds.join(' ')).addClass($(this).data("theme"));
            }, function() {
              $body.removeClass(theme3ds.join(' ')).addClass(theme3d);
            }).click(function() {
              theme3d = $(this).data("theme");
              $.post($(this).parent().data("href"), {
                theme: theme3d
              });
            });
            $themepicker.find('.is3d div.no-square').hover(function() {
              $body.removeClass(set3ds.join(' ')).addClass($(this).data("set"));
            }, function() {
              $body.removeClass(set3ds.join(' ')).addClass(set3d);
            }).click(function() {
              set3d = $(this).data("set");
              $.post($(this).parent().data("href"), {
                set: set3d
              });
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
              });
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
                });
                applyBackground(v);
              }, 200));
          }
        });
      });

      // Zoom
      var getZoom = function() {
        return (lichess.isTrident || lichess.isSafari) ? 1 : (lichess.storage.get('zoom') || 1);
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
        $lichessGame.find('.lichess_overboard').css("left", px(56 + (zoom - 1) * 254));

        if ($('body > .content').hasClass('is3d')) {
          $boardWrap.css("height", px(479.08572 * zoom));
          $lichessGame.css({
            height: px(479.08572 * zoom),
            paddingTop: px(50 * (zoom - 1))
          });
          $('.chat_panels').css("height", px(290 + 529 * (zoom - 1)));
        } else {
          $boardWrap.css("height", px(512 * zoom));
          $lichessGame.css({
            height: px(512 * zoom),
            paddingTop: px(0)
          });
          $('.chat_panels').css("height", px(325 + 510 * (zoom - 1)));
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

        document.body.dispatchEvent(new Event('chessground.resize'));
      };

      var manuallySetZoom = $.fp.debounce(setZoom, 10);
      if (getZoom() > 1) setZoom(getZoom()); // Instantiate the page's zoom
      $('body').on('lichess.reset_zoom', function() {
        setZoom(getZoom());
      });

      function translateTexts() {
        $('.trans_me').each(function() {
          $(this).removeClass('trans_me');
          if ($(this).val()) $(this).val($.trans($(this).val()));
          else $(this).text($.trans($(this).text()));
        });
      }
      translateTexts();
      $('body').on('lichess.content_loaded', translateTexts);

      $('input.user-autocomplete').each(function() {
        if ($(this).attr('autofocus')) lichess.userAutocomplete($(this), {
          focus: 1
        });
        else $(this).one('focus', function() {
          lichess.userAutocomplete($(this), {
            focus: 1
          });
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
          $('body').trigger('lichess.content_loaded');
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
        if ($p.hasClass('auth')) lichess.socket.send('moveLat', true);
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
      Mousetrap.bind('esc', function() {
        var $overboard = $('.lichess_overboard .close');
        if ($overboard.length) $overboard.click();
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

  $.lazy = function(factory) {
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

  $.sound = (function() {
    var version = 1;
    var baseUrl = '/assets/sound';
    var soundSet = $('body').data('sound-set');
    Howler.volume(lichess.storage.get('sound-volume') || 0.7);

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
    var collection = new $.lazy(function(k) {
      var set = soundSet;
      if (set === 'music') {
        if (k === 'move' || k === 'capture' || k === 'check') return {
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
        if (enabled()) collection(name).play();
      }
    });
    var setVolume = function(v) {
      lichess.storage.set('sound-volume', v);
      Howler.volume(v);
    };
    var manuallySetVolume = $.fp.debounce(function(v) {
      setVolume(v);
      play.move(true);
    }, 50);
    var publish = function() {
      $('body').trigger('lichess.sound_set', soundSet);
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
          value: Howler.volume(),
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
        });
        $toggle.toggleClass('sound_state_on', enabled());
        publish();
        return false;
      });
    });


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

  $.urlToLink = function(text) {
    var exp = /\bhttps?:\/\/(?:[a-z]{0,3}\.)?(lichess\.org[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/ig;
    return text.replace(exp, "<a href='//$1'>$1</a>");
  }

  function startTournamentClock() {
    $("div.game_tournament div.clock").each(function() {
      $(this).clock({
        time: parseFloat($(this).data("time"))
      });
    });
  }

  function topMenuIntent() {
    $('#topmenu.hover').removeClass('hover').hoverIntent(function() {
      $(this).toggleClass('hover');
    });
  }

  /////////////
  // game.js //
  /////////////

  lichess.startRound = function(element, cfg) {
    var data = cfg.data;
    if (data.player.spectator && lichess.openInMobileApp(data.game.id)) return;
    var round;
    if (data.tournament) $('body').data('tournament-id', data.tournament.id);
    lichess.socket = lichess.StrongSocket(
      data.url.socket,
      data.player.version, {
        options: {
          name: "round"
        },
        params: {
          ran: "--ranph--",
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
                $('body').trigger('lichess.content_loaded');
                startTournamentClock();
              }
            });
          },
          tournamentStanding: function(id) {
            $.ajax({
              url: '/tournament/' + id + '/game-standing',
              success: function(html) {
                $('#site_header div.game_tournament').replaceWith(html);
                startTournamentClock();
              }
            });
          }
        }
      });
    var $chat;
    cfg.element = element.querySelector('.round');
    cfg.socketSend = lichess.socket.send;
    cfg.onChange = data.player.spectator ? $.noop : function(data) {
      var presets = [];
      if (data.steps.length < 4) presets = [
        'hi/Hello', 'gl/Good luck', 'hf/Have fun!', 'u2/You too!'
      ];
      else if (data.game.status.id >= 30) presets = [
        'gg/Good game', 'wp/Well played', 'ty/Thank you', 'gtg/I\'ve got to go', 'bye/Bye!'
      ];
      $chat.chat('setPresets', presets);
    };
    round = LichessRound(cfg);
    $('.crosstable', element).prependTo($('.underboard .center', element)).show();
    $chat = $('#chat').chat({
      messages: data.chat,
      initialNote: data.note,
      gameId: data.game.id
    });
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
      window.history.replaceState(null, null, '/' + data.game.id);
    if (!data.player.spectator && data.game.status.id < 25) {
      lichess.storage.set('last-game', data.game.id);
      topMenuIntent();
    }
  };

  lichess.widget("watchers", {
    _create: function() {
      this.list = this.element.find("span.list");
      this.number = this.element.find("span.number");
    },
    set: function(data) {
      var self = this;
      if (!data) {
        self.element.hide();
        return;
      }
      if (self.number.length) self.number.text(data.nb);
      if (data.users) {
        var tags = data.users.map($.userLink);
        if (data.anons === 1) tags.push('Anonymous');
        else if (data.anons) tags.push('Anonymous(' + data.anons + ')');
        self.list.html(tags.join(', '));
      } else if (!self.number.length) self.list.html(data.nb + ' players in the chat');

      self.element.show();
    }
  });

  lichess.widget("friends", {
    _create: function() {
      var self = this;
      self.$list = self.element.find("div.list");
      self.$title = self.element.find('.title').click(function() {
        self.element.find('.content_wrap').toggle(100, function() {
          lichess.storage.set('friends-hide', $(this).is(':visible') ? 0 : 1);
        });
      });
      if (lichess.storage.get('friends-hide') == 1) self.$title.click();
      self.$nbOnline = self.$title.find('.online');
      self.$nobody = self.element.find("div.nobody");
      self.set(self.element.data('preload').split(','));
    },
    repaint: function() {
      this.users = lichess.unique(this.users.filter(function(u) {
        return u !== '';
      }));
      this.$nbOnline.text(this.users.length);
      this.$nobody.toggle(this.users.length === 0);
      this.$list.html(this.users.map(this._renderUser).join(""));
      $('body').trigger('lichess.content_loaded');
    },
    set: function(us) {
      this.users = us || [];
      this.repaint();
    },
    enters: function(user) {
      this.users.push(user);
      this.repaint();
    },
    leaves: function(user) {
      this.users = this.users.filter(function(u) {
        return u != user
      });
      this.repaint();
    },
    _renderUser: function(user) {
      var id = $.fp.contains(user, ' ') ? user.split(' ')[1] : user;
      return '<a class="ulpt" href="/@/' + id + '">' + user + '</a>';
    }
  });

  lichess.widget("chat", {
    _create: function() {
      this.options = $.extend({
        messages: [],
        writeable: true,
        initialNote: '',
        gameId: null,
        presets: [],
        presetCount: 0
      }, this.options);
      var self = this;
      var $parent = self.element.parent();
      self.$msgs = self.element.find('.messages');
      self.withMsgs = !!self.$msgs.length;
      if (self.withMsgs) {
        self.$msgs.on('click', 'a', function() {
          $(this).attr('target', '_blank');
        });
        var $form = self.element.find('form');
        var $input = self.element.find('input.lichess_say')
          .focus(function() {
            document.body.classList.add('typing');
            warning();
          }).blur(function() {
            document.body.classList.remove('typing');
          });
        self.options.placeholder = $input.attr('placeholder');
        self.writeable(self.options.writeable);

        var warning = function() {
          if (lichess.once('chat-nice-notice')) $input.powerTip({
            manual: true,
            fadeInTime: 300,
            fadeOutTime: 300,
            placement: 'n'
          }).data('powertipjq', $('<div class="info">').html([
            $('<strong class="title text" data-icon="">').text('Public notice'),
            $('<div class="content">').html([
              'Failure to be nice towards other players can lead to losing chat privileges or account closure!',
              $('<div class="confirm">').html(
                $('<button class="button">').text('OK').click(function() {
                  $input.focus();
                })
              )
            ])
          ])).powerTip('show');
        };

        // send a message
        $form.submit(function() {
          var text = $.trim($input.val());
          if (!text) return false;
          if (text.length > 140) {
            alert('Max length: 140 chars. ' + text.length + ' chars used.');
            return false;
          }
          $input.val('');
          lichess.socket.send('talk', text);
          return false;
        });

        self.element.find('a.send').click(function() {
          $input.trigger('click');
          $form.submit();
        });

        // toggle the chat
        var $toggle = $parent.find('input.toggle_chat');
        $toggle.change(function() {
          var enabled = $toggle.is(':checked');
          self.element.toggleClass('hidden', !enabled);
          if (!enabled) lichess.storage.set('nochat', 1);
          else lichess.storage.remove('nochat');
        });
        $toggle[0].checked = lichess.storage.get('nochat') != 1;
        if (!$toggle[0].checked) {
          self.element.addClass('hidden');
        }
        if (self.options.messages.length > 0) self._appendMany(self.options.messages);
        self.element.on('click', '.presets button', function() {
          $input.val($(this).data('hint'));
          $form.submit();
          if (++self.options.presetCount >= 2) self.element.find('.presets').remove();
          $input.focus();
        });
        self._renderPresets();
      }

      $panels = self.element.find('div.chat_panels > div');
      $menu = $parent.find('.chat_menu');
      $menu.on('click', 'a', function() {
        var panel = $(this).data('panel');
        $(this).siblings('.active').removeClass('active').end().addClass('active');
        $panels.removeClass('active').filter('.' + panel).addClass('active');
      }).find('a[data-panel=preferences]').one('click', function() {
        self.element.find('.preferences form').each(function() {
          var $form = $(this);
          $form.find('group.radio').removeClass('radio');
          $form.find('input').change(function() {
            $.ajax({
              url: $form.attr('action'),
              method: $form.attr('method'),
              data: $form.serialize()
            });
          });
        });
      });
      $menu.find('a:first').click();

      $notes = self.element.find('.notes textarea');
      if (self.options.gameId && $notes.length) {
        $notes.on('change keyup paste', $.fp.debounce(function() {
          $.post('/' + self.options.gameId + '/note', {
            text: $notes.val()
          });
        }, 1000));
        $notes.val(self.options.initialNote || '');
      }
    },
    writeable: function(v) {
      this.options.writeable = v;
      this.element.find('input.lichess_say')
        .prop('disabled', !v)
        .prop('placeholder', v ? this.options.placeholder : 'Invited members only');
    },
    append: function(msg) {
      this._appendHtml(this._render(msg));
    },
    setPresets: function(presets) {
      if (presets.join('|') === this.options.presets.join('|')) return;
      this.options.presetCount = 0;
      this.options.presets = presets;
      this._renderPresets();
    },
    _renderPresets: function() {
      var $e = this.element.find('.messages_container');
      $e.find('.presets').remove();
      if (!this.options.presets.length) return;
      $e.append($('<div>').addClass('presets').html(
        this.options.presets.map(function(p) {
          var s = p.split('/');
          return '<button class="button hint--top thin" data-hint="' + s[1] + '">' + s[0] + '</button>';
        }).join('')
      ));
    },
    _appendMany: function(objs) {
      var self = this,
        html = "";
      $.each(objs, function() {
        html += self._render(this);
      });
      self._appendHtml(html);
    },
    _render: function(msg) {
      var user, sys = false;
      if (msg.c) {
        user = '<span class="color">[' + msg.c + ']</span>';
      } else if (msg.u == 'lichess') {
        sys = true;
        user = '<span class="system"></span>';
      } else {
        user = '<span class="user">' + $.userLinkLimit(msg.u, 14) + '</span>';
      }
      return '<li class="' + (sys ? 'system trans_me' : '') + (msg.r ? ' troll' : '') + '">' + user + $.urlToLink(msg.t) + '</li>';
    },
    _appendHtml: function(html) {
      if (!html) return;
      this.$msgs.each(function(i, el) {
        var autoScroll = (el.scrollTop == 0 || (el.scrollTop > (el.scrollHeight - el.clientHeight - 50)));
        $(el).append(html);
        if (autoScroll) el.scrollTop = 999999;
      });
      $('body').trigger('lichess.content_loaded');
    }
  });

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
      var b = function(x) {
        return '<b>' + x + '</b>';
      };
      var minutes = prefixInt(date.getUTCMinutes(), 2);
      var seconds = prefixInt(date.getSeconds(), 2);
      if (this.time >= 3600000) {
        var hours = prefixInt(date.getUTCHours(), 2);
        return b(hours) + ':' + b(minutes) + ':' + b(seconds);
      } else return b(minutes) + ':' + b(seconds);
    }
  });

  /////////////////
  // gamelist.js //
  /////////////////

  $(function() {
    $('body').on('lichess.content_loaded', lichess.parseFen);

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
    $('body').on('lichess.content_loaded', startWatching);
    $('body').on('socket.open', function() {
      socketOpened = true;
      startWatching();
    });

    setTimeout(function() {
      lichess.parseFen();
      $('div.checkmateCaptcha').each(function() {
        var $captcha = $(this);
        var $board = $captcha.find('.mini_board');
        var $input = $captcha.find('input').val('');
        var cg = $board.data('chessground');
        var dests = JSON.parse(lichess.readServerFen($board.data('x')));

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
    }, 200);
  });

  function startLobby(element, cfg) {
    var lobby;
    var nbRoundSpread = $.spreadNumber(
      document.querySelector('#nb_games_in_play span'),
      8,
      function() {
        return lichess.socket.pingInterval();
      });
    var nbUserSpread = $.spreadNumber(
      document.querySelector('#nb_connected_players > strong'),
      10,
      function() {
        return lichess.socket.pingInterval();
      });
    var onFirstConnect = function() {
      var gameId = lichess.getParameterByName('hook_like');
      if (!gameId) return;
      $.post('/setup/hook/' + lichess.StrongSocket.sri + '/like/' + gameId);
      lobby.setTab('real_time');
      window.history.replaceState(null, null, '/');
    };
    var resizeTimeline = function() {
      var e = $('#timeline');
      if (e.length) e.height(561 - e.offset().top);
    };
    resizeTimeline();
    lichess.socket = lichess.StrongSocket(
      '/lobby/socket/v1',
      cfg.data.version, {
        receive: function(t, d) {
          lobby.socketReceive(t, d);
        },
        events: {
          n: function(nbUsers, msg) {
            nbUserSpread(msg.d);
            setTimeout(function() {
              nbRoundSpread(msg.r);
            }, lichess.socket.pingInterval() / 2);
          },
          reload_timeline: function() {
            $.ajax({
              url: $("#timeline").data('href'),
              success: function(html) {
                $('#timeline').html(html);
                resizeTimeline();
                $('body').trigger('lichess.content_loaded');
              }
            });
          },
          streams: function(html) {
            $('#streams_on_air').html(html);
          },
          featured: function(o) {
            $('#featured_game').html(o.html);
            $('body').trigger('lichess.content_loaded');
          },
          redirect: function(e) {
            lobby.setRedirecting();
            $.redirect(e);
          },
          tournaments: function(data) {
            $("#enterable_tournaments").html(data);
            $('body').trigger('lichess.content_loaded');
          },
          simuls: function(data) {
            $("#enterable_simuls").html(data).parent().toggle($('#enterable_simuls tr').length > 0);
            $('body').trigger('lichess.content_loaded');
          },
          reload_forum: function() {
            var $newposts = $("div.new_posts");
            setTimeout(function() {
              $.ajax({
                url: $newposts.data('url'),
                success: function(data) {
                  $newposts.find('ol').html(data).end().scrollTop(0);
                  $('body').trigger('lichess.content_loaded');
                }
              });
            }, Math.round(Math.random() * 5000));
          },
          fen: function(e) {
            lichess.StrongSocket.defaults.events.fen(e);
            lobby.gameActivity(e.id);
          }
        },
        options: {
          name: 'lobby',
          onFirstConnect: onFirstConnect
        }
      });

    cfg.socketSend = lichess.socket.send;
    lobby = LichessLobby(element, cfg);

    var $startButtons = $('#start_buttons');

    var sliderTimes = [
      0, 0.5, 0.75, 1, 1.5, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
      16, 17, 18, 19, 20, 25, 30, 35, 40, 45, 60, 90, 120, 150, 180
    ];

    function sliderTime(v) {
      return v < sliderTimes.length ? sliderTimes[v] : 180;
    }

    function showTime(v) {
      if (v === 1 / 2) return '½';
      if (v === 3 / 4) return '¾';
      return v;
    }

    function sliderIncrement(v) {
      if (v <= 20) return v;
      switch (v) {
        case 21:
          return 25;
        case 22:
          return 30;
        case 23:
          return 35;
        case 24:
          return 40;
        case 25:
          return 45;
        case 26:
          return 60;
        case 27:
          return 90;
        case 28:
          return 120;
        case 29:
          return 150;
        default:
          return 180;
      }
    }

    function sliderDays(v) {
      if (v <= 3) return v;
      switch (v) {
        case 4:
          return 5;
        case 5:
          return 7;
        case 6:
          return 10;
        default:
          return 14;
      }
    }

    function sliderInitVal(v, f, max) {
      for (var i = 0; i < max; i++) {
        if (f(i) === v) return i;
      }
    }

    function prepareForm() {
      var $form = $('.lichess_overboard');
      var $timeModeSelect = $form.find('#timeMode');
      var $modeChoicesWrap = $form.find('.mode_choice');
      var $modeChoices = $modeChoicesWrap.find('input');
      var $casual = $modeChoices.eq(0),
        $rated = $modeChoices.eq(1);
      var $variantSelect = $form.find('#variant');
      var $fenPosition = $form.find(".fen_position");
      var $timeInput = $form.find('.time_choice input');
      var $incrementInput = $form.find('.increment_choice input');
      var $daysInput = $form.find('.days_choice input');
      var isHook = $form.hasClass('game_config_hook');
      var $ratings = $form.find('.ratings > div');
      var randomColorVariants = $form.data('random-color-variants').split(',');
      var toggleButtons = function() {
        var timeMode = $timeModeSelect.val();
        var rated = $rated.prop('checked');
        var timeOk = timeMode != '1' || $timeInput.val() > 0 || $incrementInput.val() > 0;
        var ratedOk = !isHook || !rated || timeMode != '0';
        if (timeOk && ratedOk) {
          $form.find('.color_submits button').toggleClass('nope', false);
          $form.find('.color_submits button:not(.random)').toggle(!rated || randomColorVariants.indexOf($variantSelect.val()) === -1);
        } else
          $form.find('.color_submits button').toggleClass('nope', true);
      };
      var showRating = function() {
        var timeMode = $timeModeSelect.val();
        var key;
        switch ($variantSelect.val()) {
          case '1':
            if (timeMode == '1') {
              var time = $timeInput.val() * 60 + $incrementInput.val() * 40;
              if (time < 180) key = 'bullet';
              else if (time < 480) key = 'blitz';
              else key = 'classical';
            } else key = 'correspondence';
            break;
          case '2':
            key = 'chess960';
            break;
          case '4':
            key = 'kingOfTheHill';
            break;
          case '5':
            key = 'threeCheck';
            break;
          case '6':
            key = 'antichess'
            break;
          case '7':
            key = 'atomic'
            break;
          case '8':
            key = "horde"
            break;
          case '9':
            key = "racingKings"
            break;
        }
        $ratings.hide().filter('.' + key).show();
      };
      if (isHook) {
        var $formTag = $form.find('form');
        if ($form.data('anon')) {
          $timeModeSelect.val(1)
            .children('.timeMode_2, .timeMode_0')
            .prop('disabled', true)
            .attr('title', $.trans('You need an account to do that'));
        }
        var ajaxSubmit = function(color) {
          $.ajax({
            url: $formTag.attr('action').replace(/uid-placeholder/, lichess.StrongSocket.sri),
            data: $formTag.serialize() + "&color=" + color,
            type: 'post'
          });
          $form.find('a.close').click();
          lobby.setTab($timeModeSelect.val() === '1' ? 'real_time' : 'seeks');
          return false;
        };
        $formTag.find('.color_submits button').click(function() {
          return ajaxSubmit($(this).val());
        }).attr('disabled', false);
        $formTag.submit(function() {
          return ajaxSubmit('random');
        });
      } else
        $form.find('form').one('submit', function() {
          $(this).find('.color_submits').find('button').hide().end().append(lichess.spinnerHtml);
        });
      lichess.slider().done(function() {
        $timeInput.add($incrementInput).each(function() {
          var $input = $(this),
            $value = $input.siblings('span');
          var isTimeSlider = $input.parent().hasClass('time_choice');
          $input.hide().after($('<div>').slider({
            value: sliderInitVal(parseFloat($input.val()), isTimeSlider ? sliderTime : sliderIncrement, 100),
            min: 0,
            max: isTimeSlider ? 33 : 30,
            range: 'min',
            step: 1,
            slide: function(event, ui) {
              var time = (isTimeSlider ? sliderTime : sliderIncrement)(ui.value);
              $value.text(isTimeSlider ? showTime(time) : time);
              $input.attr('value', time);
              showRating();
              toggleButtons();
            }
          }));
        });
        $daysInput.each(function() {
          var $input = $(this),
            $value = $input.siblings('span');
          $input.hide().after($('<div>').slider({
            value: sliderInitVal(parseInt($input.val()), sliderDays, 20),
            min: 1,
            max: 7,
            range: 'min',
            step: 1,
            slide: function(event, ui) {
              var days = sliderDays(ui.value);
              $value.text(days);
              $input.attr('value', days);
            }
          }));
        });
        $form.find('.rating_range').each(function() {
          var $this = $(this);
          var $input = $this.find("input");
          var $span = $this.siblings("span.range");
          var min = $input.data("min");
          var max = $input.data("max");
          var values = $input.val() ? $input.val().split("-") : [min, max];

          $span.text(values.join(' - '));
          $this.slider({
            range: true,
            min: min,
            max: max,
            values: values,
            step: 50,
            slide: function(event, ui) {
              $input.val(ui.values[0] + "-" + ui.values[1]);
              $span.text(ui.values[0] + " - " + ui.values[1]);
            }
          });
        });
      });
      $modeChoices.add($form.find('.members_only input')).on('change', function() {
        var rated = $rated.prop('checked');
        var membersOnly = $form.find('.members_only input').prop('checked');
        $form.find('.rating_range_config').toggle(rated || membersOnly);
        $form.find('.members_only').toggle(!rated);
        toggleButtons();
      }).trigger('change');
      $timeModeSelect.on('change', function() {
        var timeMode = $(this).val();
        $form.find('.time_choice, .increment_choice').toggle(timeMode == '1');
        $form.find('.days_choice').toggle(timeMode == '2');
        toggleButtons();
        showRating();
      }).trigger('change');

      var $fenInput = $fenPosition.find('input');
      var validateFen = $.fp.debounce(function() {
        $fenInput.removeClass("success failure");
        var fen = $fenInput.val();
        if (fen) {
          $.ajax({
            url: $fenInput.parent().data('validate-url'),
            data: {
              fen: fen
            },
            success: function(data) {
              $fenInput.addClass("success");
              $fenPosition.find('.preview').html(data);
              $fenPosition.find('a.board_editor').each(function() {
                $(this).attr('href', $(this).attr('href').replace(/editor\/.+$/, "editor/" + fen));
              });
              $form.find('.color_submits button').removeClass('nope');
              $('body').trigger('lichess.content_loaded');
            },
            error: function() {
              $fenInput.addClass("failure");
              $fenPosition.find('.preview').html("");
              $form.find('.color_submits button').addClass('nope');
            }
          });
        }
      }, 200);
      $fenInput.on('keyup', validateFen);

      $variantSelect.on('change', function() {
        var fen = $(this).val() == '3';
        $fenPosition.toggle(fen);
        $modeChoicesWrap.toggle(!fen);
        if (fen) $casual.click();
        showRating();
        toggleButtons();
      }).trigger('change');

      $form.find('div.level').each(function() {
        var $infos = $(this).find('.ai_info > div');
        $(this).find('label').mouseenter(function() {
          $infos.hide().filter('.' + $(this).attr('for')).show();
        });
        $(this).find('#config_level').mouseleave(function() {
          var level = $(this).find('input:checked').val();
          $infos.hide().filter('.level_' + level).show();
        }).trigger('mouseout');
      });

      $form.find('a.close.icon').click(function() {
        $form.remove();
        $startButtons.find('a.active').removeClass('active');
        return false;
      });
    }

    $startButtons.find('a').not('.disabled').click(function() {
      $(this).addClass('active').siblings().removeClass('active');
      $('.lichess_overboard').remove();
      $.ajax({
        url: $(this).attr('href'),
        success: function(html) {
          $('.lichess_overboard').remove();
          $('#hooks_wrap').prepend(html);
          prepareForm();
          $('body').trigger('lichess.content_loaded');
        },
        error: function() {
          lichess.reload();
        }
      });
      return false;
    });

    if (['#ai', '#friend', '#hook'].indexOf(location.hash) !== -1) {
      $startButtons
        .find('a.config_' + location.hash.replace('#', ''))
        .each(function() {
          $(this).attr("href", $(this).attr("href") + location.search);
        }).click();

      if (location.hash === '#hook') {
        if (/time=realTime/.test(location.search))
          lobby.setTab('real_time');
        else if (/time=correspondence/.test(location.search))
          lobby.setTab('seeks');
      }

      window.history.replaceState(null, null, '/');
    }
  };

  ///////////////////
  // tournament.js //
  ///////////////////

  function startTournament(element, cfg) {
    $('body').data('tournament-id', cfg.data.id);
    var $watchers = $("div.watchers").watchers();
    if (typeof lichess_chat !== 'undefined') $('#chat').chat({
      messages: lichess_chat
    });
    var tournament;
    lichess.socket = lichess.StrongSocket(
      '/tournament/' + cfg.data.id + '/socket/v1', cfg.data.socketVersion, {
        receive: function(t, d) {
          tournament.socketReceive(t, d)
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
          $('body').trigger('lichess.content_loaded');
        });
      };
      $('#site_header .help a.more').click(function() {
        $.modal($(this).parent().find('div.more').clone()).addClass('card');
      });
      return;
    }
  });

  function startSimul(element, cfg) {
    $('body').data('simul-id', cfg.data.id);
    var $watchers = $("div.watchers").watchers();
    if (typeof lichess_chat !== 'undefined') $('#chat').chat({
      messages: lichess_chat
    });
    var simul;
    lichess.socket = lichess.StrongSocket(
      '/simul/' + cfg.data.id + '/socket/v1', cfg.socketVersion, {
        receive: function(t, d) {
          simul.socketReceive(t, d)
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
  };

  ////////////////
  // analyse.js //
  ////////////////

  function startAnalyse(element, cfg) {
    var data = cfg.data;
    if (data.chat) $('#chat').chat({
      messages: data.chat,
      initialNote: data.note,
      gameId: data.game.id
    });
    var $watchers = $('#site_header div.watchers').watchers();
    var analyse, $panels;
    lichess.socket = lichess.StrongSocket(
      data.url.socket,
      data.player.version, {
        options: {
          name: "analyse"
        },
        params: {
          ran: "--ranph--",
          userTv: $('.user_tv').data('user-tv')
        },
        receive: function(t, d) {
          analyse.socketReceive(t, d);
        },
        events: {
          analysisProgress: function(d) {
            if (!lichess.advantageChart) startAdvantageChart();
            else if (lichess.advantageChart.update) lichess.advantageChart.update(data);
            if (Object.keys(d.tree.eval).length) $("#adv_chart_loader").remove();
          },
          crowd: function(event) {
            $watchers.watchers("set", event.watchers);
          }
        }
      });

    var $timeChart = $("#movetimes_chart");
    var $inputFen = $('input.fen', element);
    var unselect = function(chart) {
      chart.getSelectedPoints().forEach(function(point) {
        point.select(false);
      });
    };
    var lastFen;
    cfg.onChange = function(fen, path, mainlinePly) {
      if (fen === lastFen) return;
      lastFen = fen = fen || lastFen;
      var chart, point, $chart = $("#adv_chart");
      $inputFen.val(fen);
      if ($chart.length) try {
        chart = $chart.highcharts();
        if (chart) {
          if (mainlinePly === false) unselect(chart);
          else {
            point = chart.series[0].data[mainlinePly - 1 - cfg.data.game.startedAtTurn];
            if (typeof point != "undefined") point.select();
            else unselect(chart);
          }
        }
      } catch (e) {}
      if ($timeChart.length) try {
        chart = $timeChart.highcharts();
        if (chart) {
          if (mainlinePly === false) unselect(chart);
          else {
            var white = mainlinePly % 2 !== 0;
            var serie = white ? 0 : 1;
            var turn = Math.floor((mainlinePly - 1 - cfg.data.game.startedAtTurn) / 2);
            point = chart.series[serie].data[turn];
            if (typeof point != "undefined") point.select();
            else unselect(chart);
          }
        }
      } catch (e) {}
    };
    cfg.initialPly = 'url';
    cfg.element = element.querySelector('.analyse');
    cfg.socketSend = lichess.socket.send;
    analyse = LichessAnalyse(cfg);
    cfg.jumpToIndex = analyse.jumpToIndex;

    $('.underboard_content', element).appendTo($('.underboard .center', element)).show();

    var chartLoader = function() {
      return '<div id="adv_chart_loader">' +
        'Analysis<br>in progress' + lichess.spinnerHtml +
        '</div>'
    };
    var startAdvantageChart = function() {
      if (lichess.advantageChart) return;
      var loading = !data.treeParts[0].eval || !Object.keys(data.treeParts[0].eval).length;
      var $panel = $panels.filter('.computer_analysis');
      if (!$("#adv_chart").length) $panel.html('<div id="adv_chart"></div>' + (loading ? chartLoader() : ''));
      else if (loading && !$("#adv_chart_loader").length) $panel.append(chartLoader());
      lichess.loadScript('/assets/javascripts/chart/advantage.js').then(function() {
        lichess.advantageChart(data);
      });
    };

    $panels = $('div.analysis_panels > div');
    var $menu = $('div.analysis_menu');
    var storageKey = 'lichess.analysis.panel';
    var setPanel = function(panel) {
      $menu.children('.active').removeClass('active').end().find('.' + panel).addClass('active');
      $panels.removeClass('active').filter('.' + panel).addClass('active');
      if (panel === 'move_times' && !lichess.movetimeChart) try {
        lichess.loadScript('/assets/javascripts/chart/movetime.js').then(function() {
          lichess.movetimeChart(data);
        });
      } catch (e) {}
      if (panel === 'computer_analysis' && $("#adv_chart").length) startAdvantageChart();
    };
    $menu.on('click', 'a', function() {
      var panel = $(this).data('panel');
      lichess.storage.set(storageKey, panel);
      setPanel(panel);
    });
    if (cfg.data.analysis) setPanel('computer_analysis');
    else {
      var stored = lichess.storage.get(storageKey);
      if (stored && $menu.children('.' + stored).length) setPanel(stored);
      else if ($menu.children('.crosstable').length) $menu.children('.crosstable').click();
      else $menu.children(':first').click();

      $panels.find('form.future_game_analysis').submit(function() {
        if ($(this).hasClass('must_login')) {
          if (confirm($.trans('You need an account to do that'))) location.href = '/signup';
          return false;
        }
        $.ajax({
          method: 'post',
          url: $(this).attr('action'),
          success: startAdvantageChart,
          error: lichess.reload
        });
        return false;
      });
    }

    $panels.find('div.pgn').click(function() {
      var range, selection;
      if (document.body.createTextRange) {
        range = document.body.createTextRange();
        range.moveToElementText($(this)[0]);
        range.select();
      } else if (window.getSelection) {
        selection = window.getSelection();
        range = document.createRange();
        range.selectNodeContents($(this)[0]);
        selection.removeAllRanges();
        selection.addRange(range);
      }
    });
    topMenuIntent();
  }

  ////////////////
  // user_analysis.js //
  ////////////////

  function startUserAnalysis(element, cfg) {
    var analyse;
    cfg.initialPly = 'url';
    cfg.element = element.querySelector('.analyse');
    lichess.socket = lichess.StrongSocket('/socket', 0, {
      options: {
        name: "analyse"
      },
      params: {
        ran: "--ranph--"
      },
      receive: function(t, d) {
        analyse.socketReceive(t, d);
      }
    });
    cfg.socketSend = lichess.socket.send;
    analyse = LichessAnalyse(cfg);
    topMenuIntent();
  }

  ////////////////
  // study.js //
  ////////////////

  function startStudy(element, cfg) {
    var $chat = $('#chat').chat({
      messages: cfg.chat,
      gameId: cfg.study.id
    });
    cfg.chat = {
      writeable: function(v) {
        $chat.chat('writeable', v);
      }
    };
    var $watchers = $("div.watchers").watchers();
    var analyse;
    cfg.element = element.querySelector('.analyse');
    cfg.sideElement = document.querySelector('#site_header .side_box');
    lichess.socket = lichess.StrongSocket(cfg.socketUrl, cfg.socketVersion, {
      options: {
        name: "study"
      },
      params: {
        ran: "--ranph--"
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
    analyse = LichessAnalyse(cfg);
    topMenuIntent();
  }

  /////////////// forum.js ////////////////////

  $('#lichess_forum').on('click', 'a.delete', function() {
    $.post($(this).attr("href"));
    $(this).closest(".post").slideUp(100);
    return false;
  }).on('click', 'form.unsub button', function() {
    var $form = $(this).parent().toggleClass('on off');
    $.post($form.attr("action") + '?unsub=' + $(this).data('unsub'));
    return false;
  });

  $.idleTimer = function(delay, onIdle, onWakeUp) {
    var eventType = 'mousemove';
    var listening = false;
    var active = true;
    var lastSeenActive = new Date();
    var onActivity = function() {
      if (!active) onWakeUp();
      active = true;
      lastSeenActive = new Date();
      stopListening();
    };
    var startListening = function() {
      if (!listening) {
        document.addEventListener(eventType, onActivity);
        listening = true;
      }
    };
    var stopListening = function() {
      if (listening) {
        document.removeEventListener(eventType, onActivity);
        listening = false;
      }
    };
    setInterval(function() {
      if (active && new Date() - lastSeenActive > delay) {
        onIdle();
        active = false;
      }
      startListening();
    }, 5000);
  };

  $.modal = function(html) {
    if (!html.clone) html = $('<div>' + html + '</div>');
    var $wrap = $('<div id="modal-wrap">').html(html.clone().show()).prepend('<a class="close" data-icon="L"></a>');
    var $overlay = $('<div id="modal-overlay">').html($wrap);
    $overlay.add($wrap.find('.close')).one('click', $.modal.close);
    $wrap.click(function(e) {
      e.stopPropagation();
    });
    $('body').prepend($overlay);
    return $wrap;
  };
  $.modal.close = function() {
    $('#modal-overlay').remove();
  };
})();
