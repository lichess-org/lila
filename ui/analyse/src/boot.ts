import type AnalyseCtrl from './ctrl';
import type { AnalyseOpts } from './interfaces';

export function replay(opts: AnalyseOpts, start: (opts: AnalyseOpts) => AnalyseCtrl): AnalyseCtrl {
  let ctrl: AnalyseCtrl | undefined = undefined;

  const data = opts.data;
  const socketUrl = `/watch/${data.game.id}/${data.player.color}/v6`;
  window.lishogi.socket = new window.lishogi.StrongSocket(socketUrl, data.player.version, {
    options: {
      name: 'analyse',
    },
    params: {
      userTv: data.userTv?.id,
    },
    receive: (t, d) => {
      ctrl?.socket.receive(t, d);
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
  let ctrl: AnalyseCtrl | undefined = undefined;

  window.lishogi.socket = new window.lishogi.StrongSocket(opts.socketUrl, opts.socketVersion, {
    receive: (t, d) => {
      ctrl?.socket.receive(t, d);
    },
  });
  opts.initialPly = 'url';
  opts.socketSend = window.lishogi.socket.send;
  ctrl = start(opts);
  return ctrl;
}

export function analysis(
  opts: AnalyseOpts,
  start: (opts: AnalyseOpts) => AnalyseCtrl,
): AnalyseCtrl {
  let ctrl: AnalyseCtrl | undefined = undefined;

  window.lishogi.socket = new window.lishogi.StrongSocket('/analysis/socket/v4', false, {
    receive: (t, d) => {
      ctrl?.socket.receive(t, d);
    },
  });
  opts.initialPly = 'url';
  opts.$side = $('.analyse__side').clone();
  opts.socketSend = window.lishogi.socket.send;

  ctrl = start(opts);

  return ctrl;
}

export function practice(
  opts: AnalyseOpts,
  start: (opts: AnalyseOpts) => AnalyseCtrl,
): AnalyseCtrl {
  let ctrl: AnalyseCtrl | undefined = undefined;
  window.lishogi.socket = new window.lishogi.StrongSocket('/analysis/socket/v4', false, {
    receive: (t, d) => {
      ctrl?.socket.receive(t, d);
    },
  });
  opts.socketSend = window.lishogi.socket.send;
  ctrl = start(opts);

  return ctrl;
}
