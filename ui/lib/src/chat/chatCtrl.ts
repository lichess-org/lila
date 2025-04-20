import type {
  ChatOpts,
  Line,
  Tab,
  ViewModel,
  Permissions,
  ModerationCtrl,
  ChatData,
  NoteCtrl,
  ChatPalantir,
  ChatPlugin,
} from './interfaces';
import { type PresetCtrl, presetCtrl } from './preset';
import { noteCtrl } from './note';
import { moderationCtrl } from './moderation';
import { prop, type Prop } from '../common';
import { storedStringProp, storedBooleanProp } from '../storage';
import { pubsub, type PubsubEvent, type PubsubCallback } from '../pubsub';
import { alert } from '../view/dialogs';
import { isContained } from '@/algo';

export class ChatCtrl {
  data: ChatData;
  private maxLines = 200;
  private maxLinesDrop = 50; // how many lines to drop at once
  private subs: [PubsubEvent, PubsubCallback][];
  private storedTabKey: Prop<string>;
  private allTabs: Tab[] = [];

  chatEnabled: Prop<boolean> = storedBooleanProp('chat.enabled', true);
  palantir: ChatPalantir;
  moderation: ModerationCtrl | undefined;
  note: NoteCtrl | undefined;
  preset: PresetCtrl;
  vm: ViewModel;

  constructor(
    readonly opts: ChatOpts,
    readonly redraw: Redraw,
  ) {
    this.data = opts.data;
    this.storedTabKey = storedStringProp(`chat.${opts.plugin ? opts.plugin.key + '.' : ''}tab`, 'discussion');
    if (!opts.kidMode) this.allTabs.push({ key: 'discussion' });
    if (opts.noteId) this.allTabs.push({ key: 'note' });
    if (opts.plugin && (opts.plugin.kidSafe || !opts.kidMode)) {
      opts.plugin.redraw = redraw;
      this.allTabs.push(opts.plugin);
    }
    this.palantir = {
      instance: undefined,
      loaded: false,
      enabled: prop(!opts.kidMode && !!this.data.palantir),
    };
    this.vm = {
      loading: false,
      autofocus: false,
      timeout: opts.timeout ?? false,
      writeable: opts.writeable ?? false,
      domVersion: 1, // increment to force redraw
    };

    this.note = opts.noteId
      ? noteCtrl({
          id: opts.noteId,
          text: opts.noteText,
          redraw: this.redraw,
        })
      : undefined;

    this.preset = presetCtrl({
      initialGroup: opts.preset,
      post: this.post,
      redraw: this.redraw,
    });

    if (opts.kidMode) return;

    this.instanciateModeration();

    this.subs = [
      ['socket.in.message', this.onMessage],
      ['socket.in.chat_timeout', this.onTimeout],
      ['socket.in.chat_reinstate', this.onReinstate],
      ['chat.writeable', this.onWriteable],
      ['chat.permissions', this.onPermissions],
      ['palantir.toggle', this.palantir.enabled],
    ];

    this.subs.forEach(([eventName, callback]) => pubsub.on(eventName, callback));
  }

  get isOptional(): boolean {
    const tabs = this.visibleTabs;
    return tabs.length === 1 && tabs[0].key === 'discussion';
  }

  get visibleTabs(): Tab[] {
    return this.allTabs.filter(x => !x.isDisabled?.());
  }

  get plugin(): ChatPlugin | undefined {
    return this.opts.plugin;
  }

  canPostArbitraryText = (): boolean =>
    this.vm.writeable &&
    !this.vm.timeout &&
    (!this.data.loginRequired || !!this.data.userId) &&
    !this.data.restricted;

  post = (text: string): boolean => {
    text = text.trim();
    if (!text) return false;
    if (text === 'You too!' && !this.data.lines.some(l => l.u !== this.data.userId)) return false;
    if (text.length > 140) {
      alert('Max length: 140 chars. ' + text.length + ' chars used.');
      return false;
    }
    pubsub.emit('socket.send', 'talk', text);
    return true;
  };

  listenToIncoming = (cb: (line: Line) => void): void => pubsub.on('socket.in.message', cb);

  private onTimeout = (userId: string): void => {
    let change = false;
    this.data.lines.forEach(l => {
      if (l.u && l.u.toLowerCase() === userId) {
        l.d = true;
        change = true;
      }
    });
    if (userId === this.data.userId) this.vm.timeout = change = true;
    if (change) {
      this.vm.domVersion++;
      this.redraw();
    }
  };

  private onReinstate = (userId: string): void => {
    if (userId === this.data.userId) {
      this.vm.timeout = false;
      this.redraw();
    }
  };

  private onMessage = (line: Line): void => {
    this.data.lines.push(line);
    const nb = this.data.lines.length;
    if (nb > this.maxLines) {
      this.data.lines.splice(0, nb - this.maxLines + this.maxLinesDrop);
      this.vm.domVersion++;
    }
    this.redraw();
  };

  private onWriteable = (v: boolean): void => {
    this.vm.writeable = v;
  };

  private onPermissions = (obj: Permissions): void => {
    if (isContained(this.opts.permissions, obj)) return;
    Object.assign(this.opts.permissions, obj);
    this.instanciateModeration();
  };

  private instanciateModeration = () => {
    if (this.opts.permissions.timeout || this.opts.permissions.broadcast || this.opts.permissions.local) {
      this.maxLines = 1000;
      this.maxLinesDrop = 500;
      this.moderation = moderationCtrl({
        reasons: this.opts.timeoutReasons || [{ key: 'other', name: 'Inappropriate behavior' }],
        permissions: this.opts.permissions,
        resourceId: this.data.resourceId,
        redraw: this.redraw,
      });
      site.asset.loadCssPath('lib.chat.mod');
    }
  };

  destroy = (): void => {
    this.subs.forEach(([eventName, callback]) => pubsub.off(eventName, callback));
  };

  setTab = (tab: Tab = this.getTab()): Tab => {
    this.vm.autofocus = true;
    this.storedTabKey(tab.key);
    return tab;
  };

  getTab(): Tab {
    const tabs = this.visibleTabs;
    return this.chatEnabled()
      ? (tabs.find(t => t.key === this.storedTabKey()) ?? tabs[0])
      : tabs[tabs.length - 1];
  }
}
