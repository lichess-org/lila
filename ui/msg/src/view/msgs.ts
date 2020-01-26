import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { ConvoMsg, Daily } from '../interfaces'
import * as enhance from './enhance';
import MsgCtrl from '../ctrl';

export default function renderMsgs(ctrl: MsgCtrl, msgs: ConvoMsg[]): VNode {
  return h('div.msg-app__convo__msgs', {
    hook: {
      insert: setupMsgs,
      postpatch: setupMsgs
    }
  }, [
    h('div.msg-app__convo__msgs__init'),
    h('div.msg-app__convo__msgs__content', contentMsgs(ctrl, msgs))
  ]);
}

function contentMsgs(ctrl: MsgCtrl, msgs: ConvoMsg[]): VNode[] {
  const dailies = groupMsgs(msgs);
  const nodes: VNode[] = [];
  dailies.forEach(daily => nodes.push(...renderDaily(ctrl, daily)));
  return nodes;
}

function renderDaily(ctrl: MsgCtrl, daily: Daily): VNode[] {
  return [
    h('day', renderDate(daily.date)),
    ...daily.msgs.map(group =>
      h('group', group.map(msg => renderMsg(ctrl, msg)))
    )
  ];
}

function renderMsg(ctrl: MsgCtrl, msg: ConvoMsg) {
  return h(msg.user == ctrl.data.me.id ? 'mine' : 'their', [
    renderText(msg),
    h('em', `${pad2(msg.date.getHours())}:${pad2(msg.date.getMinutes())}`)
  ]);
}
function pad2(num: number): string {
  return (num < 10 ? '0' : '') + num;
}

function groupMsgs(msgs: ConvoMsg[]): Daily[] {
  let prev: ConvoMsg = msgs[0];
  if (!prev) return [{ date: new Date(), msgs: [] }];
  const dailies: Daily[] = [{
    date: prev.date,
    msgs: [[prev]]
  }];
  msgs.slice(1).forEach(msg => {
    if (sameDay(msg.date, prev.date)) {
      if (msg.user == prev.user) dailies[0].msgs[0].unshift(msg);
      else dailies[0].msgs.unshift([msg]);
    } else dailies.unshift({
      date: msg.date,
      msgs: [[msg]]
    });
    prev = msg;
  });
  return dailies;
}

const today = new Date();
const yesterday = new Date();
yesterday.setDate(yesterday.getDate() - 1);

function renderDate(date: Date) {
  if (sameDay(date, today)) return 'TODAY';
  if (sameDay(date, yesterday)) return 'YESTERDAY';
  return `${date.getDate()}/${date.getMonth() + 1}/${date.getFullYear()}`;
}

function sameDay(d: Date, e: Date) {
  return d.getDate() == e.getDate() && d.getMonth() == e.getMonth() && d.getFullYear() == e.getFullYear();
}

function renderText(msg: ConvoMsg) {
    return enhance.isMoreThanText(msg.text) ? h('t', {
      key: msg.id,
      hook: {
        create(_, vnode: VNode) {
          (vnode.elm as HTMLElement).innerHTML = enhance.enhance(msg.text);
        }
      }
    }) : h('t', { key: msg.id }, msg.text);
}

function setupMsgs(vnode: VNode) {
  (vnode.elm as HTMLElement).scrollTop = 9999999;
}
