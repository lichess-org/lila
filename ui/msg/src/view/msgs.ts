import { bind } from 'common/snabbdom';
import { isMoreThanText } from 'common/rich-text';
import { VNode, h } from 'snabbdom';
import MsgCtrl from '../ctrl';
import { Convo, Daily, Msg } from '../interfaces';
import { scroller } from './scroller';
import { i18n, i18nFormat } from 'i18n';
import { msgEnhance } from './enhance';
import { loadCompiledScript } from 'common/assets';

export default function renderMsgs(ctrl: MsgCtrl, convo: Convo): VNode {
  return h(
    'div.msg-app__convo__msgs',
    {
      hook: {
        insert: setupMsgs(true),
        postpatch: setupMsgs(false),
      },
    },
    [
      h('div.msg-app__convo__msgs__init'),
      h('div.msg-app__convo__msgs__content', [
        ctrl.canGetMoreSince
          ? h(
              'button.msg-app__convo__msgs__more.button.button-empty',
              {
                key: 'more',
                attrs: {
                  type: 'button',
                },
                hook: bind('click', _ => {
                  scroller.setMarker();
                  ctrl.getMore();
                }),
              },
              'Load more',
            )
          : null,
        ...contentMsgs(ctrl, convo.msgs),
        h(
          'div.msg-app__convo__msgs__typing',
          ctrl.typing ? i18nFormat('xIsTyping', convo.user.name) : null,
        ),
      ]),
    ],
  );
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
      h(
        'group',
        group.map(msg => renderMsg(ctrl, msg)),
      ),
    ),
  ];
}

function renderMsg(ctrl: MsgCtrl, msg: Msg) {
  const tag = msg.user == ctrl.data.me.id ? 'mine' : 'their';
  return h(tag, [
    renderText(msg),
    h('em', `${pad2(msg.date.getHours())}:${pad2(msg.date.getMinutes())}`),
  ]);
}
const pad2 = (num: number): string => (num < 10 ? '0' : '') + num;

function groupMsgs(msgs: Msg[]): Daily[] {
  let prev: Msg = msgs[0];
  if (!prev) return [{ date: new Date(), msgs: [] }];
  const dailies: Daily[] = [
    {
      date: prev.date,
      msgs: [[prev]],
    },
  ];
  msgs.slice(1).forEach(msg => {
    if (sameDay(msg.date, prev.date)) {
      if (msg.user == prev.user) dailies[0].msgs[0].unshift(msg);
      else dailies[0].msgs.unshift([msg]);
    } else
      dailies.unshift({
        date: msg.date,
        msgs: [[msg]],
      });
    prev = msg;
  });
  return dailies;
}

const today = new Date();
const yesterday = new Date();
yesterday.setDate(yesterday.getDate() - 1);

function renderDate(date: Date) {
  if (sameDay(date, today)) return i18n('today').toUpperCase();
  if (sameDay(date, yesterday)) return i18n('yesterday').toUpperCase();
  return renderFullDate(date);
}

const renderFullDate = (date: Date) =>
  `${date.getDate()}/${date.getMonth() + 1}/${date.getFullYear()}`;

const sameDay = (d: Date, e: Date) =>
  d.getDate() == e.getDate() && d.getMonth() == e.getMonth() && d.getFullYear() == e.getFullYear();

const renderText = (msg: Msg) =>
  isMoreThanText(msg.text)
    ? h('t', {
        hook: {
          create(_, vnode: VNode) {
            const el = vnode.elm as HTMLElement;
            el.innerHTML = msgEnhance(msg.text);
            el.classList.add('expand-text');
            el.querySelectorAll('img').forEach(c =>
              c.addEventListener('load', scroller.auto, { once: true }),
            );
          },
        },
      })
    : h('t', msg.text);

const setupMsgs = (insert: boolean) => (vnode: VNode) => {
  const el = vnode.elm as HTMLElement;
  if (insert) scroller.init(el);
  if (window.lishogi.modules.miscExpandText) window.lishogi.modules.miscExpandText();
  scroller.toMarker() || scroller.auto();
};
