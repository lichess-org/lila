import * as enhance from 'common/richText';
import * as spam from './spam';
import { Ctrl, Line } from './interfaces';
import { flag } from './xhr';
import { h, thunk, VNode, VNodeData } from 'snabbdom';
import { lineAction as modLineAction } from './moderation';
import { presetView } from './preset';
import { userLink } from './util';

const whisperRegex = /^\/[wW](?:hisper)?\s/;

export default function (ctrl: Ctrl): Array<VNode | undefined> {
  if (!ctrl.vm.enabled) return [];
  const scrollCb = (vnode: VNode) => {
      const el = vnode.elm as HTMLElement;
      if (ctrl.data.lines.length > 5) {
        const autoScroll = el.scrollTop === 0 || el.scrollTop > el.scrollHeight - el.clientHeight - 100;
        if (autoScroll) {
          el.scrollTop = 999999;
          setTimeout((_: any) => (el.scrollTop = 999999), 300);
        }
      }
    },
    hasMod = !!ctrl.moderation();
  const vnodes = [
    h(
      'ol.mchat__messages.chat-v-' + ctrl.data.domVersion,
      {
        attrs: {
          role: 'log',
          'aria-live': 'polite',
          'aria-atomic': 'false',
        },
        hook: {
          insert(vnode) {
            const $el = $(vnode.elm as HTMLElement).on('click', 'a.jump', (e: Event) => {
              lichess.pubsub.emit('jump', (e.target as HTMLElement).getAttribute('data-ply'));
            });
            if (hasMod)
              $el.on('click', '.mod', (e: Event) =>
                ctrl.moderation()?.open((e.target as HTMLElement).parentNode as HTMLElement)
              );
            else
              $el.on('click', '.flag', (e: Event) => report(ctrl, (e.target as HTMLElement).parentNode as HTMLElement));
            scrollCb(vnode);
          },
          postpatch: (_, vnode) => scrollCb(vnode),
        },
      },
      selectLines(ctrl).map(line => renderLine(ctrl, line))
    ),
    renderInput(ctrl),
  ];
  const presets = presetView(ctrl.preset);
  if (presets) vnodes.push(presets);
  return vnodes;
}

function renderInput(ctrl: Ctrl): VNode | undefined {
  if (!ctrl.vm.writeable) return;
  if ((ctrl.data.loginRequired && !ctrl.data.userId) || ctrl.data.restricted)
    return h('input.mchat__say', {
      attrs: {
        placeholder: ctrl.trans('loginToChat'),
        disabled: true,
      },
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
      disabled: ctrl.vm.timeout || !ctrl.vm.writeable,
      'aria-label': 'Chat input',
    },
    hook: {
      insert(vnode) {
        setupHooks(ctrl, vnode.elm as HTMLInputElement);
      },
    },
  });
}

let mouchListener: EventListener;

const setupHooks = (ctrl: Ctrl, chatEl: HTMLInputElement) => {
  const storage = lichess.tempStorage.make('chat.input');
  const previousText = storage.get();
  if (previousText) {
    chatEl.value = previousText;
    chatEl.focus();
    if (!ctrl.opts.public && previousText.match(whisperRegex)) chatEl.classList.add('whisper');
  }

  chatEl.addEventListener('keydown', (e: KeyboardEvent) => {
    if (e.key !== 'Enter') return;

    setTimeout(() => {
      const el = e.target as HTMLInputElement,
        txt = el.value,
        pub = ctrl.opts.public;

      if (txt === '')
        $('.keyboard-move input').each(function (this: HTMLInputElement) {
          this.focus();
        });
      else {
        if (!ctrl.opts.kobold) spam.selfReport(txt);
        if (pub && spam.hasTeamUrl(txt)) alert("Please don't advertise teams in the chat.");
        else ctrl.post(txt);
        el.value = '';
        storage.remove();
        if (!pub) el.classList.remove('whisper');
      }
    });
  });

  chatEl.addEventListener('input', (e: KeyboardEvent) =>
    setTimeout(() => {
      const el = e.target as HTMLInputElement,
        txt = el.value;

      el.removeAttribute('placeholder');
      if (!ctrl.opts.public) el.classList.toggle('whisper', !!txt.match(whisperRegex));
      storage.set(txt);
    })
  );

  window.Mousetrap.bind('c', () => chatEl.focus());

  // Ensure clicks remove chat focus.
  // See ornicar/chessground#109

  const mouchEvents = ['touchstart', 'mousedown'];

  if (mouchListener)
    mouchEvents.forEach(event => document.body.removeEventListener(event, mouchListener, { capture: true }));

  mouchListener = (e: MouseEvent) => {
    if (!e.shiftKey && e.buttons !== 2 && e.button !== 2) chatEl.blur();
  };

  chatEl.onfocus = () =>
    mouchEvents.forEach(event =>
      document.body.addEventListener(event, mouchListener, { passive: true, capture: true })
    );

  chatEl.onblur = () =>
    mouchEvents.forEach(event => document.body.removeEventListener(event, mouchListener, { capture: true }));
};

function sameLines(l1: Line, l2: Line) {
  return l1.d && l2.d && l1.u === l2.u;
}

function selectLines(ctrl: Ctrl): Array<Line> {
  const ls: Array<Line> = [];
  let prev: Line | undefined;
  ctrl.data.lines.forEach(line => {
    if (
      !line.d &&
      (!prev || !sameLines(prev, line)) &&
      (!line.r || (line.u || '').toLowerCase() == ctrl.data.userId) &&
      !spam.skip(line.t)
    )
      ls.push(line);
    prev = line;
  });
  return ls;
}

function updateText(parseMoves: boolean) {
  return (oldVnode: VNode, vnode: VNode) => {
    if ((vnode.data as VNodeData).lichessChat !== (oldVnode.data as VNodeData).lichessChat) {
      (vnode.elm as HTMLElement).innerHTML = enhance.enhance((vnode.data as VNodeData).lichessChat, parseMoves);
    }
  };
}

function renderText(t: string, parseMoves: boolean) {
  if (enhance.isMoreThanText(t)) {
    const hook = updateText(parseMoves);
    return h('t', {
      lichessChat: t,
      hook: {
        create: hook,
        update: hook,
      },
    });
  }
  return h('t', t);
}

function report(ctrl: Ctrl, line: HTMLElement) {
  const userA = line.querySelector('a.user-link') as HTMLLinkElement;
  const text = (line.querySelector('t') as HTMLElement).innerText;
  if (userA && confirm(`Report "${text}" to moderators?`)) flag(ctrl.data.resourceId, userA.href.split('/')[4], text);
}

function renderLine(ctrl: Ctrl, line: Line): VNode {
  const textNode = renderText(line.t, ctrl.opts.parseMoves);

  if (line.u === 'lichess') return h('li.system', textNode);

  if (line.c) return h('li', [h('span.color', '[' + line.c + ']'), textNode]);

  const userNode = thunk('a', line.u, userLink, [line.u, line.title, line.p]);

  return h(
    'li',
    ctrl.moderation()
      ? [line.u ? modLineAction() : null, userNode, ' ', textNode]
      : [
          ctrl.data.userId && line.u && ctrl.data.userId != line.u
            ? h('i.flag', {
                attrs: {
                  'data-icon': '!',
                  title: 'Report',
                },
              })
            : null,
          userNode,
          ' ',
          textNode,
        ]
  );
}
