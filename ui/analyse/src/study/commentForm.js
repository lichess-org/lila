var m = require('mithril');
var partial = require('chessground').util.partial;

module.exports = {
  ctrl: function(contribute, myUserId) {

    var current = null; // {chapterId, path, node}

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
      submit: function(text) {
        contribute('setComment', {
          chapterId: chapterId,
          path: path,
          text: text
        });
        current = null;
      }
    }
  },
  view: function(ctrl) {

    var current = ctrl.current();
    if (!current) return;

    var mine = (current.node.comments || []).find(function(c) {
      return c.by === ctrl.myUserId;
    });

    return m('div.lichess_overboard.study_overboard', {
      config: function(el, isUpdate) {
        if (!isUpdate) lichess.loadCss('/assets/stylesheets/material.form.css');
      }
    }, [
      m('a.close.icon[data-icon=L]', {
        onclick: ctrl.close
      }),
      m('h2', 'Comment position after ' + current.node.san),
      m('form.material.form', {
        onsubmit: function(e) {
          ctrl.submit(e.target.querySelector('#comment-text').value);
          e.stopPropagation();
          return false;
        }
      }, [
        m('div.game.form-group', [
          m('textarea#comment-text', {
            placeholder: 'What do you think of this move/position?',
            value: mine ? mine.text : '',
            config: function(el, isUpdate) {
              if (!isUpdate) el.focus();
            }
          }),
          m('i.bar')
        ]),
        m('div.button-container',
          m('button.submit.button[type=submit]', 'Done')
        )
      ])
    ]);
  }
};
