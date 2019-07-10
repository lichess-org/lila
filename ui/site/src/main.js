(function() {

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
    return u ? '<a class="user-link ulpt ' + (klass || '') + '" href="/@/' + id + '">' + (limit ? u.substring(0, limit) : u) + '</a>' : 'Anonymous';
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
        $('#notify-toggle').attr('data-count', e.unread || 0);
        lichess.sound.newPM();
      },
      redirect: function(o) {
        setTimeout(function() {
          lichess.hasToReload = true;
          lichess.redirect(o);
        }, 200);
      },
      deployPost: function() {
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
        if ($('#announce').length || $('body').data("tournament-id") == data.id) return;
        var url = '/tournament/' + data.id;
        $('body').append(
          '<div id="announce">' +
          '<a data-icon="g" class="text" href="' + url + '">' + data.name + '</a>' +
          '<div class="actions">' +
          '<a class="withdraw text" href="' + url + '/withdraw" data-icon="Z">Pause</a>' +
          '<a class="text" href="' + url + '" data-icon="G">Join</a>' +
          '</div></div>'
        ).find('#announce .withdraw').click(function() {
          $.post($(this).attr("href"));
          $('#announce').remove();
          return false;
        });
      },
      announce: function(d) {
        if (!$('#announce').length) $('body').append(
          '<div id="announce" class="announce">' +
          d.msg +
          '<div class="actions"><a class="close">X</a></div>' +
          '</div>'
        ).find('#announce .close').click(function() { $('#announce').remove(); });
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
    lichess.loadCssPath('autocomplete');
    return lichess.loadScript('javascripts/vendor/typeahead.jquery.min.js').done(function() {
      $input.typeahead({
        minLength: opts.minLength || 3,
      }, {
        hint: true,
        highlight: false,
        source: function(query, _, runAsync) {
          if (query.trim().match(/^[a-z0-9][\w-]{2,29}$/i)) $.ajax({
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
            return '<' + tag + ' class="ulpt user-link' + (o.online ? ' online' : '') + '" ' + (tag === 'a' ? '' : 'data-') + 'href="/@/' + o.name + '">' +
              '<i class="line' + (o.patron ? ' patron' : '') + '"></i>' + (o.title ? '<span class="title">' + o.title + '</span>&nbsp;' : '')  + o.name +
              '</' + tag + '>';
          }
        }
      }).on('typeahead:render', function() {
        lichess.pubsub.emit('content_loaded');
      });
      if (opts.focus) $input.focus();
      if (opts.onSelect) $input.on('typeahead:select', function(ev, sel) {
        opts.onSelect(sel);
      }).on('keypress', function(e) {
        if (e.which == 10 || e.which == 13) opts.onSelect($(this).val());
      });
    });
  };

  lichess.parseFen = function($elem) {
    if (!window.Chessground) return setTimeout(function() {
      lichess.parseFen($elem);
    }, 500); // if not loaded yet
    // sometimes $elem is not a jQuery, can happen when content_loaded is triggered with random args
    if (!$elem || !$elem.each) $elem = $('.parse-fen');
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
    else if (lichess.study) startStudy(lichess.study);
    else if (lichess.practice) startPractice(lichess.practice);
    else if (lichess.relay) startRelay(lichess.relay);
    else if (lichess.puzzle) startPuzzle(lichess.puzzle);
    else if (lichess.tournament) startTournament(lichess.tournament);
    else if (lichess.simul) startSimul(lichess.simul);

    // delay so round starts first (just for perceived perf)
    lichess.requestIdleCallback(function() {

      $('#reconnecting').on('click', function() {
        window.location.reload();
      });

      $('#friend_box').friends();

      $('#main-wrap')
        .on('click', '.autoselect', function() {
          $(this).select();
        })
        .on('click', 'button.copy', function() {
          $('#' + $(this).data('rel')).select();
          document.execCommand('copy');
          $(this).attr('data-icon', 'E');
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
        if (lichess.socket === null) {
          lichess.socket = lichess.StrongSocket("/socket/v4", false, {
            options: {
              remoteSocketDomain: document.body.getAttribute('data-remote-socket-domain')
            }
          });
        }
      }, 300);

      var initiatingHtml = '<div class="initiating">' + lichess.spinnerHtml + '</div>';

      lichess.challengeApp = (function() {
        var instance, booted;
        var $toggle = $('#challenge-toggle');
        $toggle.one('mouseover click', function() {
          load();
        });
        var load = function(data) {
          if (booted) return;
          booted = true;
          var $el = $('#challenge-app').html(lichess.initiatingHtml);
          lichess.loadCssPath('challenge');
          lichess.loadScript(lichess.compiledScript('challenge')).done(function() {
            instance = LichessChallenge.default($el[0], {
              data: data,
              show: function() {
                if (!$('#challenge-app').is(':visible')) $toggle.click();
              },
              setCount: function(nb) {
                $toggle.find('span').attr('data-count', nb);
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
        var $toggle = $('#notify-toggle');
        var isVisible = function() {
          return $('#notify-app').is(':visible');
        };

        var load = function(data, incoming) {
          if (booted) return;
          booted = true;
          var $el = $('#notify-app').html(initiatingHtml);
          lichess.loadCssPath('notify');
          lichess.loadScript(lichess.compiledScript('notify')).done(function() {
            instance = LichessNotify.default($el.empty()[0], {
              data: data,
              incoming: incoming,
              isVisible: isVisible,
              setCount: function(nb) {
                $toggle.find('span').attr('data-count', nb);
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
            lichess.pushSubscribe(true);
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

      window.addEventListener('resize', () => lichess.dispatchEvent(document.body, 'chessground.resize'));

      // dasher
      (function() {
        var booted;
        $('#top .dasher .toggle').one('mouseover click', function() {
          if (booted) return;
          booted = true;
          var $el = $('#dasher_app').html(initiatingHtml);
          var isPlaying = $('body').hasClass('playing');
          lichess.loadCssPath('dasher');
          lichess.loadScript(lichess.compiledScript('dasher')).done(function() {
            LichessDasher.default($el.empty()[0], {
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
          if (booted) return $.Deferred().resolve();
          booted = true;
          return lichess.loadScript(lichess.compiledScript('cli')).done(function() {
            LichessCli.app($wrap, toggle);
          });
        }
        var toggle = function(txt) {
          boot().done(function() {
            $wrap.find('input').val(txt || '');
          });
          $('body').toggleClass('clinput');
          if ($('body').hasClass('clinput')) $wrap.find('input').focus();
        };
        $wrap.find('a').on('mouseover click', function(e) {
          (e.type === 'mouseover' ? boot : toggle)();
        });
        Mousetrap.bind('/', function() {
          lichess.raf(function() { toggle('/') });
          return false;
        });
        Mousetrap.bind('s', function() {
          lichess.raf(function() { toggle() });
        });
      })();

      $('.user-autocomplete').each(function() {
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
            lichess.pubsub.emit('content_loaded');
            var ids = [];
            $(el).find('.paginated[data-dedup]').each(function() {
              var id = $(this).data('dedup');
              if (id) {
                if (ids.includes(id)) $(this).remove();
                else ids.push(id);
              }
            });
          }).find('div.pager').hide().end();
          $scroller.parent().append($('<button class="inf-more button button-empty">More</button>').on('click', function() {
            $scroller.infinitescroll('retrieve');
          }));
        });
      }
      lichess.loadInfiniteScroll('.infinitescroll');

      $('#top').on('click', 'a.toggle', function() {
        var $p = $(this).parent();
        $p.toggleClass('shown');
        $p.siblings('.shown').removeClass('shown');
        lichess.pubsub.emit('top.toggle.' + $(this).attr('id'));
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

      // still bind esc even in form fields
      Mousetrap.prototype.stopCallback = function(e, el, combo) {
        return combo !== 'esc' && (el.tagName === 'INPUT' || el.tagName === 'SELECT' || el.tagName === 'TEXTAREA');
      };
      Mousetrap.bind('esc', function() {
        var $oc = $('#modal-wrap .close');
        if ($oc.length) $oc.trigger('click');
        else {
          var $input = $(':focus');
          if ($input.length) $input.trigger('blur');
        }
        return false;
      });

      if (!lichess.storage.get('grid')) setTimeout(function() {
        if (getComputedStyle(document.body).getPropertyValue('--grid'))
          lichess.storage.set('grid', 1);
        else
          $.get(lichess.assetUrl('oops/browser.html'), html => $('body').prepend(html))
      }, 3000);

      /* A disgusting hack for a disgusting browser
       * Edge randomly fails to rasterize SVG on page load
       * A different SVG must be loaded so a new image can be rasterized */
      if (navigator.userAgent.indexOf('Edge/') > -1) setTimeout(function() {
        const sprite = $('#piece-sprite');
        sprite.attr('href', sprite.attr('href').replace('.css', '.external.css'));
      }, 1000);

      if (window.Fingerprint2) setTimeout(function() {
        var t = Date.now()
        new Fingerprint2({
          excludeJsFonts: true
        }).get(function(res) {
          var $i = $('#signup-fp-input');
          if ($i.length) $i.val(res);
          else $.post('/auth/set-fp/' + res + '/' + (Date.now() - t));
        });
      }, 500);
    });
  });

  lichess.sound = (function() {
    var api = {};
    var soundSet = $('body').data('sound-set');

    var speechStorage = lichess.storage.makeBoolean('speech.enabled');
    api.speech = function(v) {
      if (typeof v == 'undefined') return speechStorage.get();
      speechStorage.set(v);
      collection.clear();
    };
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
      confirmation: 'Confirmation',
      error: 'Error'
    };
    for (var i = 0; i <= 10; i++) names['countDown' + i] = 'CountDown' + i;

    var volumes = {
      lowtime: 0.5,
      explode: 0.35,
      confirmation: 0.5
    };
    var collection = new memoize(function(k) {
      var set = soundSet;
      if (set === 'music' || speechStorage.get()) {
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
      api[name] = function(text) {
        if (!enabled()) return;
        if (!text || !api.say(text)) {
          Howler.volume(api.getVolume());
          var sound = collection(name);
          if (Howler.ctx && Howler.ctx.state == "suspended") {
            Howler.ctx.resume().then(() => sound.play());
          } else {
            sound.play();
          }
        }
      }
    });
    api.say = function(text, cut) {
      if (!speechStorage.get()) return false;
      var msg = text.text ? text : new SpeechSynthesisUtterance(text);
      msg.volume = api.getVolume();
      msg.lang = 'en-US';
      if (cut) speechSynthesis.cancel();
      speechSynthesis.speak(msg);
      console.log(`%c${msg.text}`, 'color: blue');
      return true;
    };
    api.load = function(name) {
      if (enabled() && name in names) collection(name);
    };
    api.setVolume = function(v) {
      api.volumeStorage.set(v);
      Howler.volume(v);
    };
    api.getVolume = () => {
      // garbage has been stored stored by accident (e972d5612d)
      const v = parseFloat(api.volumeStorage.get());
      return v >= 0 ? v : api.defaultVolume;
    }

    var publish = function() {
      lichess.pubsub.emit('sound_set', soundSet);
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

  lichess.widget('watchers', {
    _create: function() {
      this.list = this.element.find(".list");
      this.number = this.element.find(".number");
      lichess.pubsub.on('socket.in.crowd', data => this.set(data.watchers || data));
      lichess.watchersData && this.set(lichess.watchersData);
    },
    set: function(data) {
      lichess.watchersData = data;
      if (!data || !data.nb) return this.element.addClass('none');
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
      var icon = '<i class="line' + (user.patron ? ' patron' : '') + '"></i>';
      var titleTag = user.title ? ('<span class="title"' + (user.title === 'BOT' ? ' data-bot' : '') + '>' + user.title + '</span>&nbsp;') : '';
      var url = '/@/' + user.name;
      var tvButton = user.playing ? '<a data-icon="1" class="tv ulpt" data-pt-pos="nw" href="' + url + '/tv" data-href="' + url + '"></a>' : '';
      var studyButton = user.studying ? '<a data-icon="4" class="friend-study" href="' + url + '/studyTv"></a>' : '';
      var rightButton = tvButton || studyButton;
      return '<div><a class="user-link ulpt" data-pt-pos="nw" href="' + url + '">' + icon + titleTag + user.name + '</a>' + rightButton + '</div>';
    };
    return {
      _create: function() {
        var self = this;
        var el = self.element;

        var hideStorage = lichess.storage.makeBoolean('friends-hide');
        var $friendBoxTitle = el.find('.friend_box_title').click(function() {
          el.find('.content_wrap').toggleNone(hideStorage.get());
          hideStorage.toggle();
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
          var users = this.users, ids = Object.keys(users).sort();
          this.$nbOnline.text(ids.length);
          this.$nobody.toggleNone(!ids.length);
          this.element.find('.list').html(
            ids.map(function(id) { return renderUser(users[id]); }).join('')
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
        var i;
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

  $(function() {
    lichess.pubsub.on('content_loaded', lichess.parseFen);

    var socketOpened = false;

    function startWatching() {
      if (!socketOpened) return;
      var ids = [];
      $('.mini-board.live').removeClass("live").each(function() {
        ids.push(this.getAttribute("data-live"));
      });
      if (ids.length) lichess.socket.send("startWatching", ids.join(" "));
    }
    lichess.pubsub.on('content_loaded', startWatching);
    lichess.pubsub.on('socket.open', function() {
      socketOpened = true;
      startWatching();
    });

    lichess.requestIdleCallback(function() {
      lichess.parseFen();
      $('.chat__members').watchers();
      if (location.hash === '#blind' && !$('body').hasClass('blind-mode'))
        $.post('/toggle-blind-mode', { enable: 1, redirect: '/' }, lichess.reload);
    });
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
        }
      });
    cfg.socketSend = lichess.socket.send;
    cfg.element = element;
    cfg.$side = $('.tour__side').clone();
    cfg.$faq = $('.tour__faq').clone();
    tournament = LichessTournament.start(cfg);
  }

  function startSimul(cfg) {
    cfg.element = document.querySelector('main.simul');
    $('body').data('simul-id', cfg.data.id);
    var simul;
    lichess.socket = lichess.StrongSocket(
      '/simul/' + cfg.data.id + '/socket/v4', cfg.socketVersion, {
        receive: function(t, d) {
          simul.socketReceive(t, d);
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
    cfg.trans = lichess.trans(cfg.i18n);
    lichess.socket = lichess.StrongSocket('/analysis/socket/v4', false, {
      receive: function(t, d) {
        analyse.socketReceive(t, d);
      }
    });
    cfg.socketSend = lichess.socket.send;
    cfg.$side = $('.analyse__side').clone();
    analyse = LichessAnalyse.start(cfg);
  }

  ////////////////
  // study.js //
  ////////////////

  function startStudy(cfg) {
    var analyse;
    cfg.initialPly = 'url';
    lichess.socket = lichess.StrongSocket(cfg.socketUrl, cfg.socketVersion, {
      receive: function(t, d) {
        analyse.socketReceive(t, d);
      }
    });
    cfg.socketSend = lichess.socket.send;
    cfg.trans = lichess.trans(cfg.i18n);
    analyse = LichessAnalyse.start(cfg);
  }

  ////////////////
  // practice.js //
  ////////////////

  function startPractice(cfg) {
    var analyse;
    cfg.trans = lichess.trans(cfg.i18n);
    lichess.socket = lichess.StrongSocket('/analysis/socket/v4', false, {
      receive: function(t, d) {
        analyse.socketReceive(t, d);
      }
    });
    cfg.socketSend = lichess.socket.send;
    analyse = LichessAnalyse.start(cfg);
  }

  ////////////////
  // relay.js //
  ////////////////

  function startRelay(cfg) {
    var analyse;
    cfg.initialPly = 'url';
    lichess.socket = lichess.StrongSocket(cfg.socketUrl, cfg.socketVersion, {
      receive: function(t, d) {
        analyse.socketReceive(t, d);
      }
    });
    cfg.socketSend = lichess.socket.send;
    cfg.trans = lichess.trans(cfg.i18n);
    analyse = LichessAnalyse.start(cfg);
  }

  ////////////////
  // puzzle.js //
  ////////////////

  function startPuzzle(cfg) {
    var puzzle;
    cfg.element = document.querySelector('main.puzzle');
    lichess.socket = lichess.StrongSocket('/socket/v4', false, {
      receive: function(t, d) {
        puzzle.socketReceive(t, d);
      }
    });
    cfg.socketSend = lichess.socket.send;
    puzzle = LichessPuzzle.default(cfg);
  }

  ////////////////////
  // service worker //
  ////////////////////

  var pushBeta = !!document.body.getAttribute('data-vapid');
  if (pushBeta && 'serviceWorker' in navigator && 'Notification' in window && 'PushManager' in window) {
    var workerUrl = lichess.assetUrl('javascripts/service-worker.js', {noVersion: true, sameDomain: true});
    navigator.serviceWorker.register(workerUrl, {scope: '/'});
  }

  lichess.pushSubscribe = function(ask) {
    if ('serviceWorker' in navigator && 'Notification' in window && 'PushManager' in window) {
      navigator.serviceWorker.ready.then(reg => {
        var storage = lichess.storage.make('push-subscribed');
        var vapid = document.body.getAttribute('data-vapid');
        var allowed = (ask || Notification.permission === 'granted') && Notification.permission !== 'denied';
        if (vapid && allowed) return reg.pushManager.getSubscription().then(sub => {
          var resub = parseInt(storage.get() || '0', 10) + 43200000 < Date.now(); // 12 hours
          var applicationServerKey = Uint8Array.from(atob(vapid), c => c.charCodeAt(0));
          if (!sub || resub) {
            return reg.pushManager.subscribe({
              userVisibleOnly: true,
              applicationServerKey: applicationServerKey
            }).then(sub => fetch('/push/subscribe', {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json'
              },
              body: JSON.stringify(sub)
            })).then(res => {
              if (res.ok) storage.set('' + Date.now());
              else throw Error(response.statusText);
            }).catch(err => console.log('push subscribe failed', err.message));
          }
        });
        else storage.remove();
      });
    }
  };

  lichess.pushSubscribe(false); // opportunistic push subscription
})();
