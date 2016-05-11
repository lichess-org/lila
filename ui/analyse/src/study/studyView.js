var m = require('mithril');
var partial = require('chessground').util.partial;
var nodeFullName = require('../util').nodeFullName;
var memberView = require('./studyMembers').view;
var chapterView = require('./studyChapters').view;
var chapterFormView = require('./chapterForm').view;
var commentFormView = require('./commentForm').view;
var glyphFormView = require('./studyGlyph').view;
var inviteFormView = require('./inviteForm').view;
var dialog = require('./dialog');

function form(ctrl) {
  return dialog.form({
    onClose: function() {
      ctrl.vm.editing = null;
    },
    content: [
      m('h2', 'Edit study'),
      m('form.material.form', {
        onsubmit: function(e) {
          ctrl.update({
            name: e.target.querySelector('#study-name').value,
            visibility: e.target.querySelector('#study-visibility').value
          });
          e.stopPropagation();
          return false;
        }
      }, [
        m('div.game.form-group', [
          m('input#study-name', {
            config: function(el, isUpdate) {
              if (!isUpdate && !el.value) {
                el.value = ctrl.data.name;
                el.focus();
              }
            }
          }),
          m('label.control-label[for=study-name]', 'Name'),
          m('i.bar')
        ]),
        m('div.game.form-group', [
          m('select#study-visibility', [
            ['public', 'Public'],
            ['private', 'Invite only']
          ].map(function(o) {
            return m('option', {
              value: o[0],
              selected: ctrl.data.visibility === o[0]
            }, o[1]);
          })),
          m('label.control-label[for=study-visibility]', 'Visibility'),
          m('i.bar')
        ]),
        dialog.button(ctrl.data.isNew ? 'Start' : 'Save')
      ])
    ]
  });
}

function contextAction(icon, text, handler) {
  return m('a.action', {
    'data-icon': icon,
    onclick: handler
  }, text);
}

function currentComments(ctrl, includingMine) {
  var path = ctrl.vm.path;
  var node = ctrl.vm.node;
  var chapter = ctrl.study.currentChapter();
  var comments = node.comments || [];
  if (!comments.length) return;
  return m('div.study_comments', comments.map(function(comment) {
    var isMine = ctrl.userId === comment.by.toLowerCase();
    if (!includingMine && isMine) return;
    var canDelete = isMine || ctrl.study.members.isOwner();
    return m('div.comment', [
      canDelete ? m('a.edit[data-icon=q][title=Delete]', {
        onclick: function() {
          if (confirm('Delete ' + comment.by + '\'s comment?'))
            ctrl.study.commentForm.delete(chapter.id, path, comment.by);
        }
      }) : null,
      isMine ? m('a.edit[data-icon=m][title=Edit]', {
        onclick: ctrl.study.commentForm.open
      }) : null,
      m('span.user_link.ulpt', {
        'data-href': '/@/' + comment.by
      }, comment.by),
      ' about ',
      m('span.node', nodeFullName(node)),
      ': ',
      m('span.text', comment.text.replace(/\n/g, '<br>'))
    ]);
  }));
}

function buttons(ctrl) {
  var path = ctrl.vm.path;
  var node = ctrl.vm.node;
  var canContribute = ctrl.study.members.canContribute();
  return m('div.study_buttons', [
    m('div.member_buttons', [
      m('span#study-sync.hint--top', {
        'data-hint': ctrl.study.vm.behind !== false ? 'Synchronize with other players' : 'Disconnect to play local moves'
      }, m('a', (function() {
        var attrs = {
          onclick: ctrl.study.toggleSync
        };
        var classes = ['button'];
        if (ctrl.study.vm.behind > 0) {
          attrs['data-count'] = ctrl.study.vm.behind;
          classes.push('data-count');
        }
        if (ctrl.study.vm.behind !== false) classes.push('glowed');
        attrs.class = classes.join(' ');
        return attrs;
      })(), m('i', {
        'data-icon': ctrl.study.vm.behind !== false ? 'G' : 'Z'
      }))),
      m('a.button.hint--top', {
        'data-hint': 'Download as PGN',
        href: '/study/' + ctrl.study.data.id + '.pgn'
      }, m('i[data-icon=x]')),
      canContribute ? [
        m('a.button.hint--top', {
          class: ctrl.study.commentForm.current() ? 'active' : '',
          'data-hint': 'Comment this position',
          disabled: ctrl.study.vm.behind !== false,
          onclick: function() {
            ctrl.study.commentForm.toggle(ctrl.study.currentChapter().id, path, node)
          }
        }, m('i[data-icon=c]')),
        m('a.button.hint--top', {
          class: ctrl.study.glyphForm.isOpen() ? 'active' : '',
          'data-hint': 'Annotate with symbols',
          disabled: ctrl.study.vm.behind !== false,
          onclick: ctrl.study.glyphForm.toggle
        }, m('i.glyph-icon'))
      ] : null
    ]),
    m('div', [
      ctrl.study.members.isOwner() ?
      m('button.button.hint--top', {
        class: ctrl.study.members.inviteForm.open() ? 'active' : '',
        'data-hint': 'Invite someone',
        onclick: ctrl.study.members.inviteForm.toggle
      }, m('i[data-icon=r]')) : null,
      ctrl.study.members.canContribute() ? m('button.button.hint--top', {
        class: ctrl.study.chapters.form.vm.open ? 'active' : '',
        'data-hint': 'Add a chapter',
        onclick: ctrl.study.chapters.form.toggle
      }, m('i[data-icon=O]')) : null
    ])
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
          ctrl.vm.editing = !ctrl.vm.editing;
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
    if (ctrl.chapters.form.vm.open) return chapterFormView(ctrl.chapters.form);
    if (ctrl.members.inviteForm.open()) return inviteFormView(ctrl.members.inviteForm);
    if (ctrl.vm.editing) return form(ctrl);
  },

  underboard: function(ctrl) {
    var glyphForm = glyphFormView(ctrl.study.glyphForm);
    var commentForm = commentFormView(ctrl.study.commentForm);
    return [
      glyphForm,
      currentComments(ctrl, !commentForm),
      commentForm,
      buttons(ctrl)
    ];
  }
};
