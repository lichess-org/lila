import { AnalyseApi, AnalyseOpts } from './interfaces';

export default function (start: (opts: AnalyseOpts) => AnalyseApi) {
  return function (cfg: AnalyseOpts) {
    const li = window.lishogi,
      data = cfg.data;
    let analyse: AnalyseApi;

    li.socket = li.StrongSocket(data.url.socket, data.player.version, {
      options: {
        name: 'analyse',
      },
      params: {
        userTv: data.userTv && data.userTv.id,
      },
      receive: function (t, d) {
        analyse.socketReceive(t, d);
      },
      events: {},
    });
    cfg.$side = $('.analyse__side').clone();
    cfg.$underboard = $('.analyse__underboard').clone();
    cfg.trans = li.trans(cfg.i18n);
    cfg.initialPly = 'url';
    cfg.socketSend = li.socket.send;
    analyse = start(cfg);
  };
}
