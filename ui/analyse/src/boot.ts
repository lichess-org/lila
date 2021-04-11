import { AnalyseOpts } from './interfaces';
import { start } from './main';

export default function (cfg: AnalyseOpts) {
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
}
