import * as xhr from 'common/xhr';
import idleTimer from './idleTimer';
import sri from './sri';
import { siteTrans } from './trans';
import { reload } from './reload';
import { storage as makeStorage } from './storage';
import { storedIntProp } from 'common/storage';
import once from './once';

type Sri = string;
type Tpe = string;
type Payload = any;
type Version = number;
interface MsgBase {
  t: Tpe;
  d?: Payload;
}
interface MsgIn extends MsgBase {
  v?: Version;
}
interface MsgOut extends MsgBase {}
interface MsgAck extends MsgOut {
  at: number;
}
type Send = (t: Tpe, d: Payload, o?: any) => void;

interface Options {
  idle: boolean;
  pingMaxLag: number; // time to wait for pong before resetting the connection
  pingDelay: number; // time between pong and ping
  autoReconnectDelay: number;
  protocol: string;
  isAuth: boolean;
  debug?: boolean;
}
interface Params {
  sri: Sri;
  flag?: string;
}
interface Settings {
  receive?: (t: Tpe, d: Payload) => void;
  events: {
    [tpe: string]: (d: Payload | null, msg: MsgIn) => any;
  };
  params?: Params;
  options?: Partial<Options>;
}

const origSend = WebSocket.prototype.send;

const isOnline = () => !('onLine' in navigator) || navigator.onLine;

// versioned events, acks, retries, resync
export default class StrongSocket {
  pubsub = site.pubsub;
  settings: Settings;
  options: Options;
  version: number | false;
  ws: WebSocket | undefined;
  pingSchedule: Timeout;
  connectSchedule: Timeout;
  ackable: Ackable = new Ackable((t, d, o) => this.send(t, d, o));
  lastPingTime: number = performance.now();
  pongCount = 0;
  averageLag = 0;
  tryOtherUrl = false;
  autoReconnect = true;
  nbConnects = 0;
  storage: LichessStorage = makeStorage.make(
    document.body.dataset.socketAlternates ? 'surl-alt' : 'surl17',
    30 * 60 * 1000,
  );
  private _sign?: string;
  private resendWhenOpen: [string, any, any][] = [];
  private baseUrls = (document.body.dataset.socketAlts || document.body.dataset.socketDomains!).split(',');
  static defaultOptions: Options = {
    idle: false,
    pingMaxLag: 9000, // time to wait for pong before resetting the connection
    pingDelay: 2500, // time between pong and ping
    autoReconnectDelay: 3500,
    protocol: location.protocol === 'https:' ? 'wss:' : 'ws:',
    isAuth: document.body.hasAttribute('data-user'),
  };
  static defaultParams: Params = {
    sri: sri,
  };

  static resolveFirstConnect: (send: Send) => void;
  static firstConnect = new Promise<Send>(r => {
    StrongSocket.resolveFirstConnect = r;
  });

  constructor(
    readonly url: string,
    version: number | false,
    settings: Partial<Settings> = {},
  ) {
    this.settings = {
      receive: settings.receive,
      events: settings.events || {},
      params: {
        ...StrongSocket.defaultParams,
        ...(settings.params || {}),
      },
    };
    const customPingDelay = storedIntProp('socket.ping.interval', 2500)();

    this.options = {
      ...StrongSocket.defaultOptions,
      ...(settings.options || {}),
      pingDelay: customPingDelay > 400 ? customPingDelay : 2500,
    };
    this.version = version;
    this.pubsub.on('socket.send', this.send);
    this.connect();
  }

  sign = (s: string) => {
    this._sign = s;
    this.ackable.sign(s);
  };

