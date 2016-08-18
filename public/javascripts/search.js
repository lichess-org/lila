$(function() {

  var $form = $("form.search");
  var $usernames = $form.find(".usernames input");
  var $userRows = $form.find(".user_row");
  var $result = $(".search_result");
  var playersRegexp = /^\??(?:.*&?players.winner=([\w-\+]+))?(?:&?players.white=([\w-\+]+))?(?:&?players.black=([\w-\+]+))?.*$/g;
  var match = playersRegexp.exec(window.location.search);
  var $playersWinner = isPlayerChosen(match[1]);
  var $playersWhite = isPlayerChosen(match[2]);
  var $playersBlack = isPlayerChosen(match[3]);

  var serialize = function(all) {
    var sel = $form.find(":input");
    return (all ? sel : sel.not('[type=hidden]')).filter(function() {
      return !!this.value;
    }).serialize()
  };

  lichess.pubsub.emit('content_loaded')();
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

  function userChoices(row) {
    var options = ["<option value=''></option>"];
    $usernames.each(function() {
      var user = $.trim($(this).val());
      if (user.length) {
        var option = [];
        option.push("<option value='" + user + "'");
        option.push(isSelected(row, "winner", user, $playersWinner));
        option.push(isSelected(row, "whiteUser", user, $playersWhite));
        option.push(isSelected(row, "blackUser", user, $playersBlack));
        option.push(">" + user + "</option>");
        options.push(option.join(""));
      }
    });
    $(row).find('select').html(options.join(""));
    $(row).toggle(options.length > 1);
  }

  function isPlayerChosen(match) {
    return typeof match !== "undefined" ? match.replace(/\+/g, " ") : "";
  }

  function isSelected(row, rowClassName, user, player) {
    return (row.classList.contains(rowClassName) && player.length && user == player) ? "selected" : ""
  }

  $usernames.bind("keyup", function() {
    $userRows.each(function() {
      userChoices(this);
    });
  }).trigger("keyup");

  var toggleAiLevel = function() {
    $form.find(".opponent select").each(function() {
      $form.find(".aiLevel").toggle($(this).val() == 1);
      $form.find(".opponentName").toggle($(this).val() != 1);
    });
  };
  toggleAiLevel();
  $form.find(".opponent select").change(toggleAiLevel);

  $form.submit(function() {
    $(this).addClass('searching');
  });
});
