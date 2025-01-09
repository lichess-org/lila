import { AnalyseOpts } from './interfaces';
import AnalyseCtrl from './ctrl';

export function replay(opts: AnalyseOpts, start: (opts: AnalyseOpts) => AnalyseCtrl): AnalyseCtrl {
  let ctrl: AnalyseCtrl;

  const data = opts.data,
    socketUrl = `/watch/${data.game.id}/${data.player.color}/v6`;
  window.lishogi.socket = new window.lishogi.StrongSocket(socketUrl, data.player.version, {
    options: {
      name: 'analyse',
    },
    params: {
      userTv: data.userTv && data.userTv.id,
    },
    receive: function (t, d) {
      ctrl.socket.receive(t, d);
    },
    events: {},
  });
  opts.$side = $('.analyse__side').clone();
  opts.$underboard = $('.analyse__underboard').clone();
  opts.initialPly = 'url';
  opts.socketSend = window.lishogi.socket.send;
  ctrl = start(opts);

  return ctrl;
}

export function study(opts: AnalyseOpts, start: (opts: AnalyseOpts) => AnalyseCtrl): AnalyseCtrl {
  let ctrl: AnalyseCtrl;
  opts.initialPly = 'url';
  window.lishogi.socket = new window.lishogi.StrongSocket(opts.socketUrl, opts.socketVersion, {
    receive: function (t, d) {
      ctrl.socket.receive(t, d);
    },
  });
  opts.socketSend = window.lishogi.socket.send;
  ctrl = start(opts);

  return ctrl;
}

export function analysis(
  opts: AnalyseOpts,
  start: (opts: AnalyseOpts) => AnalyseCtrl
): AnalyseCtrl {
  let ctrl: AnalyseCtrl;
  opts.initialPly = 'url';
  window.lishogi.socket = new window.lishogi.StrongSocket('/analysis/socket/v4', false, {
    receive: function (t, d) {
      ctrl.socket.receive(t, d);
    },
  });
  opts.socketSend = window.lishogi.socket.send;
  opts.$side = $('.analyse__side').clone();
  ctrl = start(opts);

  return ctrl;
}

export function practice(
  opts: AnalyseOpts,
  start: (opts: AnalyseOpts) => AnalyseCtrl
): AnalyseCtrl {
  let ctrl: AnalyseCtrl;
  window.lishogi.socket = new window.lishogi.StrongSocket('/analysis/socket/v4', false, {
    receive: function (t, d) {
      ctrl.socket.receive(t, d);
    },
  });
  opts.socketSend = window.lishogi.socket.send;
  ctrl = start(opts);

  return ctrl;
}
