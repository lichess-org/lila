if (typeof console == "undefined" || typeof console.log == "undefined") console = {
  log: function() {}
};

// declare now, populate later
var lichess_translations = [];

var lichess = {
  socket: null,
  socketDefaults: {
    events: {
      n: function(e) {
        var $tag = $('#nb_connected_players');
        if ($tag.length && e) {
          $tag.html($tag.html().replace(/\d+/, e)).removeClass('none');
        }
      },
      nbm: function(e) {
        var $tag = $('#nb_messages');
        $tag.text(e).toggleClass("unread", e > 0);
      }
    },
    options: {
      name: "site",
      offlineTag: $('#connection_lost'),
      lagTag: $('#connection_lag')
    }
  },
  onProduction: /.+\.lichess\.org/.test(document.domain),
  socketUrl: document.domain + ":9000"
};
//lichess.socketDefaults.options.debug = !lichess.onProduction;

$(function() {

  if (!$.websocket.available) {
    $.ajax('/assets/browser.html', { success: function(html) { $('body').prepend(html); } });
  }

  // Start game
  var $game = $('div.lichess_game').orNot();
  if ($game) {
    $game.game(lichess_data);
    if (!lichess_data.player.spectator) {
      $('a.blank_if_play').click(function() {
        if ($game.game('isPlayable')) {
          $(this).attr('target', '_blank');
        }
      });
    }
  }

  setTimeout(function() {
    if (lichess.socket == null && $('div.server_error_box').length == 0) {
      lichess.socket = new $.websocket(lichess.socketUrl + "/socket", 0, lichess.socketDefaults);
    }
  }, 2000);

  $('input.lichess_id_input').select();

  if ($board = $('div.colorable_board').orNot()) {
    if ($board.hasClass("with_marks"))
      $.displayBoardMarks($board.parent(), $('#lichess > div.lichess_player_white').length);

    // board color
    var $picker = $('#top a.colorpicker');
    var colors = ['brown', 'grey', 'green', 'blue', 'wood'];
    var color;
    function setColor(c) {
      color = c;
      $picker.add($board).removeClass(colors.join(' ')).addClass(c);
    }
    setColor($picker.data('color'));
    $picker.click(function() {
      var c = colors[(colors.indexOf(color) + 1) % colors.length];
      setColor(c);
      $.ajax($picker.attr("href"), {
        type: 'POST',
        data: {color: c}
      });
      return false;
    });
  } else {
    $('#top a.colorpicker').remove();
  }

  $.centerOverboard = function() {
    if ($overboard = $('div.lichess_overboard.auto_center').orNot()) {
      $overboard.css('top', (238 - $overboard.height() / 2) + 'px').show();
    }
  };
  $.centerOverboard();

  $('div.lichess_language').click(function() {
    $(this).toggleClass('toggled');
  });

  $('.js_email').one('click', function() {
    var email = ['thibault.', 'dupl', 'essis@', 'gmail.com'].join('');
    $(this).replaceWith($('<a/>').text(email).attr('href', 'mailto:'+email));
  });

  function translateTexts() {
    $('.trans_me').each(function() {
      $(this).removeClass('trans_me').text($.trans($(this).text()));
    });
  }
  translateTexts();
  $('body').on('lichess.content_loaded', translateTexts);

  $.tipsyfy = function($elem) {
    $elem.find('a:not(div.game_list_inner a):not(.notipsy):not(#boardTable a), input, label, .tipsyme, button').filter('[title]').tipsy({
      fade: true,
      html: false,
      live: true
    });
  };
  $.tipsyfy($('body'));

  if ($autocomplete = $('input.autocomplete').orNot()) {
    $autocomplete.autocomplete({
      source: $autocomplete.attr('data-provider'),
      minLength: 2,
      delay: 100
    });
  }

  $('.infinitescroll:has(.pager a)').each(function() {
    $(this).infinitescroll({
      navSelector: ".pager",
      nextSelector: ".pager a:last",
      itemSelector: ".infinitescroll .paginated_element",
      loading: {
        msgText: "",
        img: "/assets/images/hloader.gif",
        finishedMsg: "---"
      }
    }, function() {
      $("#infscr-loading").remove();
      $('body').trigger('lichess.content_loaded');
    }).find('div.pager').hide();
  });

  $('a.toggle_auth').toggle(function() {
    $('#top').find('div.auth').addClass('shown').find('input:first').focus();
  },
  function() {
    $('#top').find('div.auth').removeClass('shown');
  });

  $('#lichess_translation_form_code').change(function() {
    if ("0" != $(this).val()) {
      location.href = $(this).closest('form').attr('data-change-url').replace(/__/, $(this).val());
    }
  });

  $('#incomplete_translation a.close').one('click', function() {
    $(this).parent().remove();
  });

  $('a.delete, input.delete').click(function() {
    return confirm('Delete?');
  });
  $('input.confirm').click(function() {
    return confirm('Confirm this action?');
  });

  function bookmarks() {
    $('span.bookmark a.icon:not(.jsed)').each(function() {
      var t = $(this).addClass("jsed");
      t.click(function() {
        t.toggleClass("bookmarked");
        $.post(t.attr("href"));
        var count = (parseInt(t.html()) || 0) + (t.hasClass("bookmarked") ? 1 : -1);
        t.html(count > 0 ? count : "");
        return false;
      });
    });
  }
  bookmarks();
  $('body').on('lichess.content_loaded', bookmarks);

  $("a.view_pgn_toggle").one("click", function() {
    var $this = $(this).text("...");
    $.ajax({
      url: $this.attr("href"),
      success: function(text) {
        $this.after("<pre>" + text + "</pre>").text("Download PGN");
      }
    });
    return false;
  });

  var elem = document.createElement('audio');
  var canPlayAudio = !! elem.canPlayType && elem.canPlayType('audio/ogg; codecs="vorbis"');
  var $soundToggle = $('#sound_state');

  function soundEnabled() {
    return $soundToggle.hasClass("sound_state_on");
  }

  $.playSound = function() {
    if (canPlayAudio && soundEnabled()) {
      var sound = $('#lichess_sound_player').get(0);
      sound.play();
      setTimeout(function() {
        sound.pause();
      },
      1000);
    }
  }

  if (canPlayAudio) {
    $('body').append($('<audio id="lichess_sound_player">').attr('src', $('body').attr('data-sound-file')));
    $soundToggle.click(function() {
      var enabled = !soundEnabled();
      $soundToggle.toggleClass('sound_state_on', enabled);
      $.playSound();
      $.post($soundToggle.attr('href'), {sound: enabled});
      return false;
    });
    $game && $game.trigger('lichess.audio_ready');
  } else {
    $soundToggle.addClass('unavailable');
  }

  if(Boolean(window.chrome)) {
    $("div.addtochrome").show();
  }

});

