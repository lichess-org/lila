import assert from 'node:assert/strict';
import { beforeEach, describe, mock, test } from 'node:test';
import { attributesModule, classModule, h, init, propsModule } from 'snabbdom';

import type { ChatCtrl as ChatCtrlType } from '../src/chat/chatCtrl';
import type { ChatData, ChatOpts, ChatPlugin } from '../src/chat/interfaces';

document.documentElement.lang = 'en-US';

mock.module(new URL('../src/socket.ts', import.meta.url).href, { namedExports: { wsSend: () => {} } });

const { ChatCtrl } = await import('../src/chat/chatCtrl');
const { renderChat } = await import('../src/chat/renderChat');

const patch = init([attributesModule, classModule, propsModule]);

const chatOpts = (extra: Partial<ChatOpts> = {}): ChatOpts => ({
  data: {
    id: 'chat-1',
    name: 'Chat room',
    lines: [],
    userId: 'tester',
    resourceType: 'team',
    resourceId: 'team:test',
    loginRequired: false,
    restricted: false,
  } satisfies ChatData,
  writeable: true,
  kobold: false,
  blind: false,
  timeout: false,
  public: true,
  permissions: {},
  kidMode: false,
  ...extra,
});

const pluginTab = (): ChatPlugin => ({
  key: 'plugin',
  name: 'Plugin',
  view: () => h('div.plugin'),
});

const mount = (ctrl: ChatCtrlType): HTMLElement => {
  const el = document.createElement('div');
  document.body.appendChild(el);
  return patch(el, renderChat(ctrl)).elm as HTMLElement;
};

beforeEach(() => {
  localStorage.clear();
  document.body.innerHTML = '';
  document.body.dataset.user = 'tester';
});

describe('chat tab selection', () => {
  test('honors the stored tab while chat is enabled', () => {
    const ctrl = new ChatCtrl(chatOpts({ noteId: 'note-1', noteText: '' }), () => {});
    ctrl.setTab(ctrl.visibleTabs[1]);
    assert.equal(ctrl.getTab().key, 'note');
  });

  test('falls back to the first non-discussion tab when chat is off', () => {
    const ctrl = new ChatCtrl(chatOpts({ noteId: 'note-1', noteText: '', plugin: pluginTab() }), () => {});
    assert.equal(ctrl.getTab().key, 'discussion');
    ctrl.chatEnabled(false);
    assert.equal(ctrl.getTab().key, 'note');
  });

  test('keeps a stored non-discussion tab when chat is off', () => {
    const ctrl = new ChatCtrl(chatOpts({ noteId: 'note-1', noteText: '', plugin: pluginTab() }), () => {});
    ctrl.setTab(ctrl.visibleTabs[1]);
    ctrl.chatEnabled(false);
    assert.equal(ctrl.getTab().key, 'note');
  });

  test('falls back to the plugin tab when it is the only non-discussion tab', () => {
    const ctrl = new ChatCtrl(chatOpts({ plugin: pluginTab() }), () => {});
    ctrl.chatEnabled(false);
    assert.equal(ctrl.getTab().key, 'plugin');
  });

  test('keeps the discussion tab when it is the only tab and chat is off', () => {
    const ctrl = new ChatCtrl(chatOpts(), () => {});
    ctrl.chatEnabled(false);
    assert.equal(ctrl.getTab().key, 'discussion');
  });
});

describe('chat toggle rendering', () => {
  test('renders the toggle beside the discussion tab with notes and a plugin tab', () => {
    const plugin = pluginTab();
    const ctrl = new ChatCtrl(chatOpts({ noteId: 'note-1', noteText: '', plugin }), () => {});
    ctrl.setTab(plugin);
    ctrl.chatEnabled(false);

    const el = mount(ctrl);
    assert.equal(el.querySelectorAll('.mchat__tab').length, 3);

    const discussion = el.querySelector('.mchat__tab.discussion');
    assert.ok(discussion?.querySelector('.cmn-toggle'));
    assert.equal(discussion?.querySelector<HTMLInputElement>('input[type=checkbox]')?.checked, false);

    assert.ok(el.querySelector('.mchat__content.plugin'));
    assert.equal(el.querySelector('.mchat__messages'), null);
    assert.equal(el.querySelector('.mchat__say'), null);
  });

  test('keeps non-discussion tabs usable while chat is off', () => {
    const plugin = pluginTab();
    const ctrl = new ChatCtrl(chatOpts({ noteId: 'note-1', noteText: '', plugin }), () => {});
    ctrl.setTab(plugin);
    ctrl.chatEnabled(false);

    const el = mount(ctrl);
    (el.querySelector('.mchat__tab.note') as HTMLButtonElement).click();
    assert.equal(ctrl.getTab().key, 'note');
  });

  test('persists chatEnabled when the toggle changes', () => {
    const ctrl = new ChatCtrl(chatOpts({ plugin: pluginTab() }), () => {});
    ctrl.chatEnabled(false);

    const el = mount(ctrl);
    const toggle = el.querySelector<HTMLInputElement>('.mchat__tab.discussion input[type=checkbox]')!;
    toggle.checked = true;
    toggle.dispatchEvent(new window.Event('change', { bubbles: true }));

    assert.equal(ctrl.chatEnabled(), true);
    assert.equal(localStorage.getItem('chat.team.enabled.tester'), 'true');
  });
});
