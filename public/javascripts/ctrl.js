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
        $('#nb_messages').text(e || "0").toggleClass("unread", e > 0);
      },
      notificationAdd: function(html) {
        $('div.notifications ').prepend(html);
      },
      notificationRemove: function(id) {
        $('#' + id).remove();
      },
      analysisAvailable: function() {
        $("div.game_analysis.status").remove();
        $("div.game_analysis").show();
        $.playSound();
        document.title = "/!\\ ANALYSIS READY! " + document.title;
      }
    },
    params: {},
    options: {
      name: "site",
      offlineTag: $('#connection_lost'),
      lagTag: $('#connection_lag')
    }
  },
  onProduction: /.+\.lichess\.org/.test(document.domain),
  socketUrl: document.domain + ":9000"
};
lichess.socketDefaults.options.debug = !lichess.onProduction;

$(function() {

  if (!$.websocket.available) {
    if (window.opera) {
      var inUrFaceUrl = '/assets/opera-websocket.html';
    } else {
      var inUrFaceUrl = '/assets/browser.html';
    }
    $.ajax(inUrFaceUrl, { success: function(html) { $('body').prepend(html); } });
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
  }, 1000);

  $('input.lichess_id_input').select();

  if ($board = $('div.with_marks').orNot()) {
    $.displayBoardMarks($board.parent(), $('#lichess > div.lichess_player_white').length);
  }

  // themepicker
  var $body = $('body');
  var $themes = $('#top div.themepicker div.theme');
  var themes = $.makeArray($themes.map(function() { return $(this).data("theme"); }));
  var theme = $.map(document.body.className.split(/\s+/), function(a){return $.inArray(a, themes) < 0 ? null : a;})[0];
  $themes.hover(function() {
    $body.removeClass(themes.join(' ')).addClass($(this).data("theme"));
  }, function() {
    $body.removeClass(themes.join(' ')).addClass(theme);
  }).click(function() {
    theme = $(this).data("theme");
    $.post($(this).parent().data("href"), {"theme": theme});
    $('#top .themepicker').removeClass("shown");
  });

  // bgpicker
  var bgs = ["light", "dark"];
  var bg = $body.hasClass("dark") ? "dark" : "light";
  function invertBg(bg) { return bg == "dark" ? "light" : "dark"; }
  $('#top a.bgpicker').click(function() {
    bg = invertBg(bg);
    $body.removeClass(bgs.join(' ')).addClass(bg);
    $.post($(this).attr('href'), {bg: bg});
    return false;
  });

  $.centerOverboard = function() {
    if ($overboard = $('div.lichess_overboard.auto_center').orNot()) {
      $overboard.css('top', (238 - $overboard.height() / 2) + 'px').show();
    }
  };
  $.centerOverboard();

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
      img: "/assets/images/hloader3.gif",
      finishedMsg: "---"
      }
    }, function() {
      $("#infscr-loading").remove();
      $('body').trigger('lichess.content_loaded');
    }).find('div.pager').hide();
  });

  $('#top a.toggle').each(function() {
    var $this = $(this);
    var $p = $this.parent();
    $this.click(function() {
      $p.toggleClass('shown');
      setTimeout(function() {
        $p.click(function(e) { e.stopPropagation(); });
        $('html').one('click', function(e) { $p.removeClass('shown').off('click'); });
      }, 10);
    });
  });

  $('#lichess_translation_form_code').change(function() {
    if ("0" != $(this).val()) {
      location.href = $(this).closest('form').attr('data-change-url').replace(/__/, $(this).val());
    }
  });

  $('#incomplete_translation a.close').one('click', function() {
    $(this).parent().remove();
  });

  $('a.translation_call .close').click(function() {
    $.post($(this).data("href"));
    $(this).parent().fadeOut(500);
    return false;
  });

  $('a.delete, input.delete').click(function() {
    return confirm('Delete?');
  });
  $('input.confirm').click(function() {
    return confirm('Confirm this action?');
  });
  $('a.ipban').one("click", function() {
    var $a = $(this);
    if (confirm($a.text() + "?")) {
      $.post($a.attr('href'), function() {
        $a.text('Done').attr('href', '#');
      });
    }
    return false;
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

  if ($(window).width() < 1060) {
    $("div.lichess_chat").addClass("small_chat");
  }

  $("a.view_pgn_toggle").one("click", function() {
    var $this = $(this).text("...");
    $.ajax({
      url: $this.attr("href"),
      success: function(text) {
        $this.after("<textarea readonly>" + text + "</textarea>").text("Download PGN");
      }
    });
    return false;
  });

  $("div.notifications").on("click", "div.notification a", function(e) {
    var $a = $(this);
    var $notif = $a.closest("div.notification");
    var follow = !$a.hasClass("close");
    $.ajax($notif.find("a.close").attr("href"), {
      type: "delete",
      success: function() {
        if (follow) location.href = $a.attr("href");
      }
    });
    $notif.remove();
    return false;
  });

  $("form.request_analysis a").click(function() {
    $(this).parent().submit();
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
