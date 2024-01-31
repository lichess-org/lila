import LobbyController from './ctrl';
import * as hookRepo from './hookRepo';
import { Hook } from './interfaces';
import * as xhr from './xhr';

interface Handlers {
  [key: string]: (data: any) => void;
}

const li = window.lishogi;

export default class LobbySocket {
  handlers: Handlers;

  constructor(
    readonly send: SocketSend,
    ctrl: LobbyController
  ) {
    this.send = send;

    this.handlers = {
      had(hook: Hook) {
        hookRepo.add(ctrl, hook);
        if (hook.action === 'cancel') ctrl.flushHooks(true);
        ctrl.redraw();
      },
      hrm(ids: string) {
        ids.match(/.{8}/g)!.forEach(function (id) {
          hookRepo.remove(ctrl, id);
        });
        ctrl.redraw();
      },
      hooks(hooks: Hook[]) {
        hookRepo.setAll(ctrl, hooks);
        ctrl.flushHooks(true);
        ctrl.redraw();
      },
      hli(ids: string) {
        hookRepo.syncIds(ctrl, ids.match(/.{8}/g) || []);
        ctrl.redraw();
      },
      reload_seeks() {
        if (ctrl.tab === 'seeks') xhr.seeks().then(ctrl.setSeeks);
      },
    };

    li.idleTimer(
      3 * 60 * 1000,
      () => send('idle', true),
      () => {
        send('idle', false);
        ctrl.awake();
      }
    );
  }

  realTimeIn() {
    this.send('hookIn');
  }
  realTimeOut() {
    this.send('hookOut');
  }

  receive = (type: string, data: any): boolean => {
    if (this.handlers[type]) {
      this.handlers[type](data);
      return true;
    }
    return false;
  };
}
