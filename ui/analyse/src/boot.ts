import type { AnalyseApi, AnalyseOpts } from './interfaces';
import { wsConnect } from 'lib/socket';
import type { AnalyseSocketSend } from './socket';

export default function (start: (opts: AnalyseOpts) => AnalyseApi) {
  return function (cfg: AnalyseOpts) {
    const socketUrl = `/watch/${cfg.data.game.id}/${cfg.data.player.color}/v6`;
    cfg.$side = $('.analyse__side').clone();
    cfg.$underboard = $('.analyse__underboard').clone();
    cfg.socketSend = wsConnect(socketUrl, cfg.data.player.version, {
      params: {
        userTv: cfg.data.userTv && cfg.data.userTv.id,
      },
      receive(t: string, d: any) {
        analyse.socketReceive(t, d);
      },
    }).send as AnalyseSocketSend;
    const analyse = start(cfg);
  };
}
