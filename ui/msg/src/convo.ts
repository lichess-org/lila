import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Convo, ConvoMsg, Daily } from './interfaces'
import { userName, userIcon } from './util';
import MsgCtrl from './ctrl';

export default function renderConvo(ctrl: MsgCtrl, convo: Convo): VNode {
  return h('div.msg-app__convo', {
    key: `${convo.thread.contact.id}:${convo.msgs[0].date.getDate()}`,
  }, [
    h('div.msg-app__convo__head', [
      h('div.msg-app__convo__head__contact', [
        userIcon(convo.thread.contact, 'msg-app__convo__head__icon'),
        h('div.msg-app__convo__head__name', userName(convo.thread.contact))
      ])
    ]),
    h('div.msg-app__convo__msgs', {
      hook: {
        insert(vnode) {
          (vnode.elm as HTMLElement).scrollTop = 9999999;
        },
        postpatch(vnode) {
          (vnode.elm as HTMLElement).scrollTop = 9999999;
        }
      }
    }, [
      h('div.msg-app__convo__msgs__init'),
      h('div.msg-app__convo__msgs__content', renderMsgs(ctrl, convo))
    ]),
    h('div.msg-app__convo__reply', [
      h('textarea.msg-app__convo__reply__text', {
        attrs: {
          rows: 1,
          autofocus: 1
        },
        hook: {
          insert(vnode) {
            const el = vnode.elm as HTMLTextAreaElement;
            autogrow(el);
            el.addEventListener('keypress',
              (e: KeyboardEvent) => setTimeout(() => {
                const txt = el.value;
                if ((e.which == 10 || e.which == 13) && txt) {
                  ctrl.post(txt);
                  el.value = '';
                }
              })
            );
          }
        }
      })
    ])
  ]);
}

function renderMsgs(ctrl: MsgCtrl, convo: Convo): VNode[] {
  const dailies = groupMsgs(convo.msgs);
  console.log(dailies);
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
    h('p', msg.text),
    h('em', [
      msg.date.getHours(),
      ':',
      msg.date.getMinutes()
    ])
  ]);
}

function groupMsgs(msgs: ConvoMsg[]): Daily[] {
  let prev: ConvoMsg = msgs[0];
  if (!prev) return [];
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

function autogrow(textarea: HTMLTextAreaElement) {
  $(textarea)
    .one('focus', function(this: any) {
        let savedValue = this.value;
        this.value = '';
        this.baseScrollHeight = this.scrollHeight;
        this.value = savedValue;
    })
    .on('input', function(this: any) {
        this.rows = 1;
        let rows = Math.ceil((this.scrollHeight - this.baseScrollHeight) / 19);
        this.rows = 1 + rows;
    });
}
