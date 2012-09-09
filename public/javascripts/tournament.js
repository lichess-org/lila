// tournament
$(function() {

  var $wrap = $('div.tournament_show');
  if (!$wrap.length) return;
  if (!$.websocket.available) return;

  var $chat = $("div.lichess_chat");
  var $chatToggle = $chat.find('input.toggle_chat');
  var chatExists = $chat.length > 0;
  var websocketUrl = $wrap.data("socket-url");

  if (chatExists) {
    var $form = $chat.find('form');
    $chat.find('.lichess_messages').scrollable();
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
  function buildChatMessage(txt, username) {
    var html = '<li><span>'
      html += '<a class="user_link" href="/@/'+username+'">'+username.substr(0, 12) + '</a>';
    html += '</span>' + urlToLink(txt) + '</li>';
    return html;
  }

  lichess.socket = new $.websocket(lichess.socketUrl + socketUrl, lichess_preload.version, $.extend(true, lichess.socketDefaults, {
    events: {
      talk: function(e) { if (chatExists && e.txt) addToChat(buildChatMessage(e.txt, e.u)); }
    },
    options: {
      name: "tournament"
    }
  }));
  $('body').trigger('lichess.content_loaded');
});
