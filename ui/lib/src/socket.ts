import * as xhr from './xhr';
import { idleTimer, browserTaskQueueMonitor } from './event';
import { storage, once, type LichessStorage } from './storage';
import { pubsub, type PubsubEvent } from './pubsub';
import { myUserId } from './common';
import { log } from './permalog';

let siteSocket: WsSocket | undefined;

export function eventuallySetupDefaultConnection(): void {
  setTimeout(() => {
    if (!siteSocket) wsConnect('/socket/v5', false);
  }, 500);
}

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

interface Options {
  idle: boolean;
  pongTimeout: number; // time to wait for pong before resetting the connection
  pingDelay: number; // time between pong and ping
  autoReconnectDelay: number;
  protocol: string;
  isAuth: boolean;
  debug?: boolean;
}
interface Params extends Record<string, any> {
  sri?: Sri;
  flag?: string;
}

interface Settings {
  receive?: (t: Tpe, d: Payload) => void;
  events: {
    [tpe: string]: (d: Payload | null, msg: MsgIn) => any;
  };
  params?: Partial<Params>;
  options?: Partial<Options>;
}

export function wsConnect(url: string, version: number | false, settings: Partial<Settings> = {}): WsSocket {
  return (siteSocket = new WsSocket(url, version, settings));
}

export function wsDestroy(): void {
  siteSocket?.destroy();
  siteSocket = undefined;
}

export function wsSend(t: string, d?: any, o?: any, noRetry?: boolean): void {
  siteSocket?.send(t, d, o, noRetry);
}

export function wsSign(s: string): void {
  siteSocket?.sign(s);
}

export function wsVersion(): number | false {
  return siteSocket?.getVersion() ?? false;
}

export function wsPingInterval(): number {
  return siteSocket?.pingInterval() ?? 0;
}

export function wsAverageLag(): number {
  return siteSocket?.averageLag ?? 0;
}

const isOnline = () => !('onLine' in navigator) || navigator.onLine;

class WsSocket {
  averageLag = 0;

  private settings: Settings;
  private options: Options;
  private version: number | false;
  private ws: WebSocket | undefined;
  private pingSchedule: Timeout;
  private connectSchedule: Timeout;
  private ackable: Ackable = new Ackable((t, d, o) => this.send(t, d, o));
  private lastPingTime: number = performance.now();
  private pongCount = 0;
  private tryOtherUrl = false;
  private storage: LichessStorage = storage.make('surl17', 30 * 60 * 1000);
  private _sign?: string;
  private resendWhenOpen: [string, any, any][] = [];
  private baseUrls = document.body.dataset.socketDomains!.split(',');

  private lastUrl?: string;
  private heartbeat = browserTaskQueueMonitor(1000);

  constructor(
    readonly url: string,
    version: number | false,
    settings: Partial<Settings> = {},
  ) {
    this.options = {
      idle: false,
      debug: false,
      pongTimeout: 9000,
      autoReconnectDelay: 3500,
      protocol: location.protocol === 'https:' ? 'wss:' : 'ws:',
      isAuth: !!myUserId(),
      ...(settings.options || {}),
      pingDelay: 2500,
    };
    this.settings = {
      receive: settings.receive,
      events: settings.events || {},
      params: {
        sri: site.sri,
        ...(settings.params || {}),
      },
    };
    this.version = version;
    pubsub.on('socket.send', this.send);
    this.connect();
  }

  sign = (s: string): void => {
    this._sign = s;
    this.ackable.sign(s);
  };

  private connect = (): void => {
    this.destroy();
    this.lastUrl = xhr.url(this.options.protocol + '//' + this.nextBaseUrl() + this.url, {
      ...this.settings.params,
      v: this.version === false ? undefined : this.version,
    });
    this.debug('connection attempt to ' + this.lastUrl);
    try {
      const ws = (this.ws = new WebSocket(this.lastUrl));
      ws.onerror = e => this.onError(e);
      ws.onclose = this.onClose;
      ws.onopen = () => {
        this.lastUrl = ws.url;
        this.debug('connected to ' + this.lastUrl);
        const cl = document.body.classList;
        if (pubsub.past('socket.hasConnected')) cl.add('reconnected');
        cl.remove('offline');
        cl.add('online');
        this.onSuccess();
        this.pingNow();
        this.resendWhenOpen.forEach(([t, d, o]) => this.send(t, d, o));
        this.resendWhenOpen = [];
        pubsub.emit('socket.open');
        this.ackable.resend();
      };
      ws.onmessage = e => {
        if (e.data == 0) return this.pong();
        const m = JSON.parse(e.data);
        if (m.t === 'n') this.pong();
        this.handle(m);
      };
    } catch (e) {
      this.onClose({ code: 4000, reason: String(e) } as CloseEvent);
    }
    this.scheduleConnect();
  };

  send = (t: string, d: any, o: any = {}, noRetry = false): void => {
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
    if (t === 'racerScore' && o.sign != this._sign) return;
    if (t === 'move' && o.sign != this._sign) {
      let stack: string;
      try {
        stack = new Error().stack!.split('\n').join(' / ').replace(/\s+/g, ' ');
      } catch (e: any) {
        stack = `${e.message} ${navigator.userAgent}`;
      }
      if (!stack.includes('round.nvui')) {
        setTimeout(() => {
          if (once(`socket.rep.${Math.round(Date.now() / 1000 / 3600 / 3)}`))
            this.send('rep', { n: `soc: ${message} ${stack}` });
          else wsDestroy();
        }, 10000);
      }
    }
    this.debug('send ' + message);
    if (!this.ws || this.ws.readyState === WebSocket.CONNECTING) {
      if (!noRetry) this.resendWhenOpen.push([t, msg.d, o]);
    } else this.ws.send(message);
  };

