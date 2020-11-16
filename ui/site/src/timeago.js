/** based on https://github.com/hustcc/timeago.js Copyright (c) 2016 hustcc License: MIT **/
lishogi.timeago = (function () {
  // divisors for minutes, hours, days, weeks, months, years
  const DIVS = [
    60,
    60 * 60,
    60 * 60 * 24,
    60 * 60 * 24 * 7,
    60 * 60 * 2 * 365, // 24/12 = 2
    60 * 60 * 24 * 365,
  ];

  const LIMITS = [...DIVS];
  LIMITS[2] *= 2; // Show hours up to 2 days.

  // format Date / string / timestamp to Date instance.
  function toDate(input) {
    return input instanceof Date
      ? input
      : new Date(isNaN(input) ? input : parseInt(input));
  }

  // format the diff second to *** time ago
  function formatDiff(diff) {
    let agoin = 0;
    if (diff < 0) {
      agoin = 1;
      diff = -diff;
    }
    var total_sec = diff;

    let i = 0;
    for (; i < 6 && diff >= LIMITS[i]; i++);
    if (i > 0) diff /= DIVS[i - 1];

    diff = Math.floor(diff);
    i *= 2;

    if (diff > (i === 0 ? 9 : 1)) i += 1;
    return lishogi.timeagoLocale(diff, i, total_sec)[agoin].replace("%s", diff);
  }

  var formatterInst;

  function formatter() {
    return (formatterInst =
      formatterInst ||
      (window.Intl && Intl.DateTimeFormat
        ? new Intl.DateTimeFormat(document.documentElement.lang, {
            year: "numeric",
            month: "short",
            day: "numeric",
            hour: "numeric",
            minute: "numeric",
          }).format
        : function (d) {
            return d.toLocaleString();
          }));
  }

  return {
    render: function (nodes) {
      var cl,
        abs,
        set,
        str,
        diff,
        now = Date.now();
      nodes.forEach(function (node) {
        (cl = node.classList),
          (abs = cl.contains("abs")),
          (set = cl.contains("set"));
        node.date = node.date || toDate(node.getAttribute("datetime"));
        if (!set) {
          str = formatter()(node.date);
          if (abs) node.textContent = str;
          else node.setAttribute("title", str);
          cl.add("set");
          if (abs || cl.contains("once")) cl.remove("timeago");
        }
        if (!abs) {
          diff = (now - node.date) / 1000;
          node.textContent = formatDiff(diff);
          if (Math.abs(diff) > 9999) cl.remove("timeago"); // ~3h
        }
      });
    },
    // relative
    format: function (date) {
      return formatDiff((Date.now() - toDate(date)) / 1000);
    },
    absolute: function (date) {
      return formatter()(toDate(date));
    },
  };
})();
