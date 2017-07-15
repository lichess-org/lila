import { h } from 'snabbdom'
import * as dialog from './dialog';
import { prop } from 'common';
import { bind, bindSubmit } from '../util';

const visibilityChoices = [
  ['public', 'Public'],
  ['private', 'Invite only']
];
const userSelectionChoices = [
  ['nobody', 'Nobody'],
  ['owner', 'Only me'],
  ['contributor', 'Contributors'],
  ['member', 'Members'],
  ['everyone', 'Everyone']
];

function select(s) {
  return [
    h('select#study-' + s.key, s.choices.map(function(o) {
      return h('option', {
        attrs: {
          value: o[0],
          selected: s.selected === o[0]
        }
      }, o[1]);
    })),
    h('label.control-label', {
      attrs: { for: 'study-' + s.key }
    }, s.name),
    h('i.bar')
  ];
};

export function ctrl(save, getData, redraw: () => void) {

  const initAt = Date.now();

  function isNew() {
    var d = getData();
    return d.from === 'scratch' && d.isNew && Date.now() - initAt < 9000;
  }

  const open = prop(false);

  return {
    open,
    openIfNew() {
      if (isNew()) open(true);
    },
    save(data, isNew) {
      save(data, isNew);
      open(false);
    },
    getData,
    isNew,
    redraw
  };
}

export function view(ctrl) {
  const data = ctrl.getData();
  const isNew = ctrl.isNew();
  const updateName = function(vnode, isUpdate) {
    const el = vnode.elm as HTMLInputElement;
    if (!isUpdate && !el.value) {
      el.value = data.name;
      if (isNew) el.select();
      el.focus();
    }
  }
  return dialog.form({
    onClose: function() {
      ctrl.open(false);
      ctrl.redraw();
    },
    content: [
      h('h2', (isNew ? 'Create' : 'Edit') + ' study'),
      h('form.material.form.align-left', {
        hook: bindSubmit(e => {
          const obj = {};
          'name visibility computer explorer cloneable chat sticky'.split(' ').forEach(function(n) {
            obj[n] = ((e.target as HTMLElement).querySelector('#study-' + n) as HTMLInputElement).value;
          });
          ctrl.save(obj, isNew);
        }, ctrl.redraw)
      }, [
        h('div.form-group', [
          h('input#study-name', {
            attrs: {
              minlength: 3,
              maxlength: 100
            },
            hook: {
              insert: vnode => updateName(vnode, false),
              postpatch: (_, vnode) => updateName(vnode, true)
            }
          }),
          h('label.control-label', { attrs: { 'for': 'study-name' } }, 'Name'),
          h('i.bar')
        ]),
        h('div', [
          h('div.form-group.half', select({
            key: 'visibility',
            name: 'Visibility',
            choices: visibilityChoices,
            selected: data.visibility
          })),
          h('div.form-group.half', select({
            key: 'cloneable',
            name: 'Allow cloning',
            choices: userSelectionChoices,
            selected: data.settings.cloneable
          })),
          h('div.form-group.half', select({
            key: 'computer',
            name: 'Computer analysis',
            choices: userSelectionChoices,
            selected: data.settings.computer
          })),
          h('div.form-group.half', select({
            key: 'explorer',
            name: 'Opening explorer',
            choices: userSelectionChoices,
            selected: data.settings.explorer
          })),
          h('div.form-group.half', select({
            key: 'chat',
            name: 'Chat',
            choices: userSelectionChoices,
            selected: data.settings.chat
          })),
          h('div.form-group.half', select({
            key: 'sticky',
            name: 'Enable sync',
            choices: [
              [true, 'Yes: keep everyone on the same position'],
              [false, 'No: let people browse freely']
            ],
            selected: data.settings.sticky
          })),
        ]),
        dialog.button(isNew ? 'Start' : 'Save')
      ]),
      h('div.destructive', [
        isNew ? null : h('form', {
          attrs: {
            action: '/study/' + data.id + '/clear-chat',
            method: 'post'
          },
          hook: bind('submit', e => {
            if (!confirm('Delete the study chat history? There is no going back!'))
              e.preventDefault();
          })
        }, [
          h('button.button.frameless', 'Clear chat')
        ]),
        h('form', {
          attrs: {
            action: '/study/' + data.id + '/delete',
            method: 'post'
          },
          hook: bind('submit', e => {
            if (!isNew && !confirm('Delete the entire study? There is no going back!'))
              e.preventDefault();
          })
        }, [
          h('button.button.frameless', isNew ? 'Cancel' : 'Delete study')
        ])
      ])
    ]
  });
}
