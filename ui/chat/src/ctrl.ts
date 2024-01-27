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
} from './interfaces';
import { PresetCtrl, presetCtrl } from './preset';
import { noteCtrl } from './note';
import { moderationCtrl } from './moderation';
import { prop } from 'common';

export default class ChatCtrl {
  data: ChatData;
  private maxLines = 200;
  private maxLinesDrop = 50; // how many lines to drop at once
  private subs: [string, PubsubCallback][];

  allTabs: Tab[] = ['discussion'];
  palantir: ChatPalantir;
  tabStorage = lichess.storage.make('chat.tab');
  storedTab = this.tabStorage.get();
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
    this.trans = lichess.trans(this.opts.i18n);
    const noChat = lichess.storage.get('nochat');
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

    this.subs = [
      ['socket.in.message', this.onMessage],
      ['socket.in.chat_timeout', this.onTimeout],
      ['socket.in.chat_reinstate', this.onReinstate],
      ['chat.writeable', this.onWriteable],
      ['chat.permissions', this.onPermissions],
      ['palantir.toggle', this.palantir.enabled],
    ];

    this.subs.forEach(([eventName, callback]) => lichess.pubsub.on(eventName, callback));

    this.emitEnabled();
  }

  get plugin() {
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
    lichess.pubsub.emit('socket.send', 'talk', text);
    return true;
  };

  onTimeout = (userId: string) => {
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

  onReinstate = (userId: string) => {
    if (userId == this.data.userId) {
      this.vm.timeout = false;
      this.redraw();
    }
  };

  onMessage = (line: Line) => {
    this.data.lines.push(line);
    const nb = this.data.lines.length;
    if (nb > this.maxLines) {
      this.data.lines.splice(0, nb - this.maxLines + this.maxLinesDrop);
      this.vm.domVersion++;
    }
    this.redraw();
  };

  onWriteable = (v: boolean) => {
    this.vm.writeable = v;
    this.redraw();
  };

  onPermissions = (obj: Permissions) => {
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
      lichess.asset.loadCssPath('chat.mod');
    }
  };

  destroy = () => {
    this.subs.forEach(([eventName, callback]) => lichess.pubsub.off(eventName, callback));
  };

  emitEnabled = () => lichess.pubsub.emit('chat.enabled', this.vm.enabled);

  setTab = (t: Tab) => {
    this.vm.tab = t;
    this.vm.autofocus = true;
    this.tabStorage.set(t);
    this.redraw();
  };

  setEnabled = (v: boolean) => {
    this.vm.enabled = v;
    this.emitEnabled();
    if (!v) lichess.storage.set('nochat', '1');
    else lichess.storage.remove('nochat');
    this.redraw();
  };
}
