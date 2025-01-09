import SimulCtrl from './ctrl';
import { SimulOpts } from './interfaces';

export function boot(opts: SimulOpts, start: (opts: SimulOpts) => SimulCtrl): SimulCtrl {
  $('body').data('simul-id', opts.data.id);

  let ctrl: SimulCtrl;
  window.lishogi.socket = new window.lishogi.StrongSocket(
    '/simul/' + opts.data.id + '/socket/v4',
    opts.socketVersion,
    {
      receive: function (t, d) {
        ctrl.socket.receive(t, d);
      },
    }
  );
  opts.socketSend = window.lishogi.socket.send;
  opts.$side = $('.simul__side').clone();

  ctrl = start(opts);

  return ctrl;
}
