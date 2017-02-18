import { h } from 'snabbdom'

import { ChatCtrl } from './interfaces'

export default function view(ctrl: ChatCtrl) {
  return h('div', 'hello')
}
