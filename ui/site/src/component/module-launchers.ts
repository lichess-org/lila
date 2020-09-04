import StrongSocket from './socket';
import makeChat from './chat';

export default function moduleLaunchers() {
  const li: any = window.lichess;
  if (li.userAnalysis) startUserAnalysis(li.userAnalysis);
  else if (li.study) startAnalyse(li.study);
  else if (li.practice) startAnalyse(li.practice);
  else if (li.relay) startAnalyse(li.relay);
  else if (li.team) startTeam(li.team);
}

function startTeam(cfg) {
  window.lichess.socket = new StrongSocket('/team/' + cfg.id, cfg.socketVersion);
  cfg.chat && makeChat(cfg.chat);
  $('#team-subscribe').on('change', function(this: HTMLInputElement) {
    const v = this.checked;
    $(this).parents('form').each(function(this: HTMLElement) {
      $.post($(this).attr('action'), { v });
    });
  });
}

function startUserAnalysis(cfg) {
  cfg.$side = $('.analyse__side').clone();
  startAnalyse(cfg);
}

function startAnalyse(cfg) {
  let analyse;
  window.lichess.socket = new StrongSocket(cfg.socketUrl || '/analysis/socket/v5', cfg.socketVersion, {
    receive: (t: string, d: any) => analyse.socketReceive(t, d)
  });
  analyse = window.LichessAnalyse.start(cfg);
}
