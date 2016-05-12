var m = require('mithril');
var dialog = require('./dialog');

module.exports = {
  ctrl: function(send, getData) {

    var initAt = new Date();

    function isNew() {
      return getData().isNew && new Date() - initAt < 5000;
    }

    var open = m.prop(isNew());

    return {
      open: open,
      save: function(data) {
        send("editStudy", data);
        open(false);
      },
      getData: getData,
      isNew: isNew
    };
  },
  view: function(ctrl) {
    var data = ctrl.getData();
    var isNew = ctrl.isNew();
    return dialog.form({
      onClose: function() {
        ctrl.open(false);
      },
      content: [
        m('h2', (isNew ? 'Create' : 'Edit') + ' study'),
        m('form.material.form', {
          onsubmit: function(e) {
            ctrl.save({
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
                  el.value = data.name;
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
                selected: data.visibility === o[0]
              }, o[1]);
            })),
            m('label.control-label[for=study-visibility]', 'Visibility'),
            m('i.bar')
          ]),
          dialog.button(isNew ? 'Start' : 'Save')
        ]),
        m('form.delete_study', {
          action: '/study/' + data.id + '/delete',
          method: 'post',
          onsubmit: function() {
            return isNew || confirm('Delete the entire study? There is no going back!');
          }
        }, m('button.button.frameless', isNew ? 'Cancel' : 'Delete study'))
      ]
    });
  }
};
