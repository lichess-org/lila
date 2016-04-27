var m = require('mithril');
var partial = require('chessground').util.partial;
var classSet = require('chessground').util.classSet;
var memberView = require('./studyMembers').view;
var chapterView = require('./studyChapters').view;
var chapterFormView = require('./chapterForm').view;

function form(ctrl) {
  return m('div.lichess_overboard.study_overboard', {
    config: function(el, isUpdate) {
      if (!isUpdate) lichess.loadCss('/assets/stylesheets/material.form.css');
    }
  }, [
    m('a.close.icon[data-icon=L]', {
      onclick: function() {
        ctrl.vm.editing = null;
      }
    }),
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
          value: ctrl.data.name
        }),
        m('label.control-label[for=study-name]', 'Name'),
        m('i.bar')
      ]),
      m('div.game.form-group', [
        m('select#study-visibility', [['public', 'Public'], ['private', 'Invite only']].map(function(o) {
          return m('option', {
            value: o[0],
            selected: ctrl.data.visibility === o[0]
          }, o[1]);
        })),
        m('label.control-label[for=study-visibility]', 'Visibility'),
        m('i.bar')
      ]),
      m('div.button-container',
        m('button.submit.button.text[type=submit][data-icon=E]', 'Update study')
      )
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

  overboard: function(ctrl) {
    if (ctrl.chapters.form.vm.open)
      return chapterFormView(ctrl.chapters.form);
    if (ctrl.vm.editing)
      return form(ctrl);
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
