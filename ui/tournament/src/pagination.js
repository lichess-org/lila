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
  if (!ctrl.data.me) return;
  return m('button', {
    class: 'button text' + (ctrl.vm.focusOnMe ? ' active' : ''),
    'data-icon': '7',
    onclick: ctrl.toggleFocusOnMe
  }, 'Me');
}

function paginate(ctrl, page) {
  var nbResults = ctrl.data.nbPlayers;
  var max = nbResults > 15 ? maxPerPage : 15; // don't paginate 15 or less elements
  var from = (page - 1) * max;
  var to = Math.min(nbResults, page * max);
  return {
    currentPage: page,
    maxPerPage: max,
    from: from,
    to: to,
    currentPageResults: ctrl.vm.pages[page],
    nbResults: nbResults,
    nbPages: Math.ceil(nbResults / max)
  };
}

module.exports = {
  render: function(ctrl, pag, table) {
    return [
      // loader,
      pag.currentPageResults ? table() : m('div.loader'),
      pag.nbPages > 1 ? m('div.pager', [
        button('First', 'W', function() {
          ctrl.userSetPage(1);
        }, ctrl.vm.page > 1),
        button('Prev', 'Y', function() {
          ctrl.userSetPage(ctrl.vm.page - 1);
        }, ctrl.vm.page > 1),
        m('span.page', (pag.from + 1) + '-' + pag.to + ' / ' + pag.nbResults),
        button('Next', 'X', function() {
          ctrl.userSetPage(ctrl.vm.page + 1);
        }, ctrl.vm.page < pag.nbPages),
        scrollToMeButton(ctrl, pag)
      ]) : null
    ];
  },
  players: function(ctrl) {
    return paginate(ctrl, ctrl.vm.page);
  },
  myPage: function(ctrl) {
    if (!ctrl.data.me) return;
    return Math.floor((ctrl.data.me.rank - 1) / 10) + 1;
  }
};
