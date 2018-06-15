/** based on https://github.com/hustcc/timeago.js Copyright (c) 2016 hustcc License: MIT **/
lichess.timeago = (function() {

  // second, minute, hour, day, week, month, year(365 days)
  var SEC_ARRAY = [60, 60, 24, 7, 365/7/12, 12],
    SEC_ARRAY_LEN = 6;

  // format Date / string / timestamp to Date instance.
  function toDate(input) {
    return input instanceof Date ? input : (
      new Date(isNaN(input) ? input : parseInt(input))
    );
  }

  // format the diff second to *** time ago
  function formatDiff(diff) {
    var i = 0, agoin = diff < 0 ? 1 : 0, // timein or timeago
      total_sec = diff = Math.abs(diff);

    for (; diff >= SEC_ARRAY[i] && i < SEC_ARRAY_LEN; i++) {
      diff /= SEC_ARRAY[i];
    }
    diff = parseInt(diff);
    i *= 2;

    if (diff > (i === 0 ? 9 : 1)) i += 1;
    return lichess.timeagoLocale(diff, i, total_sec)[agoin].replace('%s', diff);
  }

  var formatterInst;

  function formatter() {
    return formatterInst = formatterInst || (window.Intl && Intl.DateTimeFormat ?
      new Intl.DateTimeFormat(document.documentElement.lang, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: 'numeric'
      }).format : function(d) { return d.toLocaleString(); })
  }

   return {
     render: function(nodes) {
       var cl, abs, set, str, diff, now = Date.now();
       nodes.forEach(function(node) {
         cl = node.classList,
         abs = cl.contains('abs'),
         set = cl.contains('set');
         node.date = node.date || toDate(node.getAttribute('datetime'));
         if (!set) {
           str = formatter()(node.date);
           if (abs) node.textContent = str;
           else node.setAttribute('title', str);
           cl.add('set');
           if (abs || cl.contains('once')) cl.remove('timeago');
         }
         if (!abs) {
           diff = (now - node.date) / 1000;
           node.textContent = formatDiff(diff);
           if (Math.abs(diff) > 9999) cl.remove('timeago'); // ~3h
         }
       });
     },
     // relative
     format: function(date) {
       return formatDiff((Date.now() - toDate(date)) / 1000);
     },
     absolute: function(date) {
       return formatter()(toDate(date));
     }
   };
})();
