var m = require('mithril');
var partial = require('chessground').util.partial;
var classSet = require('chessground').util.classSet;
var memberView = require('./studyMembers').view;
var chapterView = require('./studyChapters').view;

var moreIcon = m('i', {
  'data-icon': '['
});

module.exports = {

  main: function(ctrl) {

    var activeTab = ctrl.vm.tab();

    var makeTab = function(key, name) {
      return m('a', {
        class: key + (activeTab === key ? ' active' : ''),
        onclick: partial(ctrl.vm.tab, key),
      }, name);
    };

    var tabs = m('div.tabs', [
      makeTab('members', 'Members'),
      makeTab('chapters', 'Chapters'),
      makeTab('more', moreIcon)
    ]);

    var panel;
    if (activeTab === 'members') panel = memberView(ctrl);
    else if (activeTab === 'chapters') panel = chapterView.main(ctrl);

    return [
      tabs,
      panel
    ];
  },

  overboard: function(ctrl) {
    if (ctrl.chapters.vm.creating)
      return chapterView.form(ctrl.chapters, ctrl.chapters.vm.creating);
  },

  underboard: function(ctrl) {
    return m('div.study_buttons', [
      m('span.sync.hint--top', {
        'data-hint': ctrl.vm.behind !== false ? 'Synchronize with other players' : 'Disconnect to play local moves'
      }, m('a.button', (function() {
        var attrs = {
          onclick: ctrl.toggleSync
        };
        if (ctrl.vm.behind > 0) {
          attrs['data-count'] = ctrl.vm.behind;
          attrs.class = 'data-count';
        }
        return attrs;
      })(), m('i', {
        'data-icon': ctrl.vm.behind !== false ? 'G' : 'Z'
      }))),
      m('a.button.hint--top', {
        'data-hint': 'Download as PGN',
        href: '/study/' + ctrl.data.id + '.pgn'
      }, m('i[data-icon=x]'))
    ]);
  }
};
