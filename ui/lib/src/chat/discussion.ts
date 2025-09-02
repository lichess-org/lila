import * as licon from '../licon';
import * as enhance from '../richText';
import { userLink } from '../view/userLink';
import * as spam from './spam';
import type { Line } from './interfaces';
import { h, thunk, type VNode, type VNodeData } from 'snabbdom';
import { lineAction as modLineAction, flagReport } from './moderation';
import { presetView } from './preset';
import type { ChatCtrl } from './chatCtrl';
import { tempStorage } from '../storage';
import { pubsub } from '../pubsub';
import { alert } from '../view/dialogs';

const whisperRegex = /^\/[wW](?:hisper)?\s/;
const scrollState = { pinToBottom: true, lastScrollTop: 0 };

export default function (ctrl: ChatCtrl): Array<VNode | undefined> {
  if (!ctrl.chatEnabled()) return [];
  const hasMod = !!ctrl.moderation;
  const vnodes = [
    h(
      `ol.mchat__messages.chat-v-${ctrl.vm.domVersion}${hasMod ? '.as-mod' : ''}`,
      {
        attrs: { role: 'log', 'aria-live': 'polite', 'aria-atomic': 'false' },
        hook: {
          insert(vnode) {
            const el = vnode.elm as HTMLElement;
            const $el = $(el).on('click', 'a.jump', (e: Event) => {
              pubsub.emit('jump', (e.target as HTMLElement).getAttribute('data-ply'));
            });
            $el.on('click', '.reply', (e: Event) => {
              const el = e.target as HTMLElement;
              const username = el.parentElement
                ?.querySelector<HTMLLinkElement>('.user-link')
                ?.getAttribute('href')
                ?.slice(3);
              const input = el.closest('.mchat')?.querySelector<HTMLInputElement>('input.mchat__say');
              if (username && input) prependChatInput(input, `@${username} `);
            });
            if (hasMod)
              $el.on('click', '.mod', (e: Event) =>
                ctrl.moderation?.open((e.target as HTMLElement).parentNode as HTMLElement),
              );
            else $el.on('click', '.flag', (e: Event) => flagReport(ctrl, e.target as HTMLElement));

            el.addEventListener('scroll', () => {
              if (el.scrollTop < scrollState.lastScrollTop) scrollState.pinToBottom = false;
              else if (el.scrollTop + el.clientHeight > el.scrollHeight - 10) scrollState.pinToBottom = true;
              scrollState.lastScrollTop = el.scrollTop;
            });

            requestAnimationFrame(() => (el.scrollTop = el.scrollHeight));
          },
          postpatch: (_, vnode) => {
            const el = vnode.elm as HTMLElement;
            if (!scrollState.pinToBottom) return;

            if (document.visibilityState === 'hidden') el.scrollTop = el.scrollHeight;
            else if (el.scrollTop + el.clientHeight < el.scrollHeight)
              el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' });

            scrollState.lastScrollTop = el.scrollTop;
          },
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
      attrs: { placeholder: i18n.site.loginToChat, disabled: true },
    });
  let placeholder: string;
  if (ctrl.vm.timeout) placeholder = i18n.site.youHaveBeenTimedOut;
  else if (ctrl.opts.blind) placeholder = 'Chat';
  else placeholder = i18n.site.talkInChat;
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

function prependChatInput(chatInput: HTMLInputElement, prefix: string): void {
  if (!chatInput.value.includes(prefix)) chatInput.value = prefix + chatInput.value;
  chatInput.focus();
  chatInput.dispatchEvent(new Event('input', { bubbles: true, cancelable: true }));
}

let mouchListener: EventListener;

const setupHooks = (ctrl: ChatCtrl, chatEl: HTMLInputElement) => {
  const inner = tempStorage.make(`chat.input`);
  const storage = {
    get: (): string | undefined => {
      const v = inner.get();
      if (v) {
        try {
          const parsed = JSON.parse(v);
          if (parsed[0] === (ctrl.data.opponentId || '')) {
            return parsed[1] as string;
          }
        } catch (e) {
          console.log(`Could not parse "chat.input" value ${v}`);
        }
      }
      return;
    },
    set: (txt: string) => {
      inner.set(JSON.stringify([ctrl.data.opponentId || '', txt]));
    },
    inner,
  };
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
        else {
          scrollState.pinToBottom = true;
          ctrl.post(txt);
        }
        el.value = '';
        storage.inner.remove();
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

  site.mousetrap.bind('c', () => chatEl.focus(), undefined, false);

  // Ensure clicks remove chat focus.
  // See https://github.com/lichess-org/lila/pull/5323

  const mouchEvents = ['touchstart', 'mousedown'];

  if (mouchListener)
    mouchEvents.forEach(event => document.body.removeEventListener(event, mouchListener, { capture: true }));

  mouchListener = (e: MouseEvent) => {
    if (!e.shiftKey && e.buttons !== 2 && e.button !== 2 && e.target !== chatEl) chatEl.blur();
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
      (!line.r || (line.u || '').toLowerCase() === ctrl.data.userId) &&
      !spam.skip(line.t)
    )
      ls.push(line);
    prev = line;
  });
  return ls;
}

const updateText = (opts?: enhance.EnhanceOpts) => (oldVnode: VNode, vnode: VNode) => {
  if ((vnode.data as VNodeData).lichessChat !== (oldVnode.data as VNodeData).lichessChat)
    (vnode.elm as HTMLElement).innerHTML = enhance.enhance((vnode.data as VNodeData).lichessChat, opts);
};

const profileLinkRegex = /(https:\/\/)?lichess\.org\/@\/([a-zA-Z0-9_-]+)/g;

const processProfileLink = (text: string) => text.replace(profileLinkRegex, '@$2');

function renderText(t: string, opts?: enhance.EnhanceOpts) {
  const processedText = processProfileLink(t);
  if (enhance.isMoreThanText(processedText)) {
    const hook = updateText(opts);
    return h('t', { lichessChat: processedText, hook: { create: hook, update: hook } });
  }
  return h('t', processedText);
}

const userThunk = (name: string, title?: string, patron?: boolean, flair?: Flair) =>
  userLink({ name, title, patron, line: !!patron, flair });

const actionIcons = (ctrl: ChatCtrl, line: Line): Array<VNode | null> => {
  if (!ctrl.data.userId || !line.u || ctrl.data.userId === line.u) return [];
  const icons = [];
  if (ctrl.canPostArbitraryText() && !ctrl.data.resourceId.startsWith('game'))
    icons.push(
      h('action.reply', {
        attrs: { 'data-icon': licon.Back, title: 'Reply' },
      }),
    );
  icons.push(
    ctrl.moderation
      ? modLineAction()
      : h('action.flag', {
          attrs: { 'data-icon': licon.CautionTriangle, title: 'Report', 'data-text': line.t },
        }),
  );
  return icons;
};

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
      ?.find(mention => mention.trim().toLowerCase() === `@${ctrl.data.userId}`);

  return h(
    'li',
    {
      class: {
        me: userId === myUserId,
        host: !!(userId && ctrl.data.hostIds?.includes(userId)),
        mentioned,
      },
    },
    [...actionIcons(ctrl, line), userNode, ' ', textNode],
  );
}
