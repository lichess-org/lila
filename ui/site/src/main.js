
lichess.topMenuIntent = function() {
  $('#topmenu.hover').removeClass('hover').hoverIntent(function() {
    $(this).toggleClass('hover');
  });
};

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
    return u ? '<a class="user_link ulpt ' + (klass || '') + '" href="/@/' + id + '">' + (limit ? u.substring(0, limit) : u) + '</a>' : 'Anonymous';
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
  lichess.fp = {};
  lichess.fp.contains = function(list, needle) {
    return list.indexOf(needle) !== -1;
  };
  lichess.fp.debounce = function(func, wait, immediate) {
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
        $('#friend_box').friends("set", all.d, all.playing, all.studying, all.patrons);
      },
      following_enters: function(d, all) {
        $('#friend_box').friends('enters', all.d, all.playing, all.studying, all.patron);
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
      following_joined_study: function(name) {
        $('#friend_box').friends('study_join', name);
      },
      following_left_study: function(name) {
        $('#friend_box').friends('study_leave', name);
      },
      new_notification: function(e) {
        $('#site_notifications_tag').attr('data-count', e.unread || 0);
        lichess.sound.newPM();
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
          '<a class="withdraw text" href="' + url + '/withdraw" data-icon="Z">Pause</a>' +
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
    lichess.loadScript('/assets/javascripts/vendor/typeahead.jquery.min.js', {noVersion:true}).done(function() {
      $input.typeahead(null, {
        minLength: 3,
        hint: true,
        highlight: false,
        source: function(query, sync, runAsync) {
          $.ajax({
            method: 'get',
            url: '/player/autocomplete',
            data: {
              term: query,
              friend: opts.friend ? 1 : 0
            },
            success: function(res) {
              // hack to fix typeahead limit bug
              if (res.length === 10) {
                res.push(null);
              }
              runAsync(res);
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
      }).on('typeahead:render', function() {
        lichess.pubsub.emit('content_loaded')();
      });
      if (opts.focus) $input.focus();
      if (opts.onSelect) $input.on('typeahead:select', function(ev, sel) {
        opts.onSelect(sel);
      }).keypress(function(e) {
        if (e.which == 10 || e.which == 13) opts.onSelect($(this).val());
      });
    });
  };

  lichess.parseFen = (function() {
    var doParseFen = function($elem) {
      if (!window.Chessground) return $(window).on('load', function() {
        doParseFen($elem);
      });
      if (!$elem || !$elem.jquery) {
        $elem = $('.parse_fen');
      }
      $elem.each(function() {
        var $this = $(this).removeClass('parse_fen');
        var lm = $this.data('lastmove');
        var lastMove = lm && [lm[0] + lm[1], lm[2] + lm[3]];
        var color = $this.data('color') || lichess.readServerFen($(this).data('y'));
        var ground = $this.data('chessground');
        var playable = !!$this.data('playable');
        var resizable = !!$this.data('resizable');
        var config = {
          coordinates: false,
          viewOnly: !playable,
          resizable: resizable,
          fen: $this.data('fen') || lichess.readServerFen($this.data('z')),
          lastMove: lastMove,
          drawable: { enabled: false }
        };
        if (color) config.orientation = color;
        if (ground) ground.set(config);
        else {
          this.innerHTML = '<div class="cg-board-wrap">';
          $this.data('chessground', Chessground(this.firstChild, config));
        }
      });
    };
    // debounce the first parseFen at first, then process them immediately
    // because chessground initial display does a DOM read (board dimensions)
    // and the play page can have 6 miniboards to display (ongoing games)
    if (document.getElementById('now_playing')) {
      var fun = lichess.fp.debounce(doParseFen, 400, false);
      setTimeout(function() {
        fun = doParseFen;
      }, 700);
      return function($elem) {
        fun($elem);
      };
    } else return doParseFen;
  })();

  $(function() {
    if (lichess.analyse) LichessAnalyse.boot(document.getElementById('lichess'), lichess.analyse);
    else if (lichess.user_analysis) startUserAnalysis(document.getElementById('lichess'), lichess.user_analysis);
    else if (lichess.study) startStudy(document.getElementById('lichess'), lichess.study);
    else if (lichess.practice) startPractice(document.getElementById('lichess'), lichess.practice);
    else if (lichess.puzzle) startPuzzle(lichess.puzzle);
    else if (lichess.tournament) startTournament(document.getElementById('tournament'), lichess.tournament);
    else if (lichess.simul) startSimul(document.getElementById('simul'), lichess.simul);

    // delay so round starts first (just for perceived perf)
    lichess.requestIdleCallback(function() {

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
        // check that locale was loaded
        if (!window.momentLocaleUrl) lichess.requestIdleCallback(function() {
          $(".moment-from-now").each(function() {
            this.textContent = moment(this.getAttribute('datetime')).fromNow();
          });
          $("time.moment").removeClass('moment').each(function() {
            var parsed = moment(this.getAttribute('datetime'));
            var format = this.getAttribute('data-format');
            this.textContent = format === 'calendar' ? parsed.calendar(null, {
              sameElse: 'DD/MM/YYYY HH:mm'
            }) : parsed.format(format);
          });
        });
      }

      if (window.momentLocaleUrl) lichess.loadScript(momentLocaleUrl, {noVersion: true}).then(function() {
        delete window.momentLocaleUrl;
        lichess.pubsub.emit('moment.locale_loaded')();
        setMoment();
      });
      else setMoment();

      lichess.pubsub.on('content_loaded', setMoment);
      setInterval(setMoment, 2000);

      if ($('body').hasClass('blind_mode')) {
        var setBlindMode = function() {
          $('[data-hint]').each(function() {
            $(this).attr('aria-label', $(this).data('hint'));
          });
        };
        setBlindMode();
        lichess.pubsub.on('content_loaded', setBlindMode);
      }

      if (!window.customWS) setTimeout(function() {
        if (lichess.socket === null) lichess.socket = lichess.StrongSocket("/socket", false);
      }, 300);

      lichess.challengeApp = (function() {
        var instance, booted;
        var $toggle = $('#challenge_notifications_tag');
        $toggle.one('mouseover click', function() {
          load();
        });
        var load = function(data) {
          if (booted) return;
          booted = true;
          var $el = $('#challenge_app').html(lichess.initiatingHtml);
          var isDev = $('body').data('dev');
          lichess.loadCss('/assets/stylesheets/challengeApp.css');
          lichess.loadScript("/assets/compiled/lichess.challenge" + (isDev ? '' : '.min') + '.js').done(function() {
            instance = LichessChallenge.default($el[0], {
              data: data,
              show: function() {
                if (!$('#challenge_app').is(':visible')) $toggle.click();
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

      lichess.notifyApp = (function() {
        var instance, booted;
        var $toggle = $('#site_notifications_tag');
        var isVisible = function() {
          return $('#notify_app').is(':visible');
        };

        var load = function(data, incoming) {
          if (booted) return;
          booted = true;
          var $el = $('#notify_app').html(lichess.initiatingHtml);
          var isDev = $('body').data('dev');
          lichess.loadCss('/assets/stylesheets/notifyApp.css');
          lichess.loadScript("/assets/compiled/lichess.notify" + (isDev ? '' : '.min') + '.js').done(function() {
            instance = LichessNotify.default($el.empty()[0], {
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

        $toggle.one('mouseover click', function() {
          load();
        }).click(function() {
          setTimeout(function() {
            if (instance && isVisible()) instance.setVisible();
          }, 200);
        });

        return {
          update: function(data, incoming) {
            if (!instance) load(data, incoming);
            else instance.update(data, incoming);
          }
        };
      })();

      // Zoom
      var currentZoom = (!lichess.isTrident && $('body').data('zoom') / 100) || 1;

      var setZoom = function(zoom) {

        currentZoom = zoom;

        var $lichessGame = $('.lichess_game, .board_and_ground');
        var $boardWrap = $lichessGame.find('.cg-board-wrap').not('.mini_board .cg-board-wrap');
        var px = function(i) {
          return Math.round(i) + 'px';
        };

        $('.underboard').css("width", px(512 * zoom + 242 + 15));
        $boardWrap.add($('.underboard .center, .progress_bar_container')).css("width", px(512 * zoom));

        if ($('body > .content').hasClass('is3d')) {
          $boardWrap.css("height", px(464.5 * zoom));
          $lichessGame.css({
            height: px(476 * zoom),
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
      lichess.pubsub.on('reset_zoom', function() {
        if (currentZoom > 1 || $('body').data('zoom') > 100) setZoom(currentZoom);
      });

      // dasher
      (function() {
        var booted;
        $('#top .dasher .toggle').one('mouseover click', function() {
          if (booted) return;
          booted = true;
          var $el = $('#dasher_app').html(lichess.initiatingHtml);
          var isDev = $('body').data('dev');
          lichess.loadCss('/assets/stylesheets/dasherApp.css');
          lichess.loadScript("/assets/compiled/lichess.dasher" + (isDev ? '' : '.min') + '.js').done(function() {
            instance = LichessDasher.default($el.empty()[0], {
              setZoom: setZoom,
              playing: $el.data('playing')
            });
          });
        });
      })();

      function translateTexts() {
        $('.trans_me').each(function() {
          var t = $(this).removeClass('trans_me');
          if (t.val()) t.val(lichess.globalTrans(t.val()));
          else t.text(lichess.globalTrans(t.text()));
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

      $('.infinitescroll').each(function() {
        if (!$('.pager a', this).length) return;
        var $scroller = $(this).infinitescroll({
          navSelector: ".pager",
          nextSelector: ".pager a",
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
        }).find('div.pager').hide().end();
        $scroller.parent().append($('<button class="inf-more">More</button>').on('click', function() {
          $scroller.infinitescroll('retrieve');
        }));
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
      $('#topmenu').on('click', 'section > a', function() {
        return false;
      });

      $('#ham-plate').one('mouseover click', function() {
        if (!$('#fpmenu').length) {
          $('body').append($('<div id=fpmenu>').load('/fpmenu', function() {
            $(this)
              .find('form[action="/login"]')
              .attr('action', '/login?referrer=' + window.location.pathname);
          }));
        }
        lichess.loadCss('/assets/stylesheets/fpmenu.css');
      }).click(function() {
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

      if (window.Fingerprint2) setTimeout(function() {
        var t = Date.now()
          new Fingerprint2({
            excludeJsFonts: true
          }).get(function(res) {
            var time = Date.now() - t;
            $.post('/set-fingerprint/' + res + '/' + time);
          });
      }, 500);
    });
  });

  lichess.sound = (function() {
    var api = {};
    var soundSet = $('body').data('sound-set');

    api.volumeStorage = lichess.storage.make('sound-volume');
    api.defaultVolume = 0.7;

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
        if (lichess.fp.contains(['move', 'capture', 'check'], k)) return {
          play: $.noop
        };
        set = 'standard';
      }
      var baseUrl = lichess.assetUrl('/assets/sound', {noVersion:true});
      return new Howl({
        src: ['ogg', 'mp3'].map(function(ext) {
          return [baseUrl, set, names[k] + '.' + ext].join('/');
        }),
        volume: volumes[k] || 1
      });
    });
    var enabled = function() {
      return soundSet !== 'silent';
    };
    Object.keys(names).forEach(function(name) {
      api[name] = function() {
        if (!enabled()) return;
        Howler.volume(api.volumeStorage.get() || api.defaultVolume);
        collection(name).play();
      }
    });
    api.load = function(name) {
      if (enabled() && name in names) collection(name);
    };
    api.setVolume = function(v) {
      api.volumeStorage.set(v);
      Howler.volume(v);
    };

    var publish = function() {
      lichess.pubsub.emit('sound_set')(soundSet);
    };
    setTimeout(publish, 500);

    api.changeSet = function(s) {
      soundSet = s;
      collection.clear();
      publish();
    };

    api.warmup = function() {
      if (enabled()) {
        // See goldfire/howler.js#715
        Howler._autoResume();   // This resumes sound if suspended.
        Howler._autoSuspend();  // This starts the 30s timer to suspend.
      }
    };

    api.set = function() {
      return soundSet;
    };
    return api;
  })();

  lichess.globalTrans = function() {
    var str = window.lichess_translations && lichess_translations[arguments[0]];
    if (!str) return arguments[0];
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
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
      var id = lichess.fp.contains(user.name, ' ') ? user.name.split(' ')[1] : user.name;
      return id.toLowerCase() === userId;
    };
    return {
      _create: function() {
        var self = this;
        var hideStorage = lichess.storage.make('friends-hide');
        self.$list = self.element.find("div.list");
        var $title = self.element.find('.title').click(function() {
          var show = hideStorage.get() == 1;
          self.element.find('.content_wrap').toggleNone(show);
          if (show) hideStorage.remove();
          else hideStorage.set(1);
        });
        if (hideStorage.get() == 1) self.element.find('.content_wrap').addClass('none');
        self.$nbOnline = $title.find('.online');
        self.$nobody = self.element.find(".nobody");

        var users = self.element.data('preload').split(',');
        var playings = self.element.data('playing').split(',');
        var studyings = self.element.data('studying').split(',');
        var patrons = self.element.data('patrons').split(',');
        self.set(users, playings, studyings, patrons);
      },
      _findByUsername: function(n) {
        return this.users.filter(function(u) {
          return isSameUser(n.toLowerCase(), u);
        })[0];
      },
      _makeUser: function(name, playing, studying, patron) {
        return {
          'name': name,
          'playing': !!playing,
          'studying': !!studying,
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
          this.$nobody.toggleNone(!this.users.length);
          this.$list.html(this.users.sort(function(a, b) {
            return a.name.toLowerCase() < b.name.toLowerCase() ? -1 : 1;
          }).map(this._renderUser).join(""));
        }.bind(this));
      },
      set: function(us, playings, studyings, patrons) {
        this.users = us.map(function(user) {
          return this._makeUser(user, false, false, false);
        }.bind(this));
        for (i in playings) this._setPlaying(playings[i], true);
        for (i in studyings) this._setStudying(studyings[i], true);
        for (i in patrons) this._setPatron(patrons[i], true);
        this.repaint();
      },
      enters: function(userName, playing, studying, patron) {
        var user = this._makeUser(userName, playing, studying, patron);
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
      _setStudying: function(userName, studying) {
        var user = this._findByUsername(userName);
        if (user) user.studying = studying;
      },
      playing: function(userName) {
        this._setPlaying(userName, true);
        this.repaint();
      },
      stopped_playing: function(userName) {
        this._setPlaying(userName, false);
        this.repaint();
      },
      study_join: function(userName) {
        this._setStudying(userName, true);
        this.repaint();
      },
      study_leave: function(userName) {
        this._setStudying(userName, false);
        this.repaint();
      },
      _renderUser: function(user) {
        var icon = '<i class="is-green line' + (user.patron ? ' patron' : '') + '"></i>';
        var name = lichess.fp.contains(user.name, ' ') ? user.name.split(' ')[1] : user.name;
        var url = '/@/' + name;
        var tvButton = user.playing ? '<a data-icon="1" class="tv is-green ulpt" data-pt-pos="nw" href="' + url + '/tv" data-href="' + url + '"></a>' : '';
        var studyButton = user.studying ? '<a data-icon="&#xe00e;" class="is-green friend-study" href="' + url + '/studyTv"></a>' : '';
        var rightButton = tvButton || studyButton;

        return '<div><a class="user_link ulpt" data-pt-pos="nw" href="' + url + '">' + icon + user.name + '</a>' + rightButton + '</div>';
      }
    };
  })());

  lichess.widget("clock", {
    _create: function() {
      var self = this;
      // this.options.time: seconds Integer
      var target = this.options.time * 1000 + Date.now();
      var timeEl = this.element.find('.time')[0];
      var tick = function() {
        var remaining = target - Date.now();
        if (remaining <= 0) clearInterval(self.interval);
        timeEl.innerHTML = self._formatMs(remaining);
      };
      this.interval = setInterval(tick, 1000);
      tick();
    },

    _pad: function(x) { return (x < 10 ? '0' : '') + x; },

    _formatMs: function(msTime) {
      var date = new Date(Math.max(0, msTime + 500));

      var hours = date.getUTCHours(),
        minutes = date.getUTCMinutes(),
        seconds = date.getUTCSeconds();

      if (hours > 0) {
        return hours + ':' + this._pad(minutes) + ':' + this._pad(seconds);
      } else {
        return minutes + ':' + this._pad(seconds);
      }
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

    lichess.requestIdleCallback(function() {
      lichess.parseFen();
      setTimeout(function() {
        $('div.checkmateCaptcha').each(function() {
          var $captcha = $(this);
          var $board = $captcha.find('.mini_board');
          var $input = $captcha.find('input').val('');
          var cg = $board.data('chessground');
          var dests = JSON.parse(lichess.readServerFen($board.data('x')));
          for (var k in dests) dests[k] = dests[k].match(/.{2}/g);
          var config = {
            turnColor: cg.state.orientation,
            movable: {
              free: false,
              dests: dests,
              color: cg.state.orientation,
              events: {
                after: function(orig, dest) {
                  $captcha.removeClass("success failure");
                  submit(orig + ' ' + dest);
                }
              }
            }
          };
          cg.set(config);

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
                    turnColor: cg.state.orientation,
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
    });
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
