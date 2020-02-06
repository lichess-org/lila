import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Convo, Msg, Daily } from '../interfaces'
import * as enhance from './enhance';
import { scroller } from './scroller';
import { bind } from './util';
import MsgCtrl from '../ctrl';

export default function renderMsgs(ctrl: MsgCtrl, convo: Convo): VNode {
  return h('div.msg-app__convo__msgs', {
    hook: {
      insert: setupMsgs(true),
      postpatch: setupMsgs(false)
    }
  }, [
    h('div.msg-app__convo__msgs__init'),
    h('div.msg-app__convo__msgs__content', [
      ctrl.canGetMoreSince ? h('button.msg-app__convo__msgs__more.button.button-empty', {
        key: 'more',
        hook: bind('click', _ => {
          scroller.setMarker();
          ctrl.getMore();
        })
      }, 'Load more') : null,
      ...contentMsgs(ctrl, convo.msgs),
      ctrl.typing ? h('div.msg-app__convo__msgs__typing', `${convo.user.name} is typing...`) : null
    ])
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
    h('day', {
      key: `d${daily.date.getTime()}`
    }, renderDate(daily.date, ctrl.trans)),
    ...daily.msgs.map(group =>
      h('group', {
        key: `g${daily.date.getTime()}`
      }, group.map(msg => renderMsg(ctrl, msg)))
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

function renderDate(date: Date, trans: Trans) {
  if (sameDay(date, today)) return trans.noarg('today').toUpperCase();
  if (sameDay(date, yesterday)) return trans.noarg('yesterday').toUpperCase();
  return `${date.getDate()}/${date.getMonth() + 1}/${date.getFullYear()}`;
}

function sameDay(d: Date, e: Date) {
  return d.getDate() == e.getDate() && d.getMonth() == e.getMonth() && d.getFullYear() == e.getFullYear();
}

function renderText(msg: Msg) {
  return enhance.isMoreThanText(msg.text) ? h('t', {
    hook: {
      create(_, vnode: VNode) {
        const el = (vnode.elm as HTMLElement);
        el.innerHTML = enhance.enhance(msg.text);
        el.querySelectorAll('img').forEach(c =>
          c.addEventListener('load', scroller.auto, { once: true })
        );
      }
    }
  }) : h('t', msg.text);
}

const setupMsgs = (insert: boolean) => (vnode: VNode) => {
  const el = (vnode.elm as HTMLElement);
  if (insert) scroller.init(el);
  enhance.expandIFrames(el);
  scroller.toMarker() || scroller.auto();
}
