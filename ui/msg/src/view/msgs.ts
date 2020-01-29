import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Msg, Daily } from '../interfaces'
import * as enhance from './enhance';
import MsgCtrl from '../ctrl';

export default function renderMsgs(ctrl: MsgCtrl, msgs: Msg[]): VNode {
  return h('div.msg-app__convo__msgs', {
    hook: {
      insert: setupMsgs(true),
      postpatch: setupMsgs(false)
    }
  }, [
    h('div.msg-app__convo__msgs__init'),
    h('div.msg-app__convo__msgs__content', contentMsgs(ctrl, msgs))
  ]);
}

function contentMsgs(ctrl: MsgCtrl, msgs: Msg[]): VNode[] {
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

function renderMsg(ctrl: MsgCtrl, msg: Msg) {
  return h(msg.user == ctrl.data.me.id ? 'mine' : 'their', [
    renderText(msg),
    h('em', `${pad2(msg.date.getHours())}:${pad2(msg.date.getMinutes())}`)
  ]);
}
function pad2(num: number): string {
  return (num < 10 ? '0' : '') + num;
}

function groupMsgs(msgs: Msg[]): Daily[] {
  let prev: Msg = msgs[0];
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

let autoscroll = () => {};

function renderText(msg: Msg) {
  return enhance.isMoreThanText(msg.text) ? h('t', {
    hook: {
      create(_, vnode: VNode) {
        const el = (vnode.elm as HTMLElement);
        el.innerHTML = enhance.enhance(msg.text);
        el.querySelectorAll('img').forEach(c =>
          c.addEventListener('load', _ => autoscroll(), { once: true })
        );
      }
    }
  }) : h('t', msg.text);
}

const setupMsgs = (insert: boolean) => (vnode: VNode) => {
  const el = (vnode.elm as HTMLElement);
  if (insert) autoscroll = () => requestAnimationFrame(() => el.scrollTop = 9999999);
  enhance.expandIFrames(el, autoscroll);
  autoscroll();
}
