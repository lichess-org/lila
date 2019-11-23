import TournamentController from './ctrl';

export interface TournamentSocket {
  send: SocketSend;
  receive(type: string, data: any): void;
}

export default function(send: SocketSend, ctrl: TournamentController) {

  const handlers = {
    reload: ctrl.askReload,
    redirect(fullId) {
      ctrl.redirectFirst(fullId.slice(0, 8), true);
      return true;
    }
  };

  return {
    send,
    receive(type: string, data: any) {
      if (handlers[type]) return handlers[type](data);
      return false;
    }
  };
};
