window.onload = function () {
  var opts = lishogi_challenge;
  var selector = ".challenge-page";
  var element = document.querySelector(selector);
  var challenge = opts.data.challenge;
  var accepting;

  lishogi.socket = new lishogi.StrongSocket(
    opts.socketUrl,
    opts.data.socketVersion,
    {
      options: {
        name: "challenge",
      },
      events: {
        reload: function () {
          $.ajax({
            url: opts.xhrUrl,
            success: function (html) {
              $(selector).replaceWith($(html).find(selector));
              init();
            },
          });
        },
      },
    }
  );

  function init() {
    if (!accepting)
      $("#challenge-redirect").each(function () {
        location.href = $(this).attr("href");
      });
    $(selector)
      .find("form.accept")
      .submit(function () {
        accepting = true;
        $(this).html('<span class="ddloader"></span>');
      });
    $(selector)
      .find("form.xhr")
      .submit(function (e) {
        e.preventDefault();
        $.ajax(lishogi.formAjax($(this)));
        $(this).html('<span class="ddloader"></span>');
      });
    $(selector)
      .find("input.friend-autocomplete")
      .each(function () {
        var $input = $(this);
        lishogi.userAutocomplete($input, {
          focus: 1,
          friend: 1,
          tag: "span",
          onSelect: function () {
            $input.parents("form").submit();
          },
        });
      });
  }

  init();

  function pingNow() {
    if (document.getElementById("ping-challenge")) {
      try {
        lishogi.socket.send("ping");
      } catch (e) {}
      setTimeout(pingNow, 2000);
    }
  }

  pingNow();
};
