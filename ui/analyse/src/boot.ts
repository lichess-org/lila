import { AnalyseApi, AnalyseOpts } from './interfaces';

export default function (start: (opts: AnalyseOpts) => AnalyseApi) {
  return function (cfg: AnalyseOpts) {
    const socketUrl = `/watch/${cfg.data.game.id}/${cfg.data.player.color}/v6`;
    site.socket = new site.StrongSocket(socketUrl, cfg.data.player.version, {
      params: {
        userTv: cfg.data.userTv && cfg.data.userTv.id,
      },
      receive(t: string, d: any) {
        analyse.socketReceive(t, d);
      },
    });
    cfg.$side = $('.analyse__side').clone();
    cfg.$underboard = $('.analyse__underboard').clone();
    cfg.socketSend = site.socket.send;
    const analyse = start(cfg);
  };
}
