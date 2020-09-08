$(function() {

  var $form = $(".search__form");
  var $usernames = $form.find(".usernames input");
  var $userRows = $form.find(".user-row");
  var $result = $(".search__result");

  function getUsernames() {
    var us = [];
    $usernames.each(function() {
      var u = $(this).val().trim();
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
      option.push(isSelected(row, "loser", user, 'req-loser'));
      option.push(isSelected(row, "whiteUser", user, 'req-white'));
      option.push(isSelected(row, "blackUser", user, 'req-black'));
      option.push(">" + user + "</option>");
      options.push(option.join(""));
    });
    $(row).find('select').html(options.join(""));
    row.classList.toggle('none', options.length < 2);
  }

  function reloadUserChoices() {
    $userRows.each(function() {
      userChoices(this);
    });
  }
  reloadUserChoices();
  $usernames.on("input paste", reloadUserChoices);

  var toggleAiLevel = function() {
    $form.find(".opponent select").each(function() {
      $form[0].querySelector('.aiLevel')?.classList.toggle('none', $(this).val() != 1);
      $form[0].querySelector('.opponentName')?.classList.toggle('none', $(this).val() == 1);
    });
  };
  toggleAiLevel();
  $form.find(".opponent select").change(toggleAiLevel);

  function serialize() {
    const data = new FormData($form[0]),
    params = new URLSearchParams(data),
    keys = Array.from(params.keys());
    for (let k of keys) {
      if (params.get(k) == '') params.delete(k);
    }
    return params.toString();
  }

  const serialized = serialize();
  $result.find("a.permalink").each(function() {
    $(this).attr("href", $(this).attr("href").split('?')[0] + "?" + serialized);
  });
  $result.find('.search__rows').each(function() {
    var $next = $(this).find(".pager a");
    if (!$next.length) return;
    $next.attr("href", $next.attr("href") + "&" + serialized);
    $(this).infinitescroll({
      navSelector: ".pager",
      nextSelector: $next,
      itemSelector: ".search__rows .paginated",
      loading: {
        msgText: "",
        finishedMsg: "---"
      }
    }, function() {
      $("#infscr-loading").remove();
      lichess.pubsub.emit('content_loaded');
    });
  });

  $form.submit(function() {
    $form.find("input,select").filter(function() { return !this.value; }).attr("disabled", "disabled");
    $form.addClass('searching');
  });
});
