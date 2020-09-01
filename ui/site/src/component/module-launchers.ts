import StrongSocket from './socket';
import { makeChat } from './functions';
import trans from './trans';

export default function moduleLaunchers() {
  const li: any = window.lichess;
  if (li.analyse) window.LichessAnalyse.boot(li.analyse);
  else if (li.userAnalysis) startUserAnalysis(li.userAnalysis);
  else if (li.study) startStudy(li.study);
  else if (li.practice) startPractice(li.practice);
  else if (li.relay) startRelay(li.relay);
  else if (li.puzzle) startPuzzle(li.puzzle);
  else if (li.tournament) startTournament(li.tournament);
  else if (li.team) startTeam(li.team);
}

function startTournament(cfg) {
  const element = document.querySelector('main.tour') as HTMLElement;
  $('body').data('tournament-id', cfg.data.id);
  let tournament;
  window.lichess.socket = StrongSocket(
    `/tournament/${cfg.data.id}/socket/v5`, cfg.data.socketVersion, {
      receive: (t, d) => tournament.socketReceive(t, d)
    });
  cfg.socketSend = window.lichess.socket.send;
  cfg.element = element;
  tournament = window.LichessTournament.start(cfg);
}

function startTeam(cfg) {
  window.lichess.socket = StrongSocket('/team/' + cfg.id, cfg.socketVersion);
  cfg.chat && makeChat(cfg.chat);
  $('#team-subscribe').on('change', function(this: HTMLInputElement) {
    const v = this.checked;
    $(this).parents('form').each(function(this: HTMLElement) {
      $.post($(this).attr('action'), { v });
    });
  });
}

function startUserAnalysis(cfg) {
  var analyse;
  cfg.initialPly = 'url';
  cfg.trans = trans(cfg.i18n);
  window.lichess.socket = StrongSocket('/analysis/socket/v5', false, {
    receive: (t, d) => analyse.socketReceive(t, d)
  });
  cfg.socketSend = window.lichess.socket.send;
  cfg.$side = $('.analyse__side').clone();
  analyse = window.LichessAnalyse.start(cfg);
}

function startStudy(cfg) {
  var analyse;
  cfg.initialPly = 'url';
  window.lichess.socket = StrongSocket(cfg.socketUrl, cfg.socketVersion, {
    receive: (t, d) => analyse.socketReceive(t, d)
  });
  cfg.socketSend = window.lichess.socket.send;
  cfg.trans = trans(cfg.i18n);
  analyse = window.LichessAnalyse.start(cfg);
}

function startPractice(cfg) {
  var analyse;
  cfg.trans = trans(cfg.i18n);
  window.lichess.socket = StrongSocket('/analysis/socket/v5', false, {
    receive: (t, d) => analyse.socketReceive(t, d)
  });
  cfg.socketSend = window.lichess.socket.send;
  analyse = window.LichessAnalyse.start(cfg);
}

function startRelay(cfg) {
  var analyse;
  cfg.initialPly = 'url';
  window.lichess.socket = StrongSocket(cfg.socketUrl, cfg.socketVersion, {
    receive: (t, d) => analyse.socketReceive(t, d)
  });
  cfg.socketSend = window.lichess.socket.send;
  cfg.trans = trans(cfg.i18n);
  analyse = window.LichessAnalyse.start(cfg);
}

function startPuzzle(cfg) {
  cfg.element = document.querySelector('main.puzzle');
  window.LichessPuzzle(cfg);
}
