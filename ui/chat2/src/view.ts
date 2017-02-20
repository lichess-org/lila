import { h } from 'snabbdom'
import { Ctrl, Tab } from './interfaces'
import { renderDiscussion } from './discussion'

export default function view(ctrl: Ctrl) {
  return h('div', {
    class: {
      mchat: true,
      mod: ctrl.opts.permissions.timeout
    }
  }, normalView(ctrl))
}

const tabs: Array<Tab> = ['discussion'];

function normalView(ctrl: Ctrl) {
  const active = ctrl.vm.tab
  // if (ctrl.note) tabs.push('note');
  return [
    h('div', {
      class: 'chat_tabs nb_' + tabs.length
    }, tabs.map(t => renderTab(ctrl, t, active))),
      h('div.content', renderDiscussion(ctrl))
  ]
}

function renderTab(ctrl: Ctrl, tab: Tab, active: Tab) {
  return h('div', {
    class: 'tab ' + tab + (tab === active ? ' active' : ''),
    on: { click: () => ctrl.setTab(tab) }
  }, tabName(ctrl, tab));
}

function tabName(ctrl: Ctrl, tab: Tab) {
  switch (tab) {
    case 'discussion':
      return [
      h('span', ctrl.data.name),
      // h('input', {
      //   type: 'checkbox',
      //   class: 'toggle_chat',
      //   title: ctrl.trans('toggleTheChat'),
      //   onchange: m.withAttr('checked', ctrl.setEnabled),
      //   checked: ctrl.vm.enabled()
      // })
    ];
    case 'note':
      return ctrl.trans('notes');
  }
}

// return [
//   m('div', {
//     class: 'chat_tabs nb_' + tabs.length
//   }, tabs.map(function(t) {
//     return m('div', {
//       onclick: function() {
//         ctrl.vm.tab(t);
//       },
//       class: 'tab ' + t + (tab === t ? ' active' : '')
//     }, tabName(ctrl, t));
//   })),
//   m('div.content.' + tab, tabContent(ctrl, tab))
// ];
