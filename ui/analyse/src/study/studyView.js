var m = require('mithril');
var partial = require('chessground').util.partial;
var classSet = require('chessground').util.classSet;
var memberView = require('./studyMembers').view;
var chapterView = require('./studyChapters').view;

module.exports = function(ctrl) {

  var makeTab = function(key, name) {
    return m('a', {
      class: ctrl.vm.tab() === key ? 'active' : '',
      onclick: partial(ctrl.vm.tab, key),
    }, name);
  };

  var tabs = m('div.tabs', [
      makeTab('members', 'Members'),
      makeTab('chapters', 'Chapters'),
      makeTab('settings', 'Settings')
  ]);

  var panel;
  if (ctrl.vm.tab() === 'members') panel = memberView(ctrl);
  else if (ctrl.vm.tab() === 'chapters') panel = chapterView(ctrl);

  return [
    tabs,
    panel
  ];
};