  private scheduleConnect = (delay: number = this.options.pongTimeout): void => {
    if (this.options.idle) delay = 10 * 1000 + Math.random() * 10 * 1000;
    clearTimeout(this.pingSchedule);
    clearTimeout(this.connectSchedule);
    this.connectSchedule = setTimeout(() => {
      document.body.classList.add('offline');
      document.body.classList.remove('online');
      if (isOnline()) $('#network-status').text(i18n?.site?.reconnecting ?? 'Reconnecting');
      else $('#network-status').text(i18n?.site?.noNetwork ?? 'Offline');
      this.tryOtherUrl = true;
      this.connect();
    }, delay);
  };

  private schedulePing = (delay: number): void => {
    clearTimeout(this.pingSchedule);
    this.pingSchedule = setTimeout(this.pingNow, delay);
  };

  private pingNow = (): void => {
    clearTimeout(this.pingSchedule);
    clearTimeout(this.connectSchedule);
    const pingData =
      this.options.isAuth && this.pongCount % 10 === 2
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
    this.scheduleConnect();
  };

  private computePingDelay = (): number => this.options.pingDelay + (this.options.idle ? 1000 : 0);

  private pong = (): void => {
    clearTimeout(this.connectSchedule);
    this.schedulePing(this.computePingDelay());
    const currentLag = Math.min(performance.now() - this.lastPingTime, 10000);
    this.pongCount++;

    // Average first 4 pings, then switch to decaying average.
    const mix = this.pongCount > 4 ? 0.1 : 1 / this.pongCount;
    this.averageLag += mix * (currentLag - this.averageLag);

    pubsub.emit('socket.lag', this.averageLag);
  };

  private handle = (m: MsgIn, retries: number = 10): void => {
    if (m.v && this.version !== false) {
      if (m.v <= this.version) {
        this.debug('already has event ' + m.v);
        return;
      }
      // it's impossible but according to previous logging, it happens nonetheless
      if (m.v > this.version + 1) {
        if (retries > 0) {
          console.debug('version gap, retrying', m.v, this.version, retries);
          setTimeout(() => this.handle(m, retries - 1), 200);
        } else {
          log(`${window.location.pathname}: version incoming ${m.v} vs current ${this.version}`);
          site.reload();
        }
        return;
      }
      this.version = m.v;
    }
    switch (m.t || false) {
      case false:
        break;
      case 'resync':
        setTimeout(() => site.reload('lila-ws resync'), 500);
        break;
      case 'ack':
        this.ackable.onServerAck(m.d);
        break;
      case 'batch':
        m.d.forEach(this.handle);
        break;
      default:
        // return true in a receive handler to prevent pubsub and events
        if (!(this.settings.receive && this.settings.receive(m.t, m.d))) {
          const sentAsEvent = this.settings.events[m.t] && this.settings.events[m.t](m.d || null, m);
          if (!sentAsEvent) pubsub.emit(('socket.in.' + m.t) as PubsubEvent, m.d, m);
        }
    }
  };

  private debug = (msg: unknown, always = false): void => {
    if (always || this.options.debug) console.debug(msg);
  };

  destroy = (): void => {
    clearTimeout(this.pingSchedule);
    clearTimeout(this.connectSchedule);
    this.disconnect();
    this.ws = undefined;
  };

  private disconnect = (): void => {
    const ws = this.ws;
    if (ws) {
      this.debug('Disconnect');
      ws.onerror = ws.onclose = ws.onopen = ws.onmessage = () => {};
      ws.close();
    }
  };

  private onError = (e: unknown): void => {
    if (this.heartbeat.wasSuspended) return;
    this.options.debug = true;
    this.debug(`error: ${e} ${JSON.stringify(e)}`); // e not always from lila
  };

  private onClose = (e: CloseEvent): void => {
    pubsub.emit('socket.close');

    if (this.heartbeat.wasSuspended) return this.onSuspended();

    if (this.ws) {
      this.debug('Will autoreconnect in ' + this.options.autoReconnectDelay);
      this.scheduleConnect(this.options.autoReconnectDelay);
    }
    if (e.wasClean && e.code < 1002) return;

    if (isOnline()) this.tryOtherUrl = true;
    clearTimeout(this.pingSchedule);
  };

  private onSuccess = (): void => {
    if (pubsub.past('socket.hasConnected')) return;

    pubsub.complete('socket.hasConnected');
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
  };

  private onSuspended() {
    this.heartbeat.reset(); // not a networking error, just get our connection back
    clearTimeout(this.pingSchedule);
    clearTimeout(this.connectSchedule);
    this.connect();
  }

  private nextBaseUrl = (): string => {
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

  pingInterval = (): number => this.computePingDelay() + this.averageLag;
  getVersion = (): number | false => this.version;
}

class Ackable {
  currentId = 1; // increment with each ackable message sent
  messages: MsgAck[] = [];
  private _sign: string;

  constructor(readonly send: (t: Tpe, d: Payload, o?: any) => void) {
    setInterval(this.resend, 1200);
  }

  sign = (s: string): string => (this._sign = s);

  resend = (): void => {
    const resendCutoff = performance.now() - 2500;
    this.messages.forEach(m => {
      if (m.at < resendCutoff) this.send(m.t, m.d, { sign: this._sign });
    });
  };

  register = (t: Tpe, d: Payload): void => {
    d.a = this.currentId++;
    this.messages.push({
      t: t,
      d: d,
      at: performance.now(),
    });
  };

  onServerAck = (id: number): void => {
    this.messages = this.messages.filter(m => m.d.a !== id);
  };
}
