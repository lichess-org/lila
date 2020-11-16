var tablesort = require("tablesort");

let $toggle = $(".mod-zone-toggle");
let $zone = $(".mod-zone");
let nbOthers = 100;

function streamLoad() {
  const source = new EventSource(
    $toggle.attr("href") + "?nbOthers=" + nbOthers
  );
  const callback = lishogi.debounce(() => userMod($zone), 300);
  source.addEventListener("message", (e) => {
    if (!e.data) return;
    const html = $("<output>").append($.parseHTML(e.data));
    html.find(".mz-section").each(function () {
      const prev = $("#" + this.id);
      if (prev.length) prev.replaceWith($(this));
      else $zone.append($(this).clone());
    });
    callback();
  });
  source.onerror = () => source.close();
}

function loadZone() {
  $zone.html(lishogi.spinnerHtml).removeClass("none");
  $("#main-wrap").addClass("full-screen-force");
  $zone.html("");
  streamLoad();
  window.addEventListener("scroll", onScroll);
  scrollTo(".mod-zone");
}
function unloadZone() {
  $zone.addClass("none");
  $("#main-wrap").removeClass("full-screen-force");
  window.removeEventListener("scroll", onScroll);
  scrollTo("#top");
}
function reloadZone() {
  streamLoad();
}

function scrollTo(el) {
  const offset = $("#inquiry").length ? -50 : 50;
  window.scrollTo(0, document.querySelector(el).offsetTop + offset);
}

$toggle.click(function () {
  if ($zone.hasClass("none")) loadZone();
  else unloadZone();
  return false;
});

function userMod($zone) {
  lishogi.pubsub.emit("content_loaded");

  $("#mz_menu > a:not(.available)").each(function () {
    $(this).toggleClass("available", !!$($(this).attr("href")).length);
  });
  makeReady("#mz_menu", (el) => {
    $(el)
      .find("a")
      .each(function (i) {
        const id = this.href.replace(/.+(#\w+)$/, "$1"),
          n = "" + (i + 1);
        $(this).prepend(`<i>${n}</i>`);
        Mousetrap.bind(n, () => scrollTo(id));
      });
  });

  makeReady("form.xhr", (el) => {
    $(el).submit(() => {
      $(el).addClass("ready").find("input").attr("disabled", true);
      $.ajax({
        ...lishogi.formAjax($(el)),
        success: function (html) {
          $("#mz_actions").replaceWith(html);
          userMod($zone);
        },
      });
      return false;
    });
  });

  makeReady("form.fide_title select", (el) => {
    $(el).on("change", function () {
      $(el).parent("form").submit();
    });
  });

  makeReady("#mz_others", (el) => {
    $(el).height($(el).height());
    $(el)
      .find(".mark-alt")
      .on("click", function () {
        if (confirm("Close alt account?")) {
          $.post(this.getAttribute("href"));
          $(this).remove();
        }
      });
  });
  makeReady("#mz_others table", (el) => {
    tablesort(el, { descending: true });
  });
  makeReady("#mz_identification .spy_filter", (el) => {
    $(el)
      .find(".button")
      .click(function () {
        $.post($(this).attr("href"));
        $(this).parent().parent().toggleClass("blocked");
        return false;
      });
    $(el)
      .find("tr")
      .on("mouseenter", function () {
        const v = $(this).find("td:first").text();
        $("#mz_others tbody tr").each(function () {
          $(this).toggleClass(
            "none",
            !($(this).data("tags") || "").includes(v)
          );
        });
      });
    $(el).on("mouseleave", function () {
      $("#mz_others tbody tr").removeClass("none");
    });
  });
  makeReady(
    "#mz_identification .slist--sort",
    (el) => {
      tablesort(el, { descending: true });
    },
    "ready-sort"
  );
  makeReady("#mz_others .more-others", (el) => {
    $(el)
      .addClass(".ready")
      .click(() => {
        nbOthers = 1000;
        reloadZone();
      });
  });
}

function makeReady(selector, f, cls) {
  cls = cls || "ready";
  $zone.find(selector + `:not(.${cls})`).each(function (i) {
    f($(this).addClass(cls)[0], i);
  });
}

const onScroll = (e) =>
  requestAnimationFrame(() => {
    if ($zone.hasClass("none")) return;
    $zone.toggleClass("stick-menu", window.scrollY > 200);
  });

(function () {
  var cleanNumber = function (i) {
      return i.replace(/[^\-?0-9.]/g, "");
    },
    compareNumber = function (a, b) {
      a = parseFloat(a);
      b = parseFloat(b);

      a = isNaN(a) ? 0 : a;
      b = isNaN(b) ? 0 : b;

      return a - b;
    };

  tablesort.extend(
    "number",
    function (item) {
      return item.match(
        /^[-+]?(\d)*-?([,\.]){0,1}-?(\d)+([E,e][\-+][\d]+)?%?$/
      ); // Number
    },
    function (a, b) {
      return compareNumber(cleanNumber(b), cleanNumber(a));
    }
  );
})();

if (location.search.startsWith("?mod")) $toggle.click();
Mousetrap.bind("m", () => $toggle.click());
