// tournament
$(function() {

  var $wrap = $('#tournament');
  var $userTag = $('#user_tag');
  if (!$wrap.length) return;

  if (!$.websocket.available) return;
  if (typeof _ld_ == "undefined") {
    // handle tournament list
    lichess.socketDefaults.params.flag = "tournament";
    lichess.socketDefaults.events.reload = function() {
      $wrap.load($wrap.data("href"), function() {
        $('body').trigger('lichess.content_loaded');
      });
    };
    return;
  }

  var $chat = $("div.lichess_chat");
  var $chatToggle = $chat.find('input.toggle_chat');
  var chatExists = $chat.length > 0;
  var $userList = $wrap.find("div.user_list");
  var socketUrl = $wrap.data("socket-url");
  var $watchers = $("div.watchers").watchers();

  if (chatExists) {
    var $form = $chat.find('form');
    var $msgs = $chat.find('.lichess_messages');
    $msgs.scrollable();
    $msgs[0].scrollTop = 9999999;
    var $input = $chat.find('input.lichess_say').one("focus", function() {
      $input.val('').removeClass('lichess_hint');
    });

    // send a message
    $form.submit(function() {
      if ($input.hasClass("lichess_hint")) return false;
      var text = $.trim($input.val());
      if (!text) return false;
      if (text.length > 140) {
        alert('Max length: 140 chars. ' + text.length + ' chars used.');
        return false;
      }
      $input.val('');
      lichess.socket.send('talk', { txt: text });
      return false;
    });
    $chat.find('a.send').click(function() { $input.trigger('click'); $form.submit(); });

    // toggle the chat
    $chatToggle.change(function() {
      var enabled = $chatToggle.is(':checked');
      $chat.toggleClass('hidden', !enabled);
      $.post($chatToggle.data('href'), {"chat": enabled});
    });
    if (!$chatToggle.data("enabled")) {
      $chat.addClass('hidden');
    }
    $chatToggle[0].checked = $chatToggle.data("enabled");
  }

  function addToChat(html) {
    $chat.find('.lichess_messages').append(html)[0].scrollTop = 9999999;
    $('body').trigger('lichess.content_loaded');
  }

  function startClock() {
    $("span.tournament_clock").each(function() {
      $(this).clock({time: $(this).data("time")}).clock("start");
    });
  }
  startClock();

  function reload() {
    $wrap.load($wrap.data("href"), function() {
      startClock();
      $('body').trigger('lichess.content_loaded');
    });
  }

  lichess.socket = new $.websocket(lichess.socketUrl + socketUrl, _ld_.version, $.extend(true, lichess.socketDefaults, {
    events: {
      talk: function(e) { if (chatExists) addToChat(e); },
    reload: reload,
    reloadPage: function() {
      location.reload();
    },
    redirect: function(e) {
      location.href = 'http://'+location.hostname+'/'+e;
    },
    crowd: function(data) {
      $watchers.watchers("set", data);
    }
    },
    options: {
      name: "tournament"
    }
  }));
});
