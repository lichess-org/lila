var tournament = require('./tournament');
var m = require('mithril');

var maxPerPage = 10;

function button(text, icon, click, enable) {
  return {
    tag: 'button',
    attrs: {
      class: 'button is',
      'data-icon': icon,
      disabled: !enable,
      onclick: click,
      title: text
    },
    children: []
  };
}

function scrollToMeButton(ctrl, pag) {
  if (!tournament.containsMe(ctrl)) return;
  return m('button', {
    class: 'button text',
    'data-icon': '7',
    onclick: ctrl.scrollToMe
  }, 'Me');
}

function paginate(ctrl, page) {
  var nbResults = ctrl.data.players.length;
  var max = nbResults > 20 ? maxPerPage : 20; // don't paginate 20 or less elements
  var from = (page - 1) * max;
  var to = Math.min(nbResults, page * max);
  return {
    currentPage: page,
    maxPerPage: max,
    from: from,
    to: to,
    currentPageResults: ctrl.data.players.slice(from, to),
    nbResults: nbResults,
    nbPages: Math.ceil(nbResults / max)
  };
}

function findIndex(arr, cond) {
  for (i in arr) {
    if (cond(arr[i])) return i;
  }
}

module.exports = {
  render: function(ctrl, pag, table) {
    return [
      table,
      pag.nbPages > 1 ? m('div.pager', [
        button('First', 'W', function() {
          ctrl.vm.page = 1;
        }, ctrl.vm.page > 1),
        button('Prev', 'Y', function() {
          ctrl.vm.page--;
        }, ctrl.vm.page > 1),
        m('span.page', (pag.from + 1) + '-' + pag.to + ' / ' + pag.nbResults),
        button('Next', 'X', function() {
          ctrl.vm.page++;
        }, ctrl.vm.page < pag.nbPages),
        scrollToMeButton(ctrl, pag)
      ]) : null
    ];
  },
  players: function(ctrl) {
    return paginate(ctrl, ctrl.vm.page);
  },
  pageOfUserId: function(ctrl) {
    if (!ctrl.userId) return;
    var pos = findIndex(ctrl.data.players, function(p) {
      return p.name.toLowerCase() === ctrl.userId;
    });
    if (pos === null) return;
    return Math.floor(pos / 10) + 1;
  }
};
