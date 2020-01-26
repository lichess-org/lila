import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import MsgCtrl from '../ctrl';
import renderConvo from './convo';
import renderContact from './contact';
import * as search from './search';

export default function(ctrl: MsgCtrl): VNode {
  const activeId = ctrl.data.convo?.user.id;
  return h('main.msg-app.box', [
    h('div.msg-app__side', [
      search.renderInput(ctrl),
      ctrl.searchRes ?
        search.renderResults(ctrl, ctrl.searchRes) :
        h('div.msg-app__contacts.msg-app__side__content',
          ctrl.data.contacts.map(t => renderContact(ctrl, t, activeId))
        )
    ]),
    ctrl.data.convo ? renderConvo(ctrl, ctrl.data.convo) : null
  ]);
}