$.fn.scrollable = function() {
  this.mousewheel(function(e, delta) {
    this.scrollTop -= delta * 30;
    return false;
  });
};

$.fn.orNot = function() {
  return this.length == 0 ? false: this;
};

$.trans = function(text) {
  //if (!lichess_translations[text]) console.debug(text);
  return lichess_translations[text] ? lichess_translations[text] : text;
}

$.displayBoardMarks = function($board, isWhite) {
  if (isWhite) {
    var factor = 1;
    var base = 0;
  } else {
    var factor = - 1;
    var base = 575;
  }
  $board.find('span.board_mark').remove();
  var letters = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];
  var marks = '';
  for (i = 1; i < 9; i++) {
    marks += '<span class="board_mark vert" style="bottom:' + (factor * i * 64 - 38 + base) + 'px;">' + i + '</span>';
    marks += '<span class="board_mark horz" style="left:' + (factor * i * 64 - 35 + base) + 'px;">' + letters[i - 1] + '</span>';
  }
  $board.append(marks);
};

if (lichess.onProduction) {
  //analytics
  var _gaq = _gaq || [];
  _gaq.push(['_setAccount', 'UA-7935029-3']);
  _gaq.push(['_trackPageview']);
  (function() {
    var ga = document.createElement('script');
    ga.type = 'text/javascript';
    ga.async = true;
    ga.src = 'http://www.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0];
    s.parentNode.insertBefore(ga, s);
  })();
}

function urlToLink(text) {
  var exp = /\bhttp:\/\/(?:[a-z]{0,3}\.)?(lichess\.org[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/ig;
  return text.replace(exp,"<a href='http://$1'>$1</a>");
}
