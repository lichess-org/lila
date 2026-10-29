import { Protocol } from '../protocol';
import {
  CevalState,
  type CevalEngine,
  type DirectEngineInfo,
  type EngineNotifier,
  type Work,
} from '../types';

export const directEngineUrl = 'http://127.0.0.1:48036';

export class DirectEngine implements CevalEngine {
  private protocol?: Protocol;
  private socket?: WebSocket;
  private failed = false;

  constructor(
    private readonly info: DirectEngineInfo,
    private readonly status: EngineNotifier,
  ) {}

  getInfo(): DirectEngineInfo {
    return this.info;
  }

  getState(): CevalState {
    return this.failed
      ? CevalState.Failed
      : !this.protocol
        ? CevalState.Initial
        : !this.protocol.engineName
          ? CevalState.Loading
          : this.protocol.isComputing()
            ? CevalState.Computing
            : CevalState.Idle;
  }

  start(work: Work): void {
    if (!this.socket) {
      this.failed = false;
      const protocol = (this.protocol = new Protocol(this.info));
      const url = new URL('/v1/uci', this.info.endpoint);
      url.protocol = 'ws:';
      const socket = (this.socket = new WebSocket(url));
      socket.onopen = () => {
        if (this.socket === socket) protocol.connected(command => socket.send(command));
      };
      socket.onmessage = event => {
        if (this.socket !== socket) return;
        protocol.received(event.data);
      };
      socket.onclose = event => {
        if (this.socket !== socket) return;
        this.socket = undefined;
        this.protocol = undefined;
        protocol.disconnected();
        this.failed = event.code !== 1000;
        this.status(this.failed ? { error: 'Direct engine disconnected' } : undefined);
      };
    }
    this.protocol!.compute(work);
  }

  stop(): void {
    this.protocol?.compute(undefined);
  }

  version(): string | undefined {
    return this.protocol?.engineName;
  }

  destroy(): void {
    this.stop();
    this.protocol?.disconnected();
    this.protocol = undefined;
    this.socket?.close();
    this.socket = undefined;
  }
}
