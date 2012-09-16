$(function() {

  var $startButtons = $('#start_buttons');

  if (!$.websocket.available) {
    $('#start_buttons a').attr('href', '#');
    $("div.lichess_overboard.joining input.submit").remove();
    return;
  }

  if (!$startButtons.length) {
    return;
  }

  function prepareForm() {
    var $form = $('div.lichess_overboard');
    var $modeChoices = $form.find('.mode_choice input');
    var $casual = $modeChoices.eq(0), $rated = $modeChoices.eq(1);
    var $clockCheckbox = $form.find('.clock_choice input');
    var isHook = $form.hasClass('game_config_hook');
    $form.find('div.buttons').buttonset().disableSelection();
    $form.find('button.submit').button().disableSelection();
    $form.find('.time_choice input, .increment_choice input').each(function() {
      var $input = $(this), $value = $input.parent().find('span');
      var $timeInput = $form.find('.time_choice input');
      var $incrementInput = $form.find('.increment_choice input');
      $input.hide().after($('<div>').slider({
        value: $input.val(),
        min: $input.data('min'),
        max: $input.data('max'),
        range: 'min',
        step: 1,
        slide: function( event, ui ) {
          $value.text(ui.value);
          $input.attr('value', ui.value);
          $form.find('.color_submits button').toggle(
            $timeInput.val() > 0 || $incrementInput.val() > 0
            );
        }
      }));
    });
    $form.find('.elo_range').each(function() {
      var $this = $(this);
      var $input = $this.find("input");
      var $span = $this.parent().find("span.range");
      var min = $input.data("min");
      var max = $input.data("max");
      if ($input.val()) {
        var values = $input.val().split("-");
      } else {
        var values = [min, max];
      }
      $span.text(values.join(' - '));
      $this.slider({
        range: true,
        min: min,
        max: max,
        values: values,
        step: 50,
        slide: function( event, ui ) {
          $input.val(ui.values[0] + "-" + ui.values[1]);
          $span.text(ui.values[0] + " - " + ui.values[1]);
        }
      });
      var $eloRangeConfig = $this.parent();
      $modeChoices.on('change', function() {
        var rated = $rated.attr('checked') == 'checked';
        $eloRangeConfig.toggle(rated);
        if (isHook && rated && $clockCheckbox.attr('checked') != 'checked') {
          $clockCheckbox.click();
        }
        $.centerOverboard();
      }).trigger('change');
    });
    $clockCheckbox.on('change', function() {
      var checked = $(this).is(':checked');
      $form.find('.time_choice, .increment_choice').toggle(checked);
      if (isHook && !checked) {
        $casual.click();
      }
      $.centerOverboard();
    }).trigger('change');
    var $eloRangeConfig = $form.find('.elo_range_config');
    $form.prepend($('<a class="close"></a>').click(function() {
      $form.remove();
      $startButtons.find('a.active').removeClass('active');
    }));
  }

  $startButtons.find('a').click(function() {
    $startButtons.find('a.active').removeClass('active');
    $(this).addClass('active');
    $('div.lichess_overboard').remove();
    $.ajax({
      url: $(this).attr('href'),
      success: function(html) {
        $('div.lichess_overboard').remove();
        $('div.lichess_board_wrap').prepend(html);
        prepareForm();
        $.centerOverboard();
      }
    });
    return false;
  });
  $('#lichess').on('submit', 'form', $.lichessOpeningPreventClicks);

  if (window.location.hash) {
    $startButtons.find('a.config_'+window.location.hash.replace(/#/, '')).click();
  }
});

$.lichessOpeningPreventClicks = function() {
  $('div.lichess_overboard, div.hooks_wrap').hide();
};

// hooks
$(function() {

  var $wrap = $('div.hooks_wrap');
  if (!$wrap.length) return;
  if (!$.websocket.available) return;

  var $chat = $("div.lichess_chat");
  var $chatToggle = $chat.find('input.toggle_chat');
  var chatExists = $chat.length > 0;
  var $bot = $("div.lichess_bot");
  var $newposts = $("div.new_posts");
  var $newpostsinner = $newposts.find('.undertable_inner');
  var $hooks = $wrap.find('div.hooks');
  var $hooksTable = $hooks.find("table");
  var actionUrls = {
    'cancel': $hooks.data('cancel-url'),
  'join': $hooks.data('join-url')
  };
  var $userTag = $('#user_tag');
  var isRegistered = $userTag.length > 0
  var myElo = isRegistered ? parseInt($userTag.data('elo')) : null;
  var hookOwnerId = $hooks.data('my-hook');

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
    var html = '<li><span>';
    html += '<a class="user_link" href="/@/'+username+'">'+username.substr(0, 12) + '</a>';
    html += '</span>' + urlToLink(txt) + '</li>';
    return html;
  }

  $bot.on("click", "tr", function() { location.href = $(this).find('a.watch').attr("href"); });
  $bot.find('.undertable_inner').scrollable();
  $newpostsinner.scrollable();
  $newpostsinner[0].scrollTop = 9999999;
  $newpostsinner.scrollable();
  setInterval(function() {
    $.ajax($newposts.data('url'), {
      timeout: 10000,
      success: function(data) {
        $newpostsinner.find('ol').html(data);
        $newpostsinner[0].scrollTop = 9999999;
        $('body').trigger('lichess.content_loaded');
      }
    });
  }, 120 * 1000);

  addHooks(lichess_preload.pool);
  renderTimeline(lichess_preload.timeline);
  if (chatExists) {
    var chatHtml = "";
    $.each(lichess_preload.chat, function() {
      if (this.txt) chatHtml += buildChatMessage(this.txt, this.u);
    });
    addToChat(chatHtml);
  }
  lichess.socket = new $.websocket(lichess.socketUrl + "/lobby/socket", lichess_preload.version, $.extend(true, lichess.socketDefaults, {
    params: {
      hook: hookOwnerId
    },
    events: {
      talk: function(e) { if (chatExists && e.txt) addToChat(buildChatMessage(e.txt, e.u)); },
    entry: function(e) { renderTimeline([e]); },
    hook_add: addHook,
    hook_remove: removeHook,
    featured: changeFeatured,
    redirect: function(e) {
      $.lichessOpeningPreventClicks();
      location.href = 'http://'+location.hostname+'/'+e;
    },
    tournaments: reloadTournaments
    },
    options: {
      name: "lobby"
    }
  }));
  $('body').trigger('lichess.content_loaded');

  function reloadTournaments(data) {
    $("table.tournaments").html(data);
  }

  function changeFeatured(data) {
    $('div.featured_game').each(function() {
      var $featured = $(this);
      $.get(
        $featured.data("href"),
        { id: data.newId },
        function(html) {
          $featured.replaceWith(html);
          $('body').trigger('lichess.content_loaded');
        }
        );
    });
  }

  function renderTimeline(data) {
    var html = "";
    for (i in data) { html += '<tr>' + data[i] + '</tr>'; }
    $bot.find('.lichess_messages').append(html).parent()[0].scrollTop = 9999999;
    $('body').trigger('lichess.content_loaded');
  }

  function removeHook(id) {
    $("#" + id).find('td.action').addClass('empty').html("").end().fadeOut(500, function() {
      $(this).remove();
      updateHookTable();
    });
  }
  function addHooks(hooks) {
    var html = "";
    for (i in hooks) html += renderHook(hooks[i]);
    $hooksTable.append(html);
    updateHookTable();
  }
  function addHook(hook) {
    $hooksTable.append(renderHook(hook));
    updateHookTable();
  }
  function updateHookTable() {
    if (0 == $hooksTable.find('tr.hook').length) {
      $hooksTable.addClass('empty_table').html('<tr class="create_game"><td colspan="5">'+$.trans("No game available right now, create one!")+'</td></tr>');
    } else {
      $hooksTable.removeClass('empty_table').find('tr.create_game').remove();
    }
    resizeLobby();
    $hooksTable.find('a.join').click($.lichessOpeningPreventClicks);
  }

  function renderHook(hook) {
    if (!isRegistered && hook.mode == "Rated") return "";
    var html = "", isEngine, engineMark, userClass, mode, eloRestriction;
    hook.action = hook.ownerId ? "cancel" : "join";
    html += '<tr id="'+hook.id+'" class="hook'+(hook.action == 'join' ? ' joinable' : '')+'">';
    html += '<td class="color"><span class="'+hook.color+'"></span></td>';
    isEngine = hook.engine && hook.action == 'join';
    engineMark = isEngine ? '<span class="engine_mark"></span>' : '';
    userClass = isEngine ? "user_link engine" : "user_link";
    if (hook.elo) {
      html += '<td><a class="'+userClass+'" href="/@/'+hook.username+'">'+hook.username.substr(0, 12)+'<br />'+'('+hook.elo+')'+engineMark+'</a></td>';
    } else {
      html += '<td>'+hook.username+'</td>';
    }
    html += '</td>';
    eloRestriction = false;
    if (isRegistered) {
      mode = $.trans(hook.mode);
      if (hook.emin) {
        if (hook.action == "join" && (myElo < parseInt(hook.emin) || myElo > parseInt(hook.emax))) {
          eloRestriction = true;
        }
        if (hook.emin > 800 || hook.emax < 2500) {
          mode += "<span class='elorange" + (eloRestriction ? ' nope' : '') + "'>" + hook.emin + ' - ' + hook.emax + '</span>';
        }
      }
    } else {
      mode = "";
    }
    if (hook.variant == 'Chess960') {
      html += '<td><a href="http://en.wikipedia.org/wiki/Chess960"><strong>960</strong></a> ' + mode + '</td>';
    } else {
      html += '<td>'+mode+'</td>';
    }
    html += '<td>'+$.trans(hook.clock)+'</td>';
    if (eloRestriction) {
      html += '<td class="action empty"></td>';
    } else {
      html += '<td class="action">';
      if (hook.action == "cancel") {
        html += '<a href="'+actionUrls.cancel.replace(/\/0{12}/, '/'+hook.ownerId)+'" class="cancel"></a>';
      } else {
        var cancelParam = hookOwnerId ? "?cancel=" + hookOwnerId : ""
          html += '<a href="'+actionUrls.join.replace(/\/0{8}/, '/'+hook.id)+cancelParam+'" class="join"></a>';
      }
    }
    return html;
  }

  function resizeLobby() {
    $wrap.toggleClass("large", $hooks.find("tr").length > 6);
  }

  $hooks.on('click', 'table.empty_table tr', function() {
    $('#start_buttons a.config_hook').click();
  });
});
