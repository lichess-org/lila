$(function() {

  var $form = $("form.search");
  var $usernames = $form.find(".usernames input");
  var $userRows = $form.find(".user_row");
  var $result = $(".search_result");

  var serialize = function(all) {
    var sel = $form.find(":input");
    return (all ? sel : sel.not('[type=hidden]')).filter(function() {
      return !!this.value;
    }).serialize()
  };

  var onResultLoad = function() {
    $('body').trigger('lichess.content_loaded');
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
        $('body').trigger('lichess.content_loaded');
      });
    });
  };
  onResultLoad();

  function realtimeResults() {
    $("div.search_status").text("Searching...");
    $result.load(
      $form.attr("action") + "?" + serialize() + " .search_result", function(text, status) {
        if (status == "error") $(".search_status").text("Something is wrong with the search engine!");
        else onResultLoad();
      });
  }

  function userChoices(row) {
    var options = ["<option value=''></option>"];
    $usernames.each(function() {
      var user = $.trim($(this).val());
      if (user.length) options.push("<option value='" + user + "'>" + user + "</option>");
    });
    $(row).find('select').html(options.join(""));
    $(row).toggle(options.length > 1);
  }

  $form.find("select, input[type=checkbox]").change(realtimeResults);
  $usernames.bind("keyup", function() {
    $userRows.each(function() {
      userChoices(this);
    });
  }).trigger("keyup");
  $usernames.bindWithDelay("keyup", realtimeResults, 700);

  var toggleAiLevel = function() {
    $form.find(".opponent select").each(function() {
      $form.find(".aiLevel").toggle($(this).val() == 1);
      $form.find(".opponentName").toggle($(this).val() != 1);
    });
  };
  toggleAiLevel();
  $form.find(".opponent select").change(toggleAiLevel);
});

// https://github.com/bgrins/bindWithDelay/blob/master/bindWithDelay.js
$.fn.bindWithDelay = function(type, data, fn, timeout, throttle) {

  if ($.isFunction(data)) {
    throttle = timeout;
    timeout = fn;
    fn = data;
    data = undefined;
  }

  // Allow delayed function to be removed with fn in unbind function
  fn.guid = fn.guid || ($.guid && $.guid++);

  // Bind each separately so that each element has its own delay
  return this.each(function() {

    var wait = null;

    function cb() {
      var e = $.extend(true, {}, arguments[0]);
      var ctx = this;
      var throttler = function() {
        wait = null;
        fn.apply(ctx, [e]);
      };

      if (!throttle) {
        clearTimeout(wait);
        wait = null;
      }
      if (!wait) {
        wait = setTimeout(throttler, timeout);
      }
    }

    cb.guid = fn.guid;

    $(this).bind(type, data, cb);
  });
};
