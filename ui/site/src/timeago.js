/**
 * based on https://github.com/hustcc/timeago.js
 * Copyright (c) 2016 hustcc
 * License: MIT
**/
window.timeago = (function() {
    // second, minute, hour, day, week, month, year(365 days)
    var SEC_ARRAY = [60, 60, 24, 7, 365/7/12, 12],
    SEC_ARRAY_LEN = 6;

  // format Date / string / timestamp to Date instance.
  function toDate(input) {
    if (input instanceof Date) return input;
    if (!isNaN(input)) return new Date(parseInt(input));
    if (/^\d+$/.test(input)) return new Date(parseInt(input));
    input = (input || '').trim().replace(/\.\d+/, '') // remove milliseconds
      .replace(/-/, '/').replace(/-/, '/')
      .replace(/(\d)T(\d)/, '$1 $2').replace(/Z/, ' UTC') // 2017-2-5T3:57:52Z -> 2017-2-5 3:57:52UTC
      .replace(/([\+\-]\d\d)\:?(\d\d)/, ' $1$2'); // -04:00 -> -0400
    return new Date(input);
  }
  // format the diff second to *** time ago
  function formatDiff(diff) {
    var i = 0,
      agoin = diff < 0 ? 1 : 0, // timein or timeago
      total_sec = diff = Math.abs(diff);

    for (; diff >= SEC_ARRAY[i] && i < SEC_ARRAY_LEN; i++) {
      diff /= SEC_ARRAY[i];
    }
    diff = parseInt(diff);
    i *= 2;

    if (diff > (i === 0 ? 9 : 1)) i += 1;
    return lichess.timeagoLocale(diff, i, total_sec)[agoin].replace('%s', diff);
  }
  // calculate the diff second between date to be formated an now date.
  function diffSec(date, nowDate) {
    nowDate = nowDate ? toDate(nowDate) : new Date();
    return (nowDate - toDate(date)) / 1000;
  }

  // get the datetime attribute, `data-timeagp` / `datetime` are supported.
  function getDateAttr(node) {
    if (node.getAttribute) return node.getAttribute('datetime'); // native
    return node.attr('datetime'); // jquery
  }
  /**
   * timeago: the function to get `timeago` instance.
   * - nowDate: the relative date, default is new Date().
   *
   * How to use it?
   * var timeagoLib = require('timeago.js');
   * var timeago = timeagoLib(); // all use default.
   * var timeago = timeagoLib('2016-09-10'); // the relative date is 2016-09-10, so the 2016-09-11 will be 1 day ago.
  **/
  function Timeago(nowDate) {
    this.nowDate = nowDate;
  }
  /**
   * format: format the date to *** time ago
   * - date: the date / string / timestamp to be formated
   *
   * How to use it?
   * var timeago = require('timeago.js')();
   * timeago.format(new Date(), 'pl'); // Date instance
   * timeago.format('2016-09-10', 'fr'); // formated date string
   * timeago.format(1473473400269); // timestamp with ms
  **/
  Timeago.prototype.format = function(date) {
    return formatDiff(diffSec(date, this.nowDate));
  };
  /**
   * render: render the DOM real-time.
   * - nodes: which nodes will be rendered.
   *
   * How to use it?
   * var timeago = require('timeago.js')();
   * // 1. javascript selector
   * timeago.render(document.querySelectorAll('.need_to_be_rendered'));
   * // 2. use jQuery selector
   * timeago.render($('.need_to_be_rendered'));
   *
   * Notice: please be sure the dom has attribute `datetime`.
  **/
  Timeago.prototype.render = function(nodes) {
    if (nodes.length === undefined) nodes = [nodes];
    for (var i = 0, len = nodes.length; i < len; i++) {
      var node = nodes[i],
      html = formatDiff(diffSec(getDateAttr(nodes[i]), this.nowDate));
      if (node.timeagoHTML !== html) {
        node.innerHTML = html;
        node.timeagoHTML = html;
      }
    }
  };

  function timeagoFactory(nowDate) {
    return new Timeago(nowDate);
  }

  /**
   * timeago: the function to get `timeago` instance.
   * - nowDate: the relative date, default is new Date().
   *
   * How to use it?
   * var timeagoFactory = require('timeago.js');
   * var timeago = timeagoFactory(); // all use default.
   * var timeago = timeagoFactory('2016-09-10'); // the relative date is 2016-09-10, so the 2016-09-11 will be 1 day ago.
   **/
  return timeagoFactory;
})();
