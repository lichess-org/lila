/** based on https://github.com/hustcc/timeago.js Copyright (c) 2016 hustcc License: MIT **/
lichess.timeago = (function() {

  // second, minute, hour, day, week, month, year(365 days)
  var SEC_ARRAY = [60,
                   60 * 60,
                   60 * 60 * 24,
                   60 * 60 * 24 * 7,
                   60 * 60 * 2 * 365, // 24/12 = 2
                   60 * 60 * 24 * 365];

  // format Date / string / timestamp to Date instance.
  function toDate(input) {
    return input instanceof Date ? input : (
      new Date(isNaN(input) ? input : parseInt(input))
    );
  }

  // format the diff and start rounding down to days only after diff iss less than days_back
   function formatDiff(diff) {
        var i = 0, agoin = 0;
        if (diff < 0) {
            agoin = 1;
            diff = -diff;
        }
        var i = 0, total_sec = diff, days_back = 2;
        while (i < 6 && diff >= SEC_ARRAY[i]) i++;
        if ((i - 1) == 2) (diff <= days_back * SEC_ARRAY[i - 1] ? i -= 1 : i = i)
        if (i > 0) diff /= SEC_ARRAY[i - 1];

        diff = Math.floor(diff);
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
