var m = require('mithril');
var partial = require('chessground').util.partial;
var nodeFullName = require('../util').nodeFullName;

module.exports = {
  ctrl: function(contribute, myUserId) {

    var current = null; // {chapterId, path, node}

    var submit = function(text) {
      if (!current) return;
      contribute('setComment', {
        chapterId: current.chapterId,
        path: current.path,
        text: text
      });
      current = null;
    }

    return {
      myUserId: myUserId,
      current: function() {
        return current;
      },
      open: function(chapterId, path, node) {
        current = {
          chapterId: chapterId,
          path: path,
          node: node
        };
      },
      close: function() {
        current = null;
      },
      submit: submit,
      clear: function() {
        submit('');
      }
    }
  },
  view: function(ctrl) {

    var current = ctrl.current();
    if (!current) return;

    var mine = (current.node.comments || []).find(function(c) {
      return c.by === ctrl.myUserId;
    });

    return m('div.study_comment_form', {
      config: function(el, isUpdate) {
        if (!isUpdate) lichess.loadCss('/assets/stylesheets/material.form.css');
      }
    }, [
      m('form.material.form', {
        onsubmit: function(e) {
          ctrl.submit(e.target.querySelector('#comment-text').value);
          e.stopPropagation();
          return false;
        }
      }, [
        m('div.game.form-group', [
          m('textarea#comment-text', {
            placeholder: 'Comment position after ' + nodeFullName(current.node),
            value: mine ? mine.text : '',
            config: function(el, isUpdate) {
              if (!isUpdate) el.focus();
            }
          }),
          m('i.bar')
        ]),
        m('div.button-container', [
          m('button.submit.button', {
            type: 'submit',
            'data-icon': 'E',
            title: 'Save'
          }),
          m('button.button', {
            'data-icon': 'L',
            title: 'Clear',
            onclick: ctrl.clear
          })
        ])
      ])
    ]);
  }
};
