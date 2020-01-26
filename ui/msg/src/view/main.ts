import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import MsgCtrl from '../ctrl';
import renderConvo from './convo';
import renderThread from './thread';
import * as search from './search';

export default function(ctrl: MsgCtrl): VNode {
  const activeId = ctrl.data.convo && ctrl.data.convo.thread.id;
  return h('main.msg-app.box', [
    h('div.msg-app__side', [
      search.renderInput(ctrl),
      ctrl.searchRes ?
        search.renderResults(ctrl, ctrl.searchRes) :
        h('div.msg-app__threads', ctrl.data.threads.map(t => renderThread(ctrl, t, activeId)))
    ]),
    ctrl.data.convo ? renderConvo(ctrl, ctrl.data.convo) : null
  ]);
}
