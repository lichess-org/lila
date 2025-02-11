import type {
  ChatOpts,
  Line,
  Tab,
  ViewModel,
  Redraw,
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
import { prop } from 'common';
import { storage, type LichessStorage } from 'common/storage';
import { pubsub, type PubsubEvent, type PubsubCallback } from 'common/pubsub';
import { alert } from 'common/dialog';

export default class ChatCtrl {
  data: ChatData;
  private maxLines = 200;
  private maxLinesDrop = 50; // how many lines to drop at once
  private subs: [PubsubEvent, PubsubCallback][];

  allTabs: Tab[] = [];
  palantir: ChatPalantir;
  tabStorage: LichessStorage = storage.make('chat.tab');
  storedTab: string | null = this.tabStorage.get();
  moderation: ModerationCtrl | undefined;
  note: NoteCtrl | undefined;
  preset: PresetCtrl;
  vm: ViewModel;

  constructor(
    readonly opts: ChatOpts,
    readonly redraw: Redraw,
  ) {
    this.data = opts.data;
    if (!opts.kidMode) this.allTabs.push('discussion');
    if (opts.noteId) this.allTabs.push('note');
    if (!opts.kidMode && opts.plugin) this.allTabs.push(opts.plugin.tab.key);
    this.palantir = {
      instance: undefined,
      loaded: false,
      enabled: prop(!opts.kidMode && !!this.data.palantir),
    };
    const noChat = storage.get('nochat');
    this.vm = {
      tab: this.allTabs.find(tab => tab === this.storedTab) || this.allTabs[0],
      enabled: opts.alwaysEnabled || !noChat,
      loading: false,
      autofocus: false,
      timeout: opts.timeout,
      writeable: opts.writeable,
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

    /* If discussion is disabled, and we have another chat tab,
     * then select that tab over discussion */
    if (this.allTabs.length > 1 && this.vm.tab === 'discussion' && noChat) this.vm.tab = this.allTabs[1];
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

    this.emitEnabled();
  }

  get plugin(): ChatPlugin | undefined {
    return this.opts.plugin;
  }

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
    this.redraw();
  };

  private onPermissions = (obj: Permissions): void => {
    let p: keyof Permissions;
    for (p in obj) this.opts.permissions[p] = obj[p];
    this.instanciateModeration();
    this.redraw();
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
      site.asset.loadCssPath('chat.mod');
    }
  };

  destroy = (): void => {
    this.subs.forEach(([eventName, callback]) => pubsub.off(eventName, callback));
  };

  private emitEnabled = (): void => pubsub.emit('chat.enabled', this.vm.enabled);

  setTab = (t: Tab): void => {
    this.vm.tab = t;
    this.vm.autofocus = true;
    this.tabStorage.set(t);
    this.redraw();
  };

  setEnabled = (v: boolean): void => {
    this.vm.enabled = v;
    this.emitEnabled();
    if (!v) storage.set('nochat', '1');
    else storage.remove('nochat');
    this.redraw();
  };
}
