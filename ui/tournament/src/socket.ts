import TournamentController from './ctrl';

export interface TournamentSocket {
  send: SocketSend;
  receive(type: string, data: any): void;
}

export function makeSocket(send: SocketSend, ctrl: TournamentController) {
  const handlers = {
    reload: ctrl.askReload,
    redirect(fullId: string) {
      ctrl.redirectFirst(fullId.slice(0, 8), true);
      return true; // prevent default redirect
    },
  };

  return {
    send,
    receive(type: string, data: any) {
      const handler = (handlers as SocketHandlers)[type];
      if (handler) return handler(data);
      return false;
    },
  };
}
