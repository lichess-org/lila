import type SimulCtrl from './ctrl';
import type { SimulData } from './interfaces';

export interface SimulSocket {
  send: SocketSend;
  receive(tpe: string, data: any): void;
}

export function makeSocket(send: SocketSend, ctrl: SimulCtrl) {
  const handlers: any = {
    reload(data: SimulData) {
      ctrl.reload(data);
      ctrl.redraw();
    },
    aborted: site.reload,
    hostGame(gameId: string) {
      ctrl.data.host.gameId = gameId;
      ctrl.redraw();
    },
  };

  return {
    send,
    receive(tpe: string, data: any) {
      if (handlers[tpe]) return handlers[tpe](data);
      return false;
    },
  };
}