  connect = () => {
    this.destroy();
    if (!isOnline()) {
      document.body.classList.remove('online');
      document.body.classList.add('offline');
      $('#network-status').text(site ? siteTrans('noNetwork') : 'Offline');
      this.scheduleConnect(1000);
      return;
    }
    this.autoReconnect = true;
    const fullUrl = xhr.url(this.options.protocol + '//' + this.baseUrl() + this.url, {
      ...this.settings.params,
      v: this.version === false ? undefined : this.version,
    });
    this.debug('connection attempt to ' + fullUrl);
    try {
      const ws = (this.ws = new WebSocket(fullUrl));
      ws.onerror = e => this.onError(e);
      ws.onclose = e => this.onClose(e, fullUrl);
      ws.onopen = () => {
        this.debug('connected to ' + fullUrl);
        this.onSuccess();
        const cl = document.body.classList;
        cl.remove('offline');
        cl.add('online');
        cl.toggle('reconnected', this.nbConnects > 1);
        this.pingNow();
        this.resendWhenOpen.forEach(([t, d, o]) => this.send(t, d, o));
        this.resendWhenOpen = [];
        this.pubsub.emit('socket.open');
        this.ackable.resend();
      };
      ws.onmessage = e => {
        if (e.data == 0) return this.pong();
        const m = JSON.parse(e.data);
        if (m.t === 'n') this.pong();
        this.handle(m);
      };
    } catch (e) {
      this.onClose({ code: 4000, reason: String(e) } as CloseEvent, fullUrl);
    }
    this.scheduleConnect(this.options.pingMaxLag);
  };

  send = (t: string, d: any, o: any = {}, noRetry = false) => {
    const msg: Partial<MsgOut> = { t };
    if (d !== undefined) {
      if (o.withLag) d.l = Math.round(this.averageLag);
      if (o.millis >= 0) d.s = Math.round(o.millis * 0.1).toString(36);
      msg.d = d;
    }
    if (o.ackable) {
      msg.d = msg.d || {}; // can't ack message without data
      this.ackable.register(t, msg.d); // adds d.a, the ack ID we expect to get back
    }

    const message = JSON.stringify(msg);
    if (t == 'racerScore' && o.sign != this._sign) return;
    if (t == 'move' && o.sign != this._sign) {
      let stack: string;
      try {
        stack = new Error().stack!.split('\n').join(' / ').replace(/\s+/g, ' ');
      } catch (e: any) {
        stack = `${e.message} ${navigator.userAgent}`;
      }
      if (!stack.includes('round.nvui'))
        setTimeout(() => {
          if (once(`socket.rep.${Math.round(Date.now() / 1000 / 3600 / 3)}`))
            this.send('rep', { n: `soc: ${message} ${stack}` });
          else site.socket.destroy();
        }, 10000);
    }
    this.debug('send ' + message);
    if (!this.ws || this.ws.readyState === WebSocket.CONNECTING) {
      if (!noRetry) this.resendWhenOpen.push([t, msg.d, o]);
    } else {
      origSend.apply(this.ws, [message]);
    }
  };

  scheduleConnect = (delay: number) => {
    if (this.options.idle) delay = 10 * 1000 + Math.random() * 10 * 1000;
    // debug('schedule connect ' + delay);
    clearTimeout(this.pingSchedule);
    clearTimeout(this.connectSchedule);
    this.connectSchedule = setTimeout(() => {
      document.body.classList.add('offline');
      document.body.classList.remove('online');
      $('#network-status').text(site ? siteTrans('reconnecting') : 'Reconnecting');
      if (!this.tryOtherUrl && isOnline()) {
        // if this was set earlier, we've already logged the error
        this.tryOtherUrl = true;
        site.log(`sri ${this.settings.params!.sri} timeout ${delay}ms, trying ${this.baseUrl()}${this.url}`);
      }
      this.connect();
    }, delay);
  };

  schedulePing = (delay: number) => {
    clearTimeout(this.pingSchedule);
    this.pingSchedule = setTimeout(this.pingNow, delay);
  };

  pingNow = () => {
    clearTimeout(this.pingSchedule);
    clearTimeout(this.connectSchedule);
    const pingData =
      this.options.isAuth && this.pongCount % 10 == 2
        ? JSON.stringify({
            t: 'p',
            l: Math.round(0.1 * this.averageLag),
          })
        : 'null';
    try {
      this.ws!.send(pingData);
      this.lastPingTime = performance.now();
    } catch (e) {
      this.debug(e, true);
    }
    this.scheduleConnect(this.options.pingMaxLag);
  };

  computePingDelay = () => this.options.pingDelay + (this.options.idle ? 1000 : 0);

  pong = () => {
    clearTimeout(this.connectSchedule);
    this.schedulePing(this.computePingDelay());
    const currentLag = Math.min(performance.now() - this.lastPingTime, 10000);
    this.pongCount++;

    // Average first 4 pings, then switch to decaying average.
    const mix = this.pongCount > 4 ? 0.1 : 1 / this.pongCount;
    this.averageLag += mix * (currentLag - this.averageLag);

    this.pubsub.emit('socket.lag', this.averageLag);
  };

