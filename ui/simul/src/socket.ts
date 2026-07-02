import type SimulCtrl from './ctrl';
import type { SimulData } from './interfaces';

type SimulMessage =
  | { tpe: 'reload'; data: SimulData }
  | { tpe: 'aborted' }
  | { tpe: 'hostGame'; data: string };

export type SimulTpe = SimulMessage['tpe'];
export type SimulSocket = {
  send: SocketSend;
  receive(message: SimulMessage): void | false;
};

export function makeSocket(send: SocketSend, ctrl: SimulCtrl): SimulSocket {
  return {
    send,
    receive(message) {
      switch (message.tpe) {
        case 'reload':
          ctrl.reload(message.data);
          ctrl.redraw();
          return;
        case 'aborted':
          site.reload();
          return;
        case 'hostGame':
          ctrl.data.host.gameId = message.data;
          ctrl.redraw();
          return;
        default:
          return false;
      }
    },
  };
}
