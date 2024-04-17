import * as licon from 'common/licon';
import * as enhance from 'common/richText';
import { userLink } from 'common/userLink';
import * as spam from './spam';
import { Line } from './interfaces';
import { h, thunk, VNode, VNodeData } from 'snabbdom';
import { lineAction as modLineAction, report } from './moderation';
import { presetView } from './preset';
import ChatCtrl from './ctrl';

const whisperRegex = /^\/[wW](?:hisper)?\s/;

export default function (ctrl: ChatCtrl): Array<VNode | undefined> {
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
    hasMod = !!ctrl.moderation;
  const vnodes = [
    h(
      `ol.mchat__messages.chat-v-${ctrl.vm.domVersion}${hasMod ? '.as-mod' : ''}`,
      {
        attrs: { role: 'log', 'aria-live': 'polite', 'aria-atomic': 'false' },
        hook: {
          insert(vnode) {
            const $el = $(vnode.elm as HTMLElement).on('click', 'a.jump', (e: Event) => {
              site.pubsub.emit('jump', (e.target as HTMLElement).getAttribute('data-ply'));
            });
            if (hasMod)
              $el.on(
                'click',
                '.mod',
                (e: Event) => ctrl.moderation?.open((e.target as HTMLElement).parentNode as HTMLElement),
              );
            else
              $el.on('click', '.flag', (e: Event) =>
                report(ctrl, (e.target as HTMLElement).parentNode as HTMLElement),
              );
            scrollCb(vnode);
          },
          postpatch: (_, vnode) => scrollCb(vnode),
        },
      },
      selectLines(ctrl).map(line => renderLine(ctrl, line)),
    ),
    renderInput(ctrl),
  ];
  const presets = presetView(ctrl.preset);
  if (presets) vnodes.push(presets);
  return vnodes;
}

function renderInput(ctrl: ChatCtrl): VNode | undefined {
  if (!ctrl.vm.writeable) return;
  if ((ctrl.data.loginRequired && !ctrl.data.userId) || ctrl.data.restricted)
    return h('input.mchat__say', {
      attrs: { placeholder: ctrl.trans('loginToChat'), disabled: true },
    });
  let placeholder: string;
  if (ctrl.vm.timeout) placeholder = ctrl.trans('youHaveBeenTimedOut');
  else if (ctrl.opts.blind) placeholder = 'Chat';
  else placeholder = ctrl.trans.noarg(ctrl.vm.placeholderKey);
  return h('input.mchat__say', {
    attrs: {
      placeholder,
      autocomplete: 'off',
      enterkeyhint: 'send',
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

const setupHooks = (ctrl: ChatCtrl, chatEl: HTMLInputElement) => {
  const storage = site.tempStorage.make('chat.input');
  const previousText = storage.get();
  if (previousText) {
    chatEl.value = previousText;
    chatEl.focus();
    if (!ctrl.opts.public && previousText.match(whisperRegex)) chatEl.classList.add('whisper');
  } else if (ctrl.vm.autofocus) chatEl.focus();

  chatEl.addEventListener('keydown', (e: KeyboardEvent) => {
    if (e.key !== 'Enter') return;

    setTimeout(() => {
      const el = e.target as HTMLInputElement,
        txt = el.value,
        pub = ctrl.opts.public;

      if (txt === '')
        $('.input-move input').each(function (this: HTMLInputElement) {
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
    }),
  );

  site.mousetrap.bind('c', () => chatEl.focus());

  // Ensure clicks remove chat focus.
  // See lichess-org/chessground#109

  const mouchEvents = ['touchstart', 'mousedown'];

  if (mouchListener)
    mouchEvents.forEach(event => document.body.removeEventListener(event, mouchListener, { capture: true }));

  mouchListener = (e: MouseEvent) => {
    if (!e.shiftKey && e.buttons !== 2 && e.button !== 2) chatEl.blur();
  };

  chatEl.onfocus = () =>
    mouchEvents.forEach(event =>
      document.body.addEventListener(event, mouchListener, { passive: true, capture: true }),
    );

  chatEl.onblur = () =>
    mouchEvents.forEach(event => document.body.removeEventListener(event, mouchListener, { capture: true }));
};

const sameLines = (l1: Line, l2: Line) => l1.d && l2.d && l1.u === l2.u;

function selectLines(ctrl: ChatCtrl): Array<Line> {
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

const updateText = (opts?: enhance.EnhanceOpts) => (oldVnode: VNode, vnode: VNode) => {
  if ((vnode.data as VNodeData).lichessChat !== (oldVnode.data as VNodeData).lichessChat) {
    (vnode.elm as HTMLElement).innerHTML = enhance.enhance((vnode.data as VNodeData).lichessChat, opts);
  }
};

function renderText(t: string, opts?: enhance.EnhanceOpts) {
  if (enhance.isMoreThanText(t)) {
    const hook = updateText(opts);
    return h('t', { lichessChat: t, hook: { create: hook, update: hook } });
  }
  return h('t', t);
}

const userThunk = (name: string, title?: string, patron?: boolean, flair?: Flair) =>
  userLink({ name, title, patron, line: !!patron, flair });

function renderLine(ctrl: ChatCtrl, line: Line): VNode {
  const textNode = renderText(line.t, ctrl.opts.enhance);

  if (line.u === 'lichess') return h('li.system', textNode);

  if (line.c) return h('li', [h('span.color', '[' + line.c + ']'), textNode]);

  const userNode = thunk('a', line.u, userThunk, [line.u, line.title, line.p, line.f]);
  const userId = line.u?.toLowerCase();

  const myUserId = ctrl.data.userId;
  const mentioned =
    !!myUserId &&
    !!line.t
      .match(enhance.userPattern)
      ?.find(mention => mention.trim().toLowerCase() == `@${ctrl.data.userId}`);

  return h(
    'li',
    {
      class: {
        me: userId === myUserId,
        host: !!(userId && ctrl.data.hostIds?.includes(userId)),
        mentioned,
      },
    },
    ctrl.moderation
      ? [line.u ? modLineAction() : null, userNode, ' ', textNode]
      : [
          myUserId && line.u && myUserId != line.u
            ? h('action.flag', {
                attrs: { 'data-icon': licon.CautionTriangle, title: 'Report' },
              })
            : null,
          userNode,
          ' ',
          textNode,
        ],
  );
}
