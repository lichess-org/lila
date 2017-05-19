import { h, thunk } from 'snabbdom'
import { VNode, VNodeData } from 'snabbdom/vnode'
import { Ctrl, Line } from './interfaces'
import * as spam from './spam'
import enhance from './enhance';
import { presetView } from './preset';
import { lineAction } from './moderation';
import { userLink, bind } from './util';

const whisperRegex = /^\/w(?:hisper)?\s/;

export default function(ctrl: Ctrl): VNode[] {
  if (!ctrl.vm.enabled) return [];
  const scrollCb = (vnode: VNode) => {
    const el = vnode.elm as HTMLElement
      if (ctrl.data.lines.length > 5) {
        const autoScroll = (el.scrollTop === 0 || (el.scrollTop > (el.scrollHeight - el.clientHeight - 100)));
        if (autoScroll) {
          el.scrollTop = 999999;
          setTimeout(_ => el.scrollTop = 999999, 300)
        }
      }
  }
  const vnodes = [
    h('ol.messages.content.scroll-shadow-soft', {
      hook: {
        insert(vnode) {
          $(vnode.elm as HTMLElement).on('click', 'a.jump', (e: Event) => {
            window.lichess.pubsub.emit('jump')((e.target as HTMLElement).getAttribute('data-ply'));
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
  return vnodes
}

function renderInput(ctrl: Ctrl) {
  if ((ctrl.data.loginRequired && !ctrl.data.userId) || ctrl.data.restricted)
  return h('input.lichess_say', {
    attrs: {
      placeholder: 'Login to chat',
      disabled: true
    }
  });
  let placeholder: string;
  if (ctrl.vm.timeout) placeholder = 'You have been timed out.';
  else if (!ctrl.vm.writeable) placeholder = 'Invited members only.';
  else placeholder = ctrl.trans(ctrl.vm.placeholderKey);
  return h('input.lichess_say', {
    attrs: {
      placeholder: placeholder,
      autocomplete: 'off',
      maxlength: 140,
      disabled: ctrl.vm.timeout || !ctrl.vm.writeable
    },
    hook: bind('keyup', (e: KeyboardEvent) => {
      const el = e.target as HTMLInputElement;
      const txt = el.value;
      if (e.which == 10 || e.which == 13) {
        if (txt === '') {
          const kbm = document.querySelector('.keyboard-move input') as HTMLElement;
          if (kbm) kbm.focus();
        } else {
          spam.report(txt);
          if (ctrl.opts.public && spam.hasTeamUrl(txt)) alert("Please don't advertise teams in the chat.");
          else ctrl.post(txt);
          el.value = '';
          el.classList.remove('whisper');
        }
      } else el.classList.toggle('whisper', !!txt.match(whisperRegex));
    })
  })
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
    h('span', '[' + line.c + ']'),
    textNode
  ]);
  var userNode = thunk('a', line.u, userLink, [line.u]);

  return h('li', {
    hook: ctrl.moderation ? bind('click', (e: Event) => {
      const target = e.target as HTMLElement;
      if (ctrl.moderation && target.classList.contains('mod'))
      ctrl.moderation.open((target.getAttribute('data-username') as string).split(' ')[0]);
    }) : {}
  }, ctrl.moderation ? [
    line.u ? lineAction(line.u) : null,
    userNode,
    textNode
  ] : [userNode, textNode]);
}
