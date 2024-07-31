import { Protocol } from '../protocol';
import { Work, CevalState, CevalEngine, SystemEngineInfo, EngineNotifier } from '../types';

export class SystemEngine implements CevalEngine {
  private failed = false;
  private protocol = new Protocol();
  private socket: WebSocket | undefined;
  url: string;

  constructor(
    private info: SystemEngineInfo,
    private status?: EngineNotifier | undefined,
  ) {}

  getInfo(): SystemEngineInfo {
    return this.info;
  }

  getState(): CevalState {
    return !this.socket
      ? CevalState.Initial
      : this.failed
      ? CevalState.Failed
      : !this.protocol.engineName
      ? CevalState.Loading
      : this.protocol.isComputing()
      ? CevalState.Computing
      : CevalState.Idle;
  }

  start(work: Work): void {
    this.protocol.compute(work);

    if (!this.socket) {
      this.socket = new WebSocket('ws://localhost:8017');
      this.socket.addEventListener('message', e => this.protocol.received(e.data));
      this.socket.addEventListener('error', () => {
        this.failed = true;
        this.status?.({ error: 'WebSocket error.' });
      });
      this.socket.addEventListener('close', () => {
        this.failed = true;
        this.status?.({ error: 'Engine crashed or unavailable.' });
      });
      this.socket.addEventListener('open', () => {
        this.socket?.send(JSON.stringify(this.info.cmd));
        this.protocol.connected(cmd => this.socket?.send(cmd));
      });
    }
  }

  stop(): void {
    this.protocol.compute(undefined);
  }

  engineName(): string | undefined {
    return this.protocol.engineName;
  }

  destroy(): void {
    this.socket?.close();
    this.socket = undefined;
  }
}
