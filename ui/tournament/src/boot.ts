import TournamentController from './ctrl';
import { TournamentOpts } from './interfaces';

export function boot(
  opts: TournamentOpts,
  start: (opts: TournamentOpts) => TournamentController
): TournamentController {
  $('body').data('tournament-id', opts.data.id);
  let ctrl;
  window.lishogi.socket = new window.lishogi.StrongSocket(
    '/tournament/' + opts.data.id + '/socket/v4',
    opts.data.socketVersion,
    {
      receive: (t, d) => ctrl.socket.receive(t, d),
    }
  );
  opts.socketSend = window.lishogi.socket.send;
  ctrl = start(opts);

  return ctrl;
}
