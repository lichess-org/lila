$(function() {

  var $form = $("form.search");
  var $usernames = $form.find(".usernames input");
  var $userRows = $form.find(".user_row");
  var $result = $(".search_result");

  function getUsernames() {
    var us = [];
    $usernames.each(function() {
      var u = $.trim($(this).val());
      if (u) us.push(u);
    });
    return us;
  }

  function userChoices(row) {
    var options = ["<option value=''></option>"];
    var isSelected = function(row, rowClassName, user, dataKey) {
      var player = $form.data(dataKey);
      return (row.classList.contains(rowClassName) && player.length && user == player) ? "selected" : ""
    }
    getUsernames().forEach(function(user) {
      var option = [];
      option.push("<option value='" + user + "'");
      option.push(isSelected(row, "winner", user, 'req-winner'));
      option.push(isSelected(row, "whiteUser", user, 'req-white'));
      option.push(isSelected(row, "blackUser", user, 'req-black'));
      option.push(">" + user + "</option>");
      options.push(option.join(""));
    });
    $(row).find('select').html(options.join(""));
    $(row).toggle(options.length > 1);
  }

  function reloadUserChoices() {
    $userRows.each(function() {
      userChoices(this);
    });
  }
  reloadUserChoices();
  $usernames.bind("input paste", reloadUserChoices);

  var toggleAiLevel = function() {
    $form.find(".opponent select").each(function() {
      $form.find(".aiLevel").toggle($(this).val() == 1);
      $form.find(".opponentName").toggle($(this).val() != 1);
    });
  };
  toggleAiLevel();
  $form.find(".opponent select").change(toggleAiLevel);

  var serialize = function(all) {
    var sel = $form.find(":input");
    return (all ? sel : sel.not('[type=hidden]')).filter(function() {
      return !!this.value;
    }).serialize()
  };

  var serialized = serialize();
  $result.find("a.permalink").each(function() {
    var s = $(this).hasClass('download') ? serialize(true) : serialized;
    $(this).attr("href", $(this).attr("href").split('?')[0] + "?" + s);
  });
  $result.find('.search_infinitescroll:has(.pager a)').each(function() {
    var $next = $(this).find(".pager a:last");
    $next.attr("href", $next.attr("href") + "&" + serialized);
    $(this).infinitescroll({
      navSelector: ".pager",
      nextSelector: $next,
      itemSelector: ".search_infinitescroll .paginated_element",
      loading: {
        msgText: "",
        finishedMsg: "---"
      }
    }, function() {
      $("#infscr-loading").remove();
      lichess.pubsub.emit('content_loaded')();
    });
  });

  $form.submit(function() {
    $(this).addClass('searching');
  });

  if ($form.hasClass('realtime')) {
    var submit = function() {
      $form.submit();
    };
    $form.find("select, input[type=checkbox]").change(submit);
    $usernames.bind("keyup", $.fp.debounce(submit, 1500));
  }
});
