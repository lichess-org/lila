var m = require('mithril');
var partial = require('chessground').util.partial;
var classSet = require('chessground').util.classSet;
var nodeFullName = require('../util').nodeFullName;
var memberView = require('./studyMembers').view;
var chapterView = require('./studyChapters').view;
var chapterNewFormView = require('./chapterNewForm').view;
var chapterEditFormView = require('./chapterEditForm').view;
var commentFormView = require('./commentForm').view;
var currentCommentsView = require('./studyComments').currentComments;
var glyphFormView = require('./studyGlyph').view;
var inviteFormView = require('./inviteForm').view;
var studyFormView = require('./studyForm').view;

function contextAction(icon, text, handler) {
  return m('a.action', {
    'data-icon': icon,
    onclick: handler
  }, text);
}

function buttons(root) {
  var ctrl = root.study;
  var canContribute = ctrl.members.canContribute();
  return m('div.study_buttons', [
    m('div.member_buttons', [
      m('span#study-sync.hint--top', {
        'data-hint': ctrl.vm.behind !== false ? 'Synchronize with other players' : 'Disconnect to play local moves'
      }, m('a', (function() {
        var attrs = {
          onclick: ctrl.toggleSync
        };
        var classes = ['button'];
        if (ctrl.vm.behind > 0) {
          attrs['data-count'] = ctrl.vm.behind;
          classes.push('data-count');
        }
        if (ctrl.vm.behind !== false) classes.push('glowed');
        attrs.class = classes.join(' ');
        return attrs;
      })(), m('i', {
        'data-icon': ctrl.vm.behind !== false ? 'G' : 'Z'
      }))),
      m('a.button.hint--top', {
        'data-hint': 'Download as PGN',
        href: '/study/' + ctrl.data.id + '.pgn'
      }, m('i[data-icon=x]')),
      canContribute ? [
        (function(enabled) {
          return m('a.button.hint--top', {
            class: classSet({
              active: ctrl.commentForm.current(),
              disabled: !enabled
            }),
            'data-hint': 'Comment this position',
            onclick: enabled ? function() {
              ctrl.commentForm.toggle(ctrl.currentChapter().id, root.vm.path, root.vm.node)
            } : null
          }, m('i[data-icon=c]'));
        })(ctrl.vm.behind === false), (function(enabled) {
          return m('a.button.hint--top', {
            class: classSet({
              active: ctrl.glyphForm.isOpen(),
              disabled: !enabled
            }),
            'data-hint': 'Annotate with symbols',
            onclick: enabled ? ctrl.glyphForm.toggle : null
          }, m('i.glyph-icon'));
        })(root.vm.path && ctrl.vm.behind === false)
      ] : null
    ])
  ]);
}

function renderPgn(setup) {
  var obj = setup.fromPgn || setup.game;
  if (obj) return m('table.tags.slist', m('tbody', obj.tags.map(function(tag) {
    return m('tr', [
      m('th', tag.name),
      m('td', m.trust($.urlToLink(tag.value)))
    ]);
  })));
}

var lastMetaKey;

function metadata(ctrl) {
  var chapter = ctrl.currentChapter();
  if (!chapter) return;
  var cacheKey = [chapter.id, ctrl.data.name, chapter.name].join('|');
  if (cacheKey === lastMetaKey && m.redraw.strategy() === 'diff') return {
    subtree: 'retain'
  };
  lastMetaKey = cacheKey;
  return m('div.study_metadata.undertable', [
    m('h2.undertable_top', {
      'data-icon': ''
    }, [
      ctrl.data.name,
      ': ' +
      chapter.name
    ]),
    m('div.undertable_inner',
      renderPgn(ctrl.data.chapter.setup)
    )
  ]);
}

module.exports = {

  main: function(ctrl) {

    var activeTab = ctrl.vm.tab();

    var makeTab = function(key, name) {
      return m('a', {
        class: key + (activeTab === key ? ' active' : ''),
        onclick: partial(ctrl.vm.tab, key),
      }, name);
    };

    var tabs = m('div.study_tabs', [
      makeTab('members', 'Members'),
      makeTab('chapters', 'Chapters'),
      ctrl.members.isOwner() ? m('a.more', {
        onclick: function() {
          ctrl.form.open(!ctrl.form.open());
        }
      }, m('i', {
        'data-icon': '['
      })) : null
    ]);

    var panel;
    if (activeTab === 'members') panel = memberView(ctrl);
    else if (activeTab === 'chapters') panel = chapterView.main(ctrl);

    return [
      tabs,
      panel
    ];
  },

  contextMenu: function(ctrl, path, node) {
    return [
      m('a.action', {
        'data-icon': 'c',
        onclick: function() {
          ctrl.commentForm.open(ctrl.currentChapter().id, path, node);
        }
      }, 'Comment this move'),
      m('a.action.glyph-icon', {
        onclick: function() {
          ctrl.glyphForm.open();
          ctrl.userJump(path);
        }
      }, 'Annotate with symbols')
    ];
  },

  overboard: function(ctrl) {
    if (ctrl.chapters.newForm.vm.open) return chapterNewFormView(ctrl.chapters.newForm);
    if (ctrl.chapters.editForm.current()) return chapterEditFormView(ctrl.chapters.editForm);
    if (ctrl.members.inviteForm.open()) return inviteFormView(ctrl.members.inviteForm);
    if (ctrl.form.open()) return studyFormView(ctrl.form);
  },

  underboard: function(ctrl) {
    var glyphForm = glyphFormView(ctrl.study.glyphForm);
    var commentForm = commentFormView(ctrl.study.commentForm);
    return [
      glyphForm,
      currentCommentsView(ctrl, !commentForm),
      commentForm,
      buttons(ctrl),
      metadata(ctrl.study)
    ];
  }
};
