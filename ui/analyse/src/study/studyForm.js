var m = require('mithril');
var dialog = require('./dialog');

var visibilityChoices = [
  ['public', 'Public'],
  ['private', 'Invite only']
];
var userSelectionChoices = [
  ['everyone', 'Everyone'],
  ['nobody', 'Nobody'],
  ['owner', 'Only me'],
  ['contributor', 'Contributors']
];

var select = function(s) {
  return [
    m('select#study-' + s.key, s.choices.map(function(o) {
      return m('option', {
        value: o[0],
        selected: s.selected === o[0]
      }, o[1]);
    })),
    m('label.control-label', {
      for: 'study-' + s.key
    }, s.name),
    m('i.bar')
  ];
};

module.exports = {
  ctrl: function(save, getData) {

    var initAt = new Date();

    function isNew() {
      return getData().isNew && new Date() - initAt < 5000;
    }

    var open = m.prop(false);

    return {
      open: open,
      openIfNew: function() {
        if (isNew()) open(true);
      },
      save: function(data, isNew) {
        save(data, isNew);
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
              visibility: e.target.querySelector('#study-visibility').value,
              computer: e.target.querySelector('#study-computer').value,
              explorer: e.target.querySelector('#study-explorer').value
            }, isNew);
            e.stopPropagation();
            return false;
          }
        }, [
          m('div.game.form-group', [
            m('input#study-name', {
              required: true,
              minlength: 3,
              maxlength: 100,
              config: function(el, isUpdate) {
                if (!isUpdate && !el.value) {
                  el.value = data.name;
                  if (isNew) el.select();
                  el.focus();
                }
              }
            }),
            m('label.control-label[for=study-name]', 'Name'),
            m('i.bar')
          ]),
          m('div.game.form-group', select({
            key: 'visibility',
            name: 'Visibility',
            choices: visibilityChoices,
            selected: data.visibility
          })),
          m('div', [
            m('div.game.form-group.half', select({
              key: 'computer',
              name: 'Computer analysis',
              choices: userSelectionChoices,
              selected: data.settings.computer
            })),
            m('div.game.form-group.half', select({
              key: 'explorer',
              name: 'Opening explorer',
              choices: userSelectionChoices,
              selected: data.settings.explorer
            }))
          ]),
          dialog.button(isNew ? 'Start' : 'Save')
        ]),
        m('form.delete_button', {
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
