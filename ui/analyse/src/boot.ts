import { AnalyseApi, AnalyseOpts } from './interfaces';

export default function (start: (opts: AnalyseOpts) => AnalyseApi) {
  function startUserAnalysis(cfg: any) {
    cfg.$side = $('.analyse__side').clone();
    startAnalyse(cfg);
  }

  function startAnalyse(cfg: any) {
    lichess.socket = new lichess.StrongSocket(cfg.socketUrl || '/analysis/socket/v5', cfg.socketVersion, {
      receive: (t: string, d: any) => analyse.socketReceive(t, d),
    });
    cfg.socketSend = lichess.socket.send;
    const analyse = start(cfg);
  }

  lichess.load.then(() => {
    const li: any = lichess;
    if (li.userAnalysis) startUserAnalysis(li.userAnalysis);
    else if (li.study || li.practice || li.relay) startAnalyse(li.study || li.practice || li.relay);
  });

  return function (cfg: AnalyseOpts) {
    lichess.socket = new lichess.StrongSocket(cfg.data.url.socket, cfg.data.player.version, {
      params: {
        userTv: cfg.data.userTv && cfg.data.userTv.id,
      },
      receive(t: string, d: any) {
        analyse.socketReceive(t, d);
      },
    });
    cfg.$side = $('.analyse__side').clone();
    cfg.$underboard = $('.analyse__underboard').clone();
    cfg.socketSend = lichess.socket.send;
    const analyse = start(cfg);
  };
}
