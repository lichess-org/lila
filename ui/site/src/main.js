lichess.topnavIntent = function() {
  $('#topnav.hover').removeClass('hover').hoverIntent(function() {
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
  $.ajaxTransport('script', function(s) {
    // Monkeypatch jQuery to load scripts with nonce. Upstream patch:
    // - https://github.com/jquery/jquery/pull/3766
    // - https://github.com/jquery/jquery/pull/3782
    // Original transport:
    // https://github.com/jquery/jquery/blob/master/src/ajax/script.js
    var script, callback;
    return {
      send: function(_, complete) {
        script = $("<script>").prop({
          nonce: document.body.getAttribute('data-nonce'), // Add the nonce!
          charset: s.scriptCharset,
          src: s.url
        }).on("load error", callback = function(evt) {
          script.remove();
          callback = null;
          if (evt) {
            complete(evt.type === "error" ? 404 : 200, evt.type);
          }
        });
        document.head.appendChild(script[0]);
      },
      abort: function() {
        if (callback) {
          callback();
        }
      }
    };
  });
  $.userLink = function(u) {
    return $.userLinkLimit(u, false);
  };
  $.userLinkLimit = function(u, limit, klass) {
    var split = u.split(' ');
    var id = split.length == 1 ? split[0] : split[1];
    return u ? '<a class="user_link ulpt ' + (klass || '') + '" href="/@/' + id + '">' + (limit ? u.substring(0, limit) : u) + '</a>' : 'Anonymous';
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
          lichess.redirect(o);
        }, 200);
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
          }, $('body').hasClass('playing') ? 9000 : 15000);
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
          '</div></div></div>'
        ).find("a.withdraw").click(function() {
          $.post($(this).attr("href"));
          $('#tournament_reminder').remove();
          return false;
        });
      }
    },
    params: {},
    options: {
      name: "site",
      lagTag: null,
      isAuth: !!$('body').data('user')
    }
  });

  lichess.readServerFen = function(t) {
    return atob(t.split("").reverse().join(""));
  };

  lichess.userAutocomplete = function($input, opts) {
    opts = opts || {};
    lichess.loadCss('stylesheets/autocomplete.css');
    return lichess.loadScript('javascripts/vendor/typeahead.jquery.min.js', {noVersion:true}).done(function() {
      $input.typeahead(null, {
        minLength: opts.minLength || 3,
        hint: true,
        highlight: false,
        source: function(query, _, runAsync) {
          $.ajax({
            url: '/player/autocomplete',
            cache: true,
            data: {
              term: query,
              friend: opts.friend ? 1 : 0,
              tour: opts.tour,
              object: 1
            },
            success: function(res) {
              res = res.result;
              // hack to fix typeahead limit bug
              if (res.length === 10) res.push(null);
              runAsync(res);
            }
          });
        },
        limit: 10,
        displayKey: 'name',
        templates: {
          empty: '<div class="empty">No player found</div>',
          pending: lichess.spinnerHtml,
          suggestion: function(o) {
            var tag = opts.tag || 'a';
            return '<' + tag + ' class="ulpt user_link' + (o.online ? ' online' : '') + '" ' + (tag === 'a' ? '' : 'data-') + 'href="/@/' + o.name + '">' +
              '<i class="line' + (o.patron ? ' patron' : '') + '"></i>' + (o.title ? '<span class="title">' + o.title + '</span>&nbsp;' : '')  + o.name +
              '</' + tag + '>';
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

  lichess.parseFen = function($elem) {
    if (!window.Chessground) return setTimeout(function() {
      lichess.parseFen($elem);
    }, 500); // if not loaded yet
    if (!$elem || !$elem.jquery) {
      $elem = $('.parse-fen');
    }
    $elem.each(function() {
      var $this = $(this).removeClass('parse-fen');
      var lm = $this.data('lastmove');
      var lastMove = lm && (lm[1] === '@' ? [lm.slice(2)] : [lm[0] + lm[1], lm[2] + lm[3]]);
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
        drawable: { enabled: false, visible: false }
      };
      if (color) config.orientation = color;
      if (ground) ground.set(config);
      else $this.data('chessground', Chessground(this, config));
    });
  };

  $(function() {
    if (lichess.analyse) LichessAnalyse.boot(lichess.analyse);
    else if (lichess.user_analysis) startUserAnalysis(lichess.user_analysis);
    else if (lichess.study) startStudy(document.getElementById('lichess'), lichess.study);
    else if (lichess.practice) startPractice(document.getElementById('lichess'), lichess.practice);
    else if (lichess.relay) startRelay(document.getElementById('lichess'), lichess.relay);
    else if (lichess.puzzle) startPuzzle(lichess.puzzle);
    else if (lichess.tournament) startTournament(lichess.tournament);
    else if (lichess.simul) startSimul(lichess.simul);

    // delay so round starts first (just for perceived perf)
    lichess.requestIdleCallback(function() {

      $('#reconnecting').on('click', function() {
        window.location.reload();
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
          } else throw '';
          $(this).attr('data-icon', 'E');
        } catch (e) {
          usePrompt();
        }
      });

      $('body').on('click', 'a.relation-button', function() {
        var $a = $(this).addClass('processing').css('opacity', 0.3);
        $.ajax({
          url: $a.attr('href'),
          type: 'post',
          success: function(html) {
            if (html.includes('relation-actions')) $a.parent().replaceWith(html);
            else $a.replaceWith(html);
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

      function renderTimeago() {
        lichess.raf(function() {
          lichess.timeago.render([].slice.call(document.getElementsByClassName('timeago'), 0, 99));
        });
      }
      function setTimeago(interval) {
        renderTimeago();
        setTimeout(function() { setTimeago(interval * 1.1); }, interval);
      }
      setTimeago(1200);
      lichess.pubsub.on('content_loaded', renderTimeago);

      if (!window.customWS) setTimeout(function() {
        if (lichess.socket === null) lichess.socket = lichess.StrongSocket("/socket/v4", false);
      }, 300);

      var initiatingHtml = '<div class="initiating">' + lichess.spinnerHtml + '</div>';

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
          lichess.loadCss(lichess.cssPath('challengeApp', 'challenge'));
          lichess.loadScript(lichess.compiledScript('challenge')).done(function() {
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
          var $el = $('#notify_app').html(initiatingHtml);
          var isDev = $('body').data('dev');
          lichess.loadCss(lichess.cssPath('notifyApp', 'notify'));
          lichess.loadScript(lichess.compiledScript('notify')).done(function() {
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
      var currentZoom = $('body').data('zoom') / 100;

      var setZoom = function(zoom) {
        document.body.setAttribute('style', '--zoom:' + Math.round(100 * (zoom - 1)));
        lichess.dispatchEvent(window, 'resize');
      };
      lichess.pubsub.on('reset_zoom', function() {
        if (currentZoom > 1 || $('body').data('zoom') > 100) setZoom(currentZoom);
      });
      window.addEventListener('resize', () => lichess.dispatchEvent(document.body, 'chessground.resize'));

      // dasher
      (function() {
        var booted;
        $('#top .dasher .toggle').one('mouseover click', function() {
          if (booted) return;
          booted = true;
          var $el = $('#dasher_app').html(initiatingHtml);
          var isDev = $('body').data('dev');
          var isPlaying = $('body').hasClass('playing');
          lichess.loadCss(lichess.cssPath('dasherApp', 'dasher'));
          lichess.loadScript(lichess.compiledScript('dasher')).done(function() {
            instance = LichessDasher.default($el.empty()[0], {
              setZoom: setZoom,
              playing: isPlaying
            });
          });
        });
      })();

      // cli
      (function() {
        var $wrap = $('#clinput');
        if (!$wrap.length) return;
        var booted;
        var boot = function() {
          if (booted) return;
          booted = true;
          lichess.loadCss('stylesheets/cli.css');
          lichess.loadScript(lichess.compiledScript('cli')).done(function() {
            LichessCli.app($wrap, toggle);
          });
        }
        var toggle = function() {
          boot();
          $wrap.toggleClass('shown');
          if ($wrap.hasClass('shown')) $wrap.find('input').focus();
        };
        $wrap.children('a').on('mouseover click', function(e) {
          (e.type === 'mouseover' ? boot : toggle)();
        });
        Mousetrap.bind('s', function() {
          setTimeout(toggle, 100);
        });
      })();

      $('input.user-autocomplete').each(function() {
        var opts = {
          focus: 1,
          friend: $(this).data('friend'),
          tag: $(this).data('tag')
        };
        if ($(this).attr('autofocus')) lichess.userAutocomplete($(this), opts);
        else $(this).one('focus', function() {
          lichess.userAutocomplete($(this), opts);
        });
      });

      $('#topnav-toggle').on('change', e => {
        document.body.classList.toggle('masked', e.target.checked);
      });

      lichess.loadInfiniteScroll = function(el) {
        $(el).each(function() {
          if (!$('.pager a', this).length) return;
          var $scroller = $(this).infinitescroll({
            navSelector: ".pager",
            nextSelector: ".pager a",
            itemSelector: ".infinitescroll .paginated",
            errorCallback: function() {
              $("#infscr-loading").remove();
            },
            loading: {
              msg: $('<div id="infscr-loading">').html(lichess.spinnerHtml)
            }
          }, function() {
            $("#infscr-loading").remove();
            lichess.pubsub.emit('content_loaded')();
            var ids = [];
            $(el).find('.paginated[data-dedup]').each(function() {
              var id = $(this).data('dedup');
              if (id) {
                if (ids.includes(id)) $(this).remove();
                else ids.push(id);
              }
            });
          }).find('div.pager').hide().end();
          $scroller.parent().append($('<button class="inf-more">More</button>').on('click', function() {
            $scroller.infinitescroll('retrieve');
          }));
        });
      }
      lichess.loadInfiniteScroll('.infinitescroll');

      $('#top').on('click', 'a.toggle', function() {
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
        return confirm($(this).attr('title') || 'Confirm this action?');
      });

      $('#main-wrap').on('click', 'a.bookmark', function() {
        var t = $(this).toggleClass("bookmarked");
        $.post(t.attr("href"));
        var count = (parseInt(t.text(), 10) || 0) + (t.hasClass("bookmarked") ? 1 : -1);
        t.find('span').html(count > 0 ? count : "");
        return false;
      });

      // minimal touchscreen support for topnav
      if ('ontouchstart' in window) $('#topnav').on('click', 'section > a', function() {
        return false;
      });

      // still bind esc even in form fields
      Mousetrap.prototype.stopCallback = function(e, el, combo) {
        return combo !== 'esc' && (el.tagName === 'INPUT' || el.tagName === 'SELECT' || el.tagName === 'TEXTAREA');
      };
      Mousetrap.bind('esc', function() {
        var $oc = $('#modal-wrap .close');
        if ($oc[0]) $oc[0].click();
        else {
          $input = $(':focus');
          if ($input.length) $input.blur();
        }
        return false;
      });

      if (!lichess.storage.get('grid')) setTimeout(function() {
        if (getComputedStyle(document.body).getPropertyValue('--grid'))
          lichess.storage.set('grid', 1);
        else
          $.get(lichess.assetUrl('oops/browser.html'), html => $('body').prepend(html))
      }, 3000);

      if (window.Fingerprint2) setTimeout(function() {
        var t = Date.now()
        new Fingerprint2({
          excludeJsFonts: true
        }).get(function(res) {
          $i = $('#signup-fp-input');
          if ($i.length) $i.val(res);
          else $.post('/auth/set-fp/' + res + '/' + (Date.now() - t));
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
        if (['move', 'capture', 'check'].includes(k)) return {
          play: $.noop
        };
        set = 'standard';
      }
      var baseUrl = lichess.assetUrl('sound', {noVersion: true});
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

  lichess.widget("watchers", {
    _create: function() {
      this.list = this.element.find(".list");
      this.number = this.element.find(".number");
      lichess.pubsub.on('socket.in.crowd', data => this.set(data.watchers || data));
    },
    set: function(data) {
      if (!data) return this.element.addClass('none');
      if (this.number.length) this.number.text(data.nb);
      if (data.users) {
        var tags = data.users.map($.userLink);
        if (data.anons === 1) tags.push('Anonymous');
        else if (data.anons) tags.push('Anonymous(' + data.anons + ')');
        this.list.html(tags.join(', '));
      } else if (!this.number.length) this.list.html(data.nb + ' players in the chat');
      this.element.removeClass('none');
    }
  });

  lichess.widget("friends", (function() {
    var getId = function(titleName) {
      return titleName.toLowerCase().replace(/^\w+\s/, '');
    };
    var makeUser = function(titleName) {
      var split = titleName.split(' ');
      return {
        id: split[split.length - 1].toLowerCase(),
        name: split[split.length - 1],
        title: (split.length > 1) ? split[0] : undefined,
        playing: false,
        studying: false,
        patron: false
      };
    };
    var renderUser = function(user) {
      var icon = '<i class="is-green line' + (user.patron ? ' patron' : '') + '"></i>';
      var titleTag = user.title ? ('<span class="title"' + (user.title === 'BOT' ? ' data-bot' : '') + '>' + user.title + '</span>&nbsp;') : '';
      var url = '/@/' + user.name;
      var tvButton = user.playing ? '<a data-icon="1" class="tv is-green ulpt" data-pt-pos="nw" href="' + url + '/tv" data-href="' + url + '"></a>' : '';
      var studyButton = user.studying ? '<a data-icon="4" class="is-green friend-study" href="' + url + '/studyTv"></a>' : '';
      var rightButton = tvButton || studyButton;
      return '<div><a class="user_link ulpt" data-pt-pos="nw" href="' + url + '">' + icon + titleTag + user.name + '</a>' + rightButton + '</div>';
    };
    return {
      _create: function() {
        var self = this;
        var el = self.element;

        var hideStorage = lichess.storage.make('friends-hide');
        var $friendBoxTitle = el.find('.friend_box_title').click(function() {
          var show = hideStorage.get() == 1;
          el.find('.content_wrap').toggleNone(show);
          if (show) hideStorage.remove();
          else hideStorage.set(1);
        });
        if (hideStorage.get() == 1) el.find('.content_wrap').addClass('none');

        self.$nbOnline = $friendBoxTitle.find('.online');
        self.$nobody = el.find(".nobody");

        function dataList(name) { return el.data(name) ? el.data(name).split(',') : []; }
        self.set(
          dataList('preload'),
          dataList('playing'),
          dataList('studying'),
          dataList('patrons'));
      },
      repaint: function() {
        lichess.raf(function() {
          var ids = Object.keys(this.users).sort();
          this.$nbOnline.text(ids.length);
          this.$nobody.toggleNone(!ids.length);
          this.element.find('div.list').replaceWith(
            $('<div class="content list"></div>').append(ids.map(function(id) {
              return renderUser(this.users[id]);
            }.bind(this)))
          );
        }.bind(this));
      },
      insert: function(titleName) {
        var id = getId(titleName);
        if (!this.users[id]) this.users[id] = makeUser(titleName);
        return this.users[id];
      },
      set: function(online, playing, studying, patrons) {
        this.users = {};
        for (i in online) this.insert(online[i]);
        for (i in playing) this.insert(playing[i]).playing = true;
        for (i in studying) this.insert(studying[i]).studying = true;
        for (i in patrons) this.insert(patrons[i]).patron = true;
        this.repaint();
      },
      enters: function(titleName, playing, studying, patron) {
        var user = this.insert(titleName);
        user.playing = playing;
        user.studying = studying;
        user.patron = patron;
        this.repaint();
      },
      leaves: function(titleName) {
        delete this.users[getId(titleName)];
        this.repaint();
      },
      playing: function(titleName) {
        this.insert(titleName).playing = true;
        this.repaint();
      },
      stopped_playing: function(titleName) {
        this.insert(titleName).playing = false;
        this.repaint();
      },
      study_join: function(titleName) {
        this.insert(titleName).studying = true;
        this.repaint();
      },
      study_leave: function(titleName) {
        this.insert(titleName).studying = false;
        this.repaint();
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
      $('.mini-board.live').removeClass("live").each(function() {
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
      $('.chat__members').watchers();
      $('div.captcha').each(function() {
        var $captcha = $(this);
        var $board = $captcha.find('.mini-board');
        var $input = $captcha.find('input').val('');
        var cg = $board.data('chessground');
        var dests = JSON.parse(lichess.readServerFen($board.data('x')));
        for (var k in dests) dests[k] = dests[k].match(/.{2}/g);
        cg.set({
          turnColor: cg.state.orientation,
          movable: {
            free: false,
            dests: dests,
            color: cg.state.orientation,
            events: {
              after: function(orig, dest) {
                $captcha.removeClass('success failure');
                submit(orig + ' ' + dest);
              }
            }
          }
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
    });

    if (location.hash === '#enable-blind-mode' && !$('body').hasClass('blind_mode'))
      $.post('/toggle-blind-mode', { enable: 1, redirect: '/' }, lichess.reload);
  });

  ///////////////////
  // tournament.js //
  ///////////////////

  function startTournament(cfg) {
    var element = document.querySelector('main.tour');
    $('body').data('tournament-id', cfg.data.id);
    var tournament;
    lichess.socket = lichess.StrongSocket(
      '/tournament/' + cfg.data.id + '/socket/v4', cfg.data.socketVersion, {
        receive: function(t, d) {
          return tournament.socketReceive(t, d);
        },
        options: {
          name: "tournament"
        }
      });
    cfg.socketSend = lichess.socket.send;
    cfg.element = element;
    cfg.$side = $('.tour__side').clone();
    cfg.$faq = $('.tour__faq').clone();
    tournament = LichessTournament.start(cfg);
  };

  function startSimul(cfg) {
    cfg.element = document.querySelector('main.simul');
    $('body').data('simul-id', cfg.data.id);
    var simul;
    lichess.socket = lichess.StrongSocket(
      '/simul/' + cfg.data.id + '/socket/v4', cfg.socketVersion, {
        receive: function(t, d) {
          simul.socketReceive(t, d);
        },
        options: {
          name: "simul"
        }
      });
    cfg.socketSend = lichess.socket.send;
    cfg.$side = $('.simul__side').clone();
    simul = LichessSimul(cfg);
  }

  ////////////////
  // user_analysis.js //
  ////////////////

  function startUserAnalysis(cfg) {
    var analyse;
    cfg.initialPly = 'url';
    cfg.element = document.querySelector('main.analyse');
    cfg.trans = lichess.trans(cfg.i18n);
    lichess.socket = lichess.StrongSocket('/analysis/socket/v4', false, {
      options: {
        name: 'analyse'
      },
      receive: function(t, d) {
        analyse.socketReceive(t, d);
      }
    });
    cfg.socketSend = lichess.socket.send;
    analyse = LichessAnalyse.start(cfg);
    lichess.topnavIntent();
  }

  ////////////////
  // study.js //
  ////////////////

  function startStudy(element, cfg) {
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
      }
    });
    cfg.socketSend = lichess.socket.send;
    cfg.trans = lichess.trans(cfg.i18n);
    analyse = LichessAnalyse.start(cfg);
    if (cfg.chat) {
      lichess.pubsub.on('chat.enabled', function(v) {
        $('#site_header .board_left').toggleClass('no_chat', !v);
      });
      lichess.makeChat(cfg.chat);
    }
    lichess.topnavIntent();
  }

  ////////////////
  // practice.js //
  ////////////////

  function startPractice(element, cfg) {
    var analyse;
    cfg.element = element.querySelector('.analyse');
    cfg.sideElement = document.querySelector('#site_header .side_box');
    cfg.trans = lichess.trans(cfg.i18n);
    lichess.socket = lichess.StrongSocket('/analysis/socket/v4', false, {
      options: {
        name: "practice"
      },
      receive: function(t, d) {
        analyse.socketReceive(t, d);
      }
    });
    cfg.socketSend = lichess.socket.send;
    analyse = LichessAnalyse.start(cfg);
    lichess.topnavIntent();
  }

  ////////////////
  // relay.js //
  ////////////////

  function startRelay(element, cfg) {
    var analyse;
    cfg.initialPly = 'url';
    cfg.element = element.querySelector('.analyse');
    cfg.sideElement = document.querySelector('#site_header .side_box');
    lichess.socket = lichess.StrongSocket(cfg.socketUrl, cfg.socketVersion, {
      options: {
        name: "relay"
      },
      receive: function(t, d) {
        analyse.socketReceive(t, d);
      }
    });
    cfg.socketSend = lichess.socket.send;
    cfg.trans = lichess.trans(cfg.i18n);
    analyse = LichessAnalyse.start(cfg);
    if (cfg.chat) {
      lichess.pubsub.on('chat.enabled', function(v) {
        $('#site_header .board_left').toggleClass('no_chat', !v);
      });
      lichess.makeChat(cfg.chat);
    }
    lichess.topnavIntent();
  }

  ////////////////
  // puzzle.js //
  ////////////////

  function startPuzzle(cfg) {
    var puzzle;
    cfg.element = document.querySelector('main.puzzle');
    lichess.socket = lichess.StrongSocket('/socket/v4', false, {
      options: {
        name: "puzzle"
      },
      receive: function(t, d) {
        puzzle.socketReceive(t, d);
      }
    });
    cfg.socketSend = lichess.socket.send;
    puzzle = LichessPuzzle.default(cfg);
    lichess.topnavIntent();
  }

})();