  handle = (m: MsgIn) => {
    if (m.v && this.version !== false) {
      if (m.v <= this.version) {
        this.debug('already has event ' + m.v);
        return;
      }
      // it's impossible but according to previous logging, it happens nonetheless
      if (m.v > this.version + 1) return reload();
      this.version = m.v;
    }
    switch (m.t || false) {
      case false:
        break;
      case 'resync':
        setTimeout(() => reload('lila-ws resync'), 500);
        break;
      case 'ack':
        this.ackable.onServerAck(m.d);
        break;
      default:
        // return true in a receive handler to prevent pubsub and events
        if (!(this.settings.receive && this.settings.receive(m.t, m.d))) {
          const sentAsEvent = this.settings.events[m.t] && this.settings.events[m.t](m.d || null, m);
          if (!sentAsEvent) {
            this.pubsub.emit('socket.in.' + m.t, m.d, m);
          }
        }
    }
  };

  debug = (msg: unknown, always = false) => {
    if (always || this.options.debug) console.debug(msg);
  };

  destroy = () => {
    clearTimeout(this.pingSchedule);
    clearTimeout(this.connectSchedule);
    this.disconnect();
    this.ws = undefined;
  };

  disconnect = () => {
    const ws = this.ws;
    if (ws) {
      this.debug('Disconnect');
      this.autoReconnect = false;
      ws.onerror = ws.onclose = ws.onopen = ws.onmessage = () => {};
      ws.close();
    }
  };

  onError = (e: unknown) => {
    this.options.debug = true;
    this.debug(`error: ${e} ${JSON.stringify(e)}`); // e not always from lila
  };

  onClose = (e: CloseEvent, url: string) => {
    this.pubsub.emit('socket.close');
    if (this.autoReconnect) {
      this.debug('Will autoreconnect in ' + this.options.autoReconnectDelay);
      this.scheduleConnect(this.options.autoReconnectDelay);
    }
    if (e.wasClean && e.code < 1002) return;

    if (isOnline()) site.log(`${sri ? 'sri ' + sri : ''} unclean close ${e.code} ${url} ${e.reason}`);
    this.tryOtherUrl = true;
    clearTimeout(this.pingSchedule);
  };

  onSuccess = () => {
    this.nbConnects++;
    if (this.nbConnects == 1) {
      StrongSocket.resolveFirstConnect(this.send);
      let disconnectTimeout: Timeout | undefined;
      idleTimer(
        10 * 60 * 1000,
        () => {
          this.options.idle = true;
          disconnectTimeout = setTimeout(this.destroy, 2 * 60 * 60 * 1000);
        },
        () => {
          this.options.idle = false;
          if (this.ws) clearTimeout(disconnectTimeout);
          else location.reload();
        },
      );
    }
  };

  baseUrl = () => {
    let url = this.storage.get();
    if (!url || !this.baseUrls.includes(url)) {
      url = this.baseUrls[Math.floor(Math.random() * this.baseUrls.length)];
      this.storage.set(url);
    } else if (this.tryOtherUrl) {
      const i = this.baseUrls.findIndex(u => u === url);
      url = this.baseUrls[(i + 1) % this.baseUrls.length];
      this.storage.set(url);
    }
    this.tryOtherUrl = false;
    return url;
  };

  pingInterval = () => this.computePingDelay() + this.averageLag;
  getVersion = () => this.version;
}

class Ackable {
  currentId = 1; // increment with each ackable message sent
  messages: MsgAck[] = [];
  private _sign: string;

  constructor(readonly send: Send) {
    setInterval(this.resend, 1200);
  }

  sign = (s: string) => (this._sign = s);

  resend = () => {
    const resendCutoff = performance.now() - 2500;
    this.messages.forEach(m => {
      if (m.at < resendCutoff) this.send(m.t, m.d, { sign: this._sign });
    });
  };

  register = (t: Tpe, d: Payload) => {
    d.a = this.currentId++;
    this.messages.push({
      t: t,
      d: d,
      at: performance.now(),
    });
  };

  onServerAck = (id: number) => {
    this.messages = this.messages.filter(m => m.d.a !== id);
  };
}
