import { h, thunk } from 'snabbdom'
import { VNode, VNodeData } from 'snabbdom/vnode'
import { Ctrl, Line } from './interfaces'
import * as spam from './spam'
import enhance from './enhance';
import { presetView } from './preset';
import { lineAction } from './moderation';
import { userLink, bind } from './util';

const whisperRegex = /^\/w(?:hisper)?\s/;

export default function(ctrl: Ctrl): Array<VNode | undefined> {
  if (!ctrl.vm.enabled) return [];
  const scrollCb = (vnode: VNode) => {
    const el = vnode.elm as HTMLElement
      if (ctrl.data.lines.length > 5) {
        const autoScroll = (el.scrollTop === 0 || (el.scrollTop > (el.scrollHeight - el.clientHeight - 100)));
        if (autoScroll) {
          el.scrollTop = 999999;
          setTimeout((_: any) => el.scrollTop = 999999, 300)
        }
      }
  },
  m = ctrl.moderation();
  const vnodes = [
    h('ol.mchat__messages', {
      attrs: {
        role: 'log',
        'aria-live': 'polite',
        'aria-atomic': false
      },
      hook: {
        insert(vnode) {
          const $el = $(vnode.elm as HTMLElement).on('click', 'a.jump', (e: Event) => {
            window.lichess.pubsub.emit('jump', (e.target as HTMLElement).getAttribute('data-ply'));
          });
          if (m) $el.on('click', '.mod', (e: Event) => {
            m.open(((e.target as HTMLElement).getAttribute('data-username') as string).split(' ')[0]);
          });
          scrollCb(vnode);
        },
        postpatch: (_, vnode) => scrollCb(vnode)
      }
    }, selectLines(ctrl).map(line => renderLine(ctrl, line))),
    renderInput(ctrl)
  ];
  const presets = presetView(ctrl.preset);
  if (presets) vnodes.push(presets)
  return vnodes;
}

function renderInput(ctrl: Ctrl): VNode | undefined {
  if (!ctrl.vm.writeable) return;
  if ((ctrl.data.loginRequired && !ctrl.data.userId) || ctrl.data.restricted)
  return h('input.mchat__say', {
    attrs: {
      placeholder: ctrl.trans('loginToChat'),
      disabled: true
    }
  });
  let placeholder: string;
  if (ctrl.vm.timeout) placeholder = ctrl.trans('youHaveBeenTimedOut');
  else if (ctrl.opts.blind) placeholder = 'Chat';
  else placeholder = ctrl.trans.noarg(ctrl.vm.placeholderKey);
  return h('input.mchat__say', {
    attrs: {
      placeholder,
      autocomplete: 'off',
      maxlength: 140,
      disabled: ctrl.vm.timeout || !ctrl.vm.writeable
    },
    hook: bind('keypress', (e: KeyboardEvent) => setTimeout(() => {
      const el = e.target as HTMLInputElement,
      txt = el.value,
      pub = ctrl.opts.public;
      if (e.which == 10 || e.which == 13) {
        if (txt === '') $('.keyboard-move input').focus();
        else {
          spam.report(txt);
          if (pub && spam.hasTeamUrl(txt)) alert("Please don't advertise teams in the chat.");
          else ctrl.post(txt);
          el.value = '';
          if (!pub) el.classList.remove('whisper');
        }
      }
      else {
        el.removeAttribute('placeholder');
        if (!pub) el.classList.toggle('whisper', !!txt.match(whisperRegex));
      }
    }))
  });
}

function sameLines(l1: Line, l2: Line) {
  return l1.d && l2.d && l1.u === l2.u;
}

function selectLines(ctrl: Ctrl): Array<Line> {
  let prev: Line, ls: Array<Line> = [];
  ctrl.data.lines.forEach(line => {
    if (!line.d &&
      (!prev || !sameLines(prev, line)) &&
      (!line.r || ctrl.opts.kobold) &&
      !spam.skip(line.t)
    ) ls.push(line);
    prev = line;
  });
  return ls;
}

function updateText(parseMoves: boolean) {
  return (oldVnode: VNode, vnode: VNode) => {
    if ((vnode.data as VNodeData).lichessChat !== (oldVnode.data as VNodeData).lichessChat) {
      (vnode.elm as HTMLElement).innerHTML = enhance((vnode.data as VNodeData).lichessChat, parseMoves);
    }
  };
}

function renderText(t: string, parseMoves: boolean) {
  const hook = updateText(parseMoves);
  return h('t', {
    lichessChat: t,
    hook: {
      create: hook,
      update: hook
    }
  });
}

function renderLine(ctrl: Ctrl, line: Line) {

  const textNode = renderText(line.t, ctrl.opts.parseMoves);

  if (line.u === 'lichess') return h('li.system', textNode);

  if (line.c) return h('li', [
    h('span.color', '[' + line.c + ']'),
    textNode
  ]);

  const userNode = thunk('a', line.u, userLink, [line.u, line.title]);

  return h('li', {
  }, ctrl.moderation() ? [
    line.u ? lineAction(line.u) : null,
    userNode,
    textNode
  ] : [userNode, textNode]);
}
