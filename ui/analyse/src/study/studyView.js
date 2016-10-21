var m = require('mithril');
var partial = require('chessground').util.partial;
var classSet = require('chessground').util.classSet;
var util = require('../util');
var memberView = require('./studyMembers').view;
var chapterView = require('./studyChapters').view;
var chapterNewFormView = require('./chapterNewForm').view;
var chapterEditFormView = require('./chapterEditForm').view;
var commentFormView = require('./commentForm').view;
var currentCommentsView = require('./studyComments').currentComments;
var glyphFormView = require('./studyGlyph').view;
var inviteFormView = require('./inviteForm').view;
var studyFormView = require('./studyForm').view;
var studyShareView = require('./studyShare').view;
var notifView = require('./notif').view;

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
      ctrl.data.features.cloneable ? m('a.button.hint--top', {
        'data-hint': 'Clone this study',
        href: '/study/' + ctrl.data.id + '/clone'
      }, m('i', {
        'data-icon': '{'
      })) : null,
      m('a.button.share.hint--top', {
          class: classSet({
            active: ctrl.share.open()
          }),
          'data-hint': 'Share & export',
          config: util.bindOnce('click', function() {
            ctrl.share.toggle();
          })
        },
        m('i.[data-icon=z]')),
      canContribute ? [
        (function(enabled) {
          return m('a.button.comment.hint--top', {
            class: classSet({
              active: ctrl.commentForm.current(),
              disabled: !enabled
            }),
            'data-hint': 'Comment this position',
            config: util.bindOnce('click', function() {
              if (ctrl.vm.behind === false) ctrl.commentForm.toggle(ctrl.currentChapter().id, root.vm.path, root.vm.node);
            })
          }, m('i[data-icon=c]'));
        })(ctrl.vm.behind === false), (function(enabled) {
          return m('a.button.glyph.hint--top', {
              class: classSet({
                active: ctrl.glyphForm.isOpen(),
                disabled: !enabled
              }),
              'data-hint': 'Annotate with symbols',
              config: util.bindOnce('click', function() {
                if (root.vm.path && ctrl.vm.behind === false) ctrl.glyphForm.toggle();
              })
            },
            m('i.glyph-icon'));
        })(root.vm.path && ctrl.vm.behind === false)
      ] : null
    ]),
    m('span.button.help.hint--top', {
      'data-hint': 'Need help? Get the tour!',
      onclick: ctrl.startTour
    }, m('i.text', {
      'data-icon': ''
    }, 'help'))
  ]);
}

function renderTable(rows) {
  return m('table.tags.slist', m('tbody', rows.map(function(r) {
    if (r) return m('tr', [
      m('th', r[0]),
      m('td', r[1])
    ]);
  })));
}

function renderPgn(setup) {
  var obj = setup.fromPgn || setup.game;
  if (obj) return renderTable([
    ['Fen', m('pre#study_fen', '')],
  ].concat(obj.tags.map(function(tag) {
    if (tag.name.toLowerCase() !== 'fen') return [
      tag.name, m.trust($.urlToLink(tag.value))
    ];
  })));
}

function renderFen(setup) {
  return renderTable([
    ['Fen', m('pre#study_fen', '')],
    ['Variant', setup.variant.name]
  ]);
}

var lastMetaKey;

function metadata(ctrl) {
  lichess.raf(function() {
    var n = document.getElementById('study_fen');
    if (n) n.textContent = ctrl.currentNode().fen;
  });
  var chapter = ctrl.currentChapter();
  if (!chapter) return;
  var d = ctrl.data;
  var cacheKey = [chapter.id, d.name, chapter.name, d.likes].join('|');
  if (cacheKey === lastMetaKey && m.redraw.strategy() === 'diff') {
    return {
      subtree: 'retain'
    };
  }
  lastMetaKey = cacheKey;
  var setup = d.chapter.setup;
  return m('div.study_metadata.undertable', [
    m('h2.undertable_top', [
      m('span.name', {
        'data-icon': ''
      }, d.name, ': ' + chapter.name),
      m('span', {
        class: 'liking text' + (d.liked ? ' liked' : ''),
        'data-icon': d.liked ? '' : '',
        title: 'Like',
        onclick: ctrl.toggleLike
      }, d.likes)
    ]),
    m('div.undertable_inner',
      renderPgn(setup) || renderFen(setup)
    )
  ]);
}

module.exports = {

  main: function(ctrl) {

    var activeTab = ctrl.vm.tab();

    var makeTab = function(key, name) {
      return m('a', {
        class: key + (activeTab === key ? ' active' : ''),
        config: util.bindOnce('mousedown', partial(ctrl.vm.tab, key)),
      }, name);
    };

    var tabs = m('div.study_tabs', [
      makeTab('members', util.plural('Member', ctrl.members.size())),
      makeTab('chapters', util.plural('Chapter', ctrl.chapters.size())),
      ctrl.members.isOwner() ? m('a.more', {
        config: util.bindOnce('click', function() {
          ctrl.form.open(!ctrl.form.open());
        })
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
    if (ctrl.members.canContribute()) return [
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
    if (ctrl.share.open()) return studyShareView(ctrl.share);
  },

  underboard: function(ctrl) {
    if (ctrl.embed) return;
    var glyphForm = glyphFormView(ctrl.study.glyphForm);
    var commentForm = commentFormView(ctrl.study.commentForm);
    return [
      glyphForm,
      currentCommentsView(ctrl, !commentForm),
      commentForm,
      buttons(ctrl),
      metadata(ctrl.study, ctrl.tree.root),
      notifView(ctrl.study.notif)
    ];
  }
};
