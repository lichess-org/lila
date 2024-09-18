import {
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
import { PresetCtrl, presetCtrl } from './preset';
import { noteCtrl } from './note';
import { moderationCtrl } from './moderation';
import { prop } from 'common';
import { trans } from 'common/i18n';
import { storage, type LichessStorage } from 'common/storage';
import { pubsub } from 'common/pubsub';

export default class ChatCtrl {
  data: ChatData;
  private maxLines = 200;
  private maxLinesDrop = 50; // how many lines to drop at once

  allTabs: Tab[] = ['discussion'];
  palantir: ChatPalantir;
  tabStorage: LichessStorage = storage.make('chat.tab');
  storedTab: string | null = this.tabStorage.get();
  moderation: ModerationCtrl | undefined;
  note: NoteCtrl | undefined;
  preset: PresetCtrl;
  trans: Trans;
  vm: ViewModel;

  constructor(
    readonly opts: ChatOpts,
    readonly redraw: Redraw,
  ) {
    this.data = opts.data;
    if (opts.noteId) this.allTabs.push('note');
    if (opts.plugin) this.allTabs.push(opts.plugin.tab.key);
    this.palantir = {
      instance: undefined,
      loaded: false,
      enabled: prop(!!this.data.palantir),
    };
    this.trans = trans(this.opts.i18n);
    const noChat = storage.get('nochat');
    this.vm = {
      tab: this.allTabs.find(tab => tab === this.storedTab) || this.allTabs[0],
      enabled: opts.alwaysEnabled || !noChat,
      placeholderKey: 'talkInChat',
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
        trans: this.trans,
        redraw: this.redraw,
      })
      : undefined;

    this.preset = presetCtrl({
      initialGroup: opts.preset,
      post: this.post,
      redraw: this.redraw,
    });

    /* If discussion is disabled, and we have another chat tab,
     * then select that tab over discussion */
    if (this.allTabs.length > 1 && this.vm.tab === 'discussion' && noChat) this.vm.tab = this.allTabs[1];
    this.instanciateModeration();

    pubsub.on('socket.in.message', this.onMessage);
    pubsub.on('socket.in.chat_timeout', this.onTimeout);
    pubsub.on('socket.in.chat_reinstate', this.onReinstate);
    pubsub.on('chat.writeable', this.onWriteable);
    pubsub.on('chat.permissions', this.onPermissions);
    pubsub.on('palantir.toggle', this.palantir.enabled);

    this.emitEnabled();
  }

  get plugin(): ChatPlugin | undefined {
    return this.opts.plugin;
  }

  post = (text: string): boolean => {
    text = text.trim();
    if (!text) return false;
    if (text == 'You too!' && !this.data.lines.some(l => l.u != this.data.userId)) return false;
    if (text.length > 140) {
      alert('Max length: 140 chars. ' + text.length + ' chars used.');
      return false;
    }
    pubsub.emit('socket.send', 'talk', text);
    return true;
  };

  onTimeout = (userId: string): void => {
    let change = false;
    this.data.lines.forEach(l => {
      if (l.u && l.u.toLowerCase() == userId) {
        l.d = true;
        change = true;
      }
    });
    if (userId == this.data.userId) this.vm.timeout = change = true;
    if (change) {
      this.vm.domVersion++;
      this.redraw();
    }
  };

  onReinstate = (userId: string): void => {
    if (userId == this.data.userId) {
      this.vm.timeout = false;
      this.redraw();
    }
  };

  onMessage = (line: Line): void => {
    this.data.lines.push(line);
    const nb = this.data.lines.length;
    if (nb > this.maxLines) {
      this.data.lines.splice(0, nb - this.maxLines + this.maxLinesDrop);
      this.vm.domVersion++;
    }
    this.redraw();
  };

  onWriteable = (v: boolean): void => {
    this.vm.writeable = v;
    this.redraw();
  };

  onPermissions = (obj: Permissions): void => {
    let p: keyof Permissions;
    for (p in obj) this.opts.permissions[p] = obj[p];
    this.instanciateModeration();
    this.redraw();
  };

  private instanciateModeration = () => {
    if (this.opts.permissions.timeout || this.opts.permissions.broadcast || this.opts.permissions.local) {
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
    pubsub.off('socket.in.message', this.onMessage);
    pubsub.off('socket.in.chat_timeout', this.onTimeout);
    pubsub.off('socket.in.chat_reinstate', this.onReinstate);
    pubsub.off('chat.writeable', this.onWriteable);
    pubsub.off('chat.permissions', this.onPermissions);
    pubsub.off('palantir.toggle', this.palantir.enabled);
  };

  emitEnabled = (): void => pubsub.emit('chat.enabled', this.vm.enabled);

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
