import * as xhr from './xhr';
import * as hookRepo from './hookRepo';
import LobbyController from './ctrl';
import StrongSocket from 'common/socket';
import { pubsub } from 'common/pubsub';
import { PoolMember, Hook } from './interfaces';

export class LobbySocket {
  send = site.socket.send;

  realTimeIn() {
    this.send('hookIn');
  }

  realTimeOut() {
    this.send('hookOut');
  }

  poolIn(member: PoolMember) {
    // last arg=true: must not retry
    // because if poolIn is sent before socket opens,
    // then poolOut is sent,
    // then poolIn shouldn't be sent again after socket opens.
    // poolIn is sent anyway on socket open event.
    this.send('poolIn', member, {}, true);
  }

  poolOut(member: PoolMember) {
    this.send('poolOut', member.id);
  }
}

export function makeSocket(ctrl: LobbyController): LobbySocket {
  const handlers: Record<string, (d: any) => boolean> = {
    had(hook: Hook) {
      hookRepo.add(ctrl, hook);
      if (hook.action === 'cancel') ctrl.flushHooks(true);
      ctrl.redraw();
      return true;
    },
    hrm(ids: string) {
      ids.match(/.{8}/g)!.forEach(function (id) {
        hookRepo.remove(ctrl, id);
      });
      ctrl.redraw();
      return true;
    },
    hooks(hooks: Hook[]) {
      hookRepo.setAll(ctrl, hooks);
      ctrl.flushHooks(true);
      ctrl.redraw();
      return true;
    },
    hli(ids: string) {
      hookRepo.syncIds(ctrl, ids.match(/.{8}/g) || []);
      ctrl.redraw();
      return true;
    },
    reload_seeks() {
      if (ctrl.tab.active === 'correspondence') xhr.seeks().then(ctrl.setSeeks);
      return true;
    },
  };
  site.socket = new StrongSocket('/lobby/socket/v5', false, {
    receive: (t: string, d: any) => handlers[t]?.(d) ?? false,
    events: {
      n(_: string, msg: any) {
        ctrl.spreadPlayersNumber && ctrl.spreadPlayersNumber(msg.d);
        setTimeout(
          () => ctrl.spreadGamesNumber && ctrl.spreadGamesNumber(msg.r),
          site.socket.pingInterval() / 2,
        );
      },
      reload_timeline() {
        xhr.text('/timeline').then(html => {
          $('.timeline').html(html);
          pubsub.emit('content-loaded');
        });
      },
      featured(o: { html: string }) {
        $('.lobby__tv').html(o.html);
        pubsub.emit('content-loaded');
      },
      redirect(e: RedirectTo) {
        ctrl.setRedirecting();
        ctrl.leavePool();
        site.redirect(e, true);
        return true;
      },
      fen(e: any) {
        ctrl.gameActivity(e.id);
      },
    },
  });

  return new LobbySocket();
}
