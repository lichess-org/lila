import StrongSocket from './socket';

const li: any = lichess;

export default function moduleLaunchers() {
  if (li.userAnalysis) startUserAnalysis(li.userAnalysis);
  else if (li.study || li.practice || li.relay) startAnalyse(li.study || li.practice || li.relay);
}

function startUserAnalysis(cfg) {
  cfg.$side = $('.analyse__side').clone();
  startAnalyse(cfg);
}

function startAnalyse(cfg) {
  lichess.socket = new StrongSocket(cfg.socketUrl || '/analysis/socket/v5', cfg.socketVersion, {
    receive: (t: string, d: any) => analyse.socketReceive(t, d),
  });
  cfg.socketSend = li.socket.send;
  const analyse = window.LichessAnalyse.start(cfg);
}
