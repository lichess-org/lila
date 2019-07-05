module.exports = function(cfg, element) {
  var pools = [{id:"1+0",lim:1,inc:0,perf:"Bullet"},{id:"2+1",lim:2,inc:1,perf:"Bullet"},{id:"3+0",lim:3,inc:0,perf:"Blitz"},{"id":"3+2","lim":3,"inc":2,"perf":"Blitz"},{id:"5+0",lim:5,inc:0,perf:"Blitz"},{"id":"5+3","lim":5,"inc":3,"perf":"Blitz"},{id:"10+0",lim:10,inc:0,perf:"Rapid"},{id:"15+15",lim:15,inc:15,perf:"Classical"}];
  var lobby;
  var nbRoundSpread = spreadNumber(
    document.querySelector('#nb_games_in_play > strong'),
    8,
    function() {
      return lichess.socket.pingInterval();
    });
  var nbUserSpread = spreadNumber(
    document.querySelector('#nb_connected_players > strong'),
    10,
    function() {
      return lichess.socket.pingInterval();
    });
  var getParameterByName = function(name) {
    var match = RegExp('[?&]' + name + '=([^&]*)').exec(location.search);
    return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
  };
  var onFirstConnect = function() {
    var gameId = getParameterByName('hook_like');
    if (!gameId) return;
    $.post('/setup/hook/' + lichess.StrongSocket.sri + '/like/' + gameId);
    lobby.setTab('real_time');
    history.replaceState(null, null, '/');
  };
  var filterStreams = function() {
    var langs = navigator.languages;
    if (!langs) return; // tss... https://developer.mozilla.org/en-US/docs/Web/API/NavigatorLanguage/languages
    langs = langs.map(function(l) {
      return l.slice(0, 2).toLowerCase();
    });
    langs.push($('html').attr('lang'));
    $('.lobby__streams a, .event-spotlight').each(function() {
      var match = $(this).text().match(/\[(\w{2})\]/mi);
      if (match && !langs.includes(match[1].toLowerCase())) $(this).hide();
    });
  };
  filterStreams();
  lichess.socket = lichess.StrongSocket(
    '/lobby/socket/v4',
    false, {
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
              lichess.pubsub.emit('content_loaded');
            }
          });
        },
        streams: function(html) {
          $('.lobby__streams').html(html);
          filterStreams();
        },
        featured: function(o) {
          $('.lobby__tv').html(o.html);
          lichess.pubsub.emit('content_loaded');
        },
        redirect: function(e) {
          lobby.leavePool();
          lobby.setRedirecting();
          window.lichess.redirect(e);
        },
        tournaments: function(data) {
          $("#enterable_tournaments").html(data);
          lichess.pubsub.emit('content_loaded');
        },
        simuls: function(data) {
          $("#enterable_simuls").html(data).parent().toggle($('#enterable_simuls tr').length > 0);
          lichess.pubsub.emit('content_loaded');
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

  cfg.trans = lichess.trans(cfg.i18n);
  cfg.socketSend = lichess.socket.send;
  cfg.element = element;
  cfg.pools = pools;
  lobby = LichessLobby.start(cfg);

  var blindMode = $('body').hasClass('blind-mode');

  var $startButtons = $('.lobby__start');

  var sliderTimes = [
    0, 1/4, 1/2, 3/4, 1, 3/2, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
    15, 16, 17, 18, 19, 20, 25, 30, 35, 40, 45, 60, 90, 120, 150, 180
  ];

  function sliderTime(v) {
    return v < sliderTimes.length ? sliderTimes[v] : 180;
  }

  function showTime(v) {
    if (v === 1 / 4) return '¼';
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

  function hookToPoolMember(color, data) {
    var hash = {};
    for (var i in data) hash[data[i].name] = data[i].value;
    var valid = color == 'random' && hash.variant == 1 && hash.mode == 1 && hash.timeMode == 1;
    var id = parseFloat(hash.time) + '+' + parseInt(hash.increment);
    var exists = pools.find(function(p) { return p.id === id; });
    if (valid && exists) return {
      id: id,
      range: hash.ratingRange
    };
  }

  function prepareForm($modal) {
    var $form = $modal.find('form');
    var $timeModeSelect = $form.find('#sf_timeMode');
    var $modeChoicesWrap = $form.find('.mode_choice');
    var $modeChoices = $modeChoicesWrap.find('input');
    var $casual = $modeChoices.eq(0),
      $rated = $modeChoices.eq(1);
    var $variantSelect = $form.find('#sf_variant');
    var $fenPosition = $form.find(".fen_position");
    var $timeInput = $form.find('.time_choice [name=time]');
    var $incrementInput = $form.find('.increment_choice [name=increment]');
    var $daysInput = $form.find('.days_choice [name=days]');
    var typ = $form.data('type');
    var $ratings = $modal.find('.ratings > div');
    var randomColorVariants = $form.data('random-color-variants').split(',');
    var $submits = $form.find('.color-submits__button');
    var toggleButtons = function() {
      var variantId = $variantSelect.val();
      var timeMode = $timeModeSelect.val();
      var rated = $rated.prop('checked');
      var limit = $timeInput.val();
      var inc = $incrementInput.val();
      // no rated variants with less than 30s on the clock
      var cantBeRated = (timeMode == '1' && variantId != '1' && limit < 0.5 && inc == 0) ||
        (variantId != '1' && timeMode != '1');
      if (cantBeRated && rated) {
        $casual.click();
        return toggleButtons();
      }
      $rated.attr('disabled', cantBeRated).siblings('label').toggleClass('disabled', cantBeRated);
      var timeOk = timeMode != '1' || limit > 0 || inc > 0;
      var ratedOk = typ != 'hook' || !rated || timeMode != '0';
      var aiOk = typ != 'ai' || variantId != '3' || limit >= 1;
      if (timeOk && ratedOk && aiOk) {
        $submits.toggleClass('nope', false);
        $submits.filter(':not(.random)').toggle(!rated || !randomColorVariants.includes(variantId));
      } else $submits.toggleClass('nope', true);
    };
    var showRating = function() {
      var timeMode = $timeModeSelect.val();
      var key;
      switch ($variantSelect.val()) {
        case '1':
        case '3':
          if (timeMode == '1') {
            var time = $timeInput.val() * 60 + $incrementInput.val() * 40;
            if (time < 30) key = 'ultraBullet';
            else if (time < 180) key = 'bullet';
            else if (time < 480) key = 'blitz';
            else if (time < 1500) key = 'rapid';
            else key = 'classical';
          } else key = 'correspondence';
          break;
        case '10':
          key = 'crazyhouse';
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
    if (typ == 'hook') {
      if ($form.data('anon')) {
        $timeModeSelect.val(1)
          .children('.timeMode_2, .timeMode_0')
          .prop('disabled', true)
          .attr('title', cfg.trans('youNeedAnAccountToDoThat'));
      }
      var ajaxSubmit = function(color) {
        var poolMember = hookToPoolMember(color, $form.serializeArray());
        $.modal.close();
        var call = {
          url: $form.attr('action').replace(/uid-placeholder/, lichess.StrongSocket.sri),
          data: $form.serialize() + "&color=" + color,
          type: 'post'
        };
        if (poolMember) {
          lobby.enterPool(poolMember);
          lobby.redraw();
          call.url += '?pool=1';
        } else lobby.setTab($timeModeSelect.val() === '1' ? 'real_time' : 'seeks');
        $.ajax(call);
        return false;
      };
      $submits.click(function() {
        return ajaxSubmit($(this).val());
      }).attr('disabled', false);
      $form.submit(function() {
        return ajaxSubmit('random');
      });
    } else $form.one('submit', function() {
      $submits.hide().end().append(lichess.spinnerHtml);
    });
    if (blindMode) {
      $variantSelect.focus();
      $timeInput.add($incrementInput).on('change', function() {
        toggleButtons();
        showRating();
      });
    } else lichess.slider().done(function() {
      $timeInput.add($incrementInput).each(function() {
        var $input = $(this),
          $value = $input.siblings('span');
        var isTimeSlider = $input.parent().hasClass('time_choice');
        $input.after($('<div>').slider({
          value: sliderInitVal(parseFloat($input.val()), isTimeSlider ? sliderTime : sliderIncrement, 100),
          min: 0,
          max: isTimeSlider ? 34 : 30,
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
        $input.after($('<div>').slider({
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
      $form.find('.rating-range').each(function() {
        var $this = $(this);
        var $input = $this.find("input");
        var $span = $this.siblings("span.range");
        var min = $input.data("min");
        var max = $input.data("max");
        var values = $input.val() ? $input.val().split("-") : [min, max];

        $span.text(values.join('–'));
        $this.slider({
          range: true,
          min: min,
          max: max,
          values: values,
          step: 50,
          slide: function(event, ui) {
            $input.val(ui.values[0] + "-" + ui.values[1]);
            $span.text(ui.values[0] + "–" + ui.values[1]);
          }
        });
      });
    });
    $timeModeSelect.on('change', function() {
      var timeMode = $(this).val();
      $form.find('.time_choice, .increment_choice').toggle(timeMode == '1');
      $form.find('.days_choice').toggle(timeMode == '2');
      toggleButtons();
      showRating();
    }).trigger('change');

    var $fenInput = $fenPosition.find('input');
    var validateFen = lichess.debounce(function() {
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
            $submits.removeClass('nope');
            lichess.pubsub.emit('content_loaded');
          },
          error: function() {
            $fenInput.addClass("failure");
            $fenPosition.find('.preview').html("");
            $submits.addClass('nope');
          }
        });
      }
    }, 200);
    $fenInput.on('keyup', validateFen);

    $variantSelect.on('change', function() {
      var fen = $(this).val() == '3';
      $fenPosition.toggle(fen);
      $modeChoicesWrap.toggle(!fen);
      if (fen) {
        $casual.click();
        lichess.raf(function() {
          document.body.dispatchEvent(new Event('chessground.resize'));
        });
      }
      showRating();
      toggleButtons();
    }).trigger('change');

    $form.find('div.level').each(function() {
      var $infos = $(this).find('.ai_info > div');
      $(this).find('label').on('mouseenter', function() {
        $infos.hide().filter('.' + $(this).attr('for')).show();
      });
      $(this).find('#config_level').on('mouseleave', function() {
        var level = $(this).find('input:checked').val();
        $infos.hide().filter('.sf_level_' + level).show();
      }).trigger('mouseout');
    });
  }

  var clickEvent = blindMode ? 'click' : 'mousedown';

  $startButtons.find('a:not(.disabled)').on(clickEvent, function() {
    $(this).addClass('active').siblings().removeClass('active');
    lichess.loadCssPath('lobby.setup');
    lobby.leavePool();
    $.ajax({
      url: $(this).attr('href'),
      success: function(html) {
        prepareForm($.modal(html, 'game-setup', () => {
          $startButtons.find('.active').removeClass('active');
        }));
        lichess.pubsub.emit('content_loaded');
      },
      error: function(res) {
        if (res.status == 400) alert(res.responseText);
        lichess.reload();
      }
    });
    return false;
  }).on('click', function() {
    return false;
  });

  if (['#ai', '#friend', '#hook'].includes(location.hash)) {
    $startButtons
      .find('.config_' + location.hash.replace('#', ''))
      .each(function() {
        $(this).attr("href", $(this).attr("href") + location.search);
      }).trigger(clickEvent);

    if (location.hash === '#hook') {
      if (/time=realTime/.test(location.search))
        lobby.setTab('real_time');
      else if (/time=correspondence/.test(location.search))
        lobby.setTab('seeks');
    }

    history.replaceState(null, null, '/');
  }
};

function spreadNumber(el, nbSteps, getDuration) {
  var previous, displayed;
  var display = function(prev, cur, it) {
    var val = lichess.numberFormat(Math.round(((prev * (nbSteps - 1 - it)) + (cur * (it + 1))) / nbSteps));
    if (val !== displayed) {
      el.textContent = val;
      displayed = val;
    }
  };
  var timeouts = [];
  return function(nb, overrideNbSteps) {
    if (!el || (!nb && nb !== 0)) return;
    if (overrideNbSteps) nbSteps = Math.abs(overrideNbSteps);
    timeouts.forEach(clearTimeout);
    timeouts = [];
    var prev = previous === 0 ? 0 : (previous || nb);
    previous = nb;
    var interv = Math.abs(getDuration() / nbSteps);
    for (var i = 0; i < nbSteps; i++)
      timeouts.push(setTimeout(display.bind(null, prev, nb, i), Math.round(i * interv)));
  };
}
