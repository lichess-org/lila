var m = require('mithril');
var dialog = require('./dialog');

var visibilityChoices = [
  ['public', 'Public'],
  ['private', 'Invite only']
];
var userSelectionChoices = [
  ['nobody', 'Nobody'],
  ['owner', 'Only me'],
  ['contributor', 'Contributors'],
  ['member', 'Members'],
  ['everyone', 'Everyone']
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

    var initAt = Date.now();

    function isNew() {
      var d = getData();
      return d.from === 'scratch' && d.isNew && Date.now() - initAt < 5000;
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
        m('form.material.form.align-left', {
          onsubmit: function(e) {
            var obj = {};
            'name visibility computer explorer cloneable chat sticky'.split(' ').forEach(function(n) {
              obj[n] = e.target.querySelector('#study-' + n).value;
            });
            ctrl.save(obj, isNew);
            e.stopPropagation();
            return false;
          }
        }, [
          m('div.form-group', [
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
          m('div', [
            m('div.form-group.half', select({
              key: 'visibility',
              name: 'Visibility',
              choices: visibilityChoices,
              selected: data.visibility
            })),
            m('div.form-group.half', select({
              key: 'cloneable',
              name: 'Allow cloning',
              choices: userSelectionChoices,
              selected: data.settings.cloneable
            })),
            m('div.form-group.half', select({
              key: 'computer',
              name: 'Computer analysis',
              choices: userSelectionChoices,
              selected: data.settings.computer
            })),
            m('div.form-group.half', select({
              key: 'explorer',
              name: 'Opening explorer',
              choices: userSelectionChoices,
              selected: data.settings.explorer
            })),
            m('div.form-group.half', select({
              key: 'chat',
              name: 'Chat',
              choices: userSelectionChoices,
              selected: data.settings.chat
            })),
            m('div.form-group.half', select({
              key: 'sticky',
              name: 'Enable sync',
              choices: [
                [true, 'Yes: keep everyone on the same ply'],
                [false, 'No: let people browse freely']
              ],
              selected: data.settings.sticky
            })),
          ]),
          dialog.button(isNew ? 'Start' : 'Save')
        ]),
        m('div.destructive', [
          isNew ? null : m('form', {
            action: '/study/' + data.id + '/clear-chat',
            method: 'post',
            onsubmit: function() {
              return confirm('Delete the study chat history? There is no going back!');
            }
          }, m('button.button.frameless', 'Clear chat')),
          m('form', {
            action: '/study/' + data.id + '/delete',
            method: 'post',
            onsubmit: function() {
              return isNew || confirm('Delete the entire study? There is no going back!');
            }
          }, m('button.button.frameless', isNew ? 'Cancel' : 'Delete study'))
        ])
      ]
    });
  }
};
