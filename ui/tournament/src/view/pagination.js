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
        button('Last', 'V', function() {
          ctrl.vm.page = pag.nbPages;
        }, ctrl.vm.page < pag.nbPages)
      ]) : null
    ];
  },
  players: function(ctrl) {
    var page = ctrl.vm.page;
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
};
