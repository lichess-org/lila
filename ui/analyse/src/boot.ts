import { AnalyseApi, AnalyseOpts } from './interfaces';
import { start } from './main';

export default function(cfg: AnalyseOpts) {
  const li = window.lichess, data = cfg.data;
  let analyse: AnalyseApi;

  li.socket = new li.StrongSocket(
    data.url.socket,
    data.player.version, {
      params: {
        userTv: data.userTv && data.userTv.id
      },
      receive(t: string, d: any) {
        analyse.socketReceive(t, d);
      }
    });
  cfg.$side = $('.analyse__side').clone();
  cfg.$underboard = $('.analyse__underboard').clone();
  cfg.initialPly = 'url';
  cfg.socketSend = li.socket.send;
  analyse = start(cfg);
};
