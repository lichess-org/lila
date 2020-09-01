export default function moduleLaunchers() {
  const li: any = window.lichess;
  if (li.analyse) window.LichessAnalyse.boot(li.analyse);
  else if (li.user_analysis) startUserAnalysis(li.user_analysis);
  else if (li.study) startStudy(li.study);
  else if (li.practice) startPractice(li.practice);
  else if (li.relay) startRelay(li.relay);
  else if (li.puzzle) startPuzzle(li.puzzle);
  else if (li.tournament) startTournament(li.tournament);
  else if (li.team) startTeam(li.team);
}

function startTournament(cfg) {
  var element = document.querySelector('main.tour');
  $('body').data('tournament-id', cfg.data.id);
  let tournament;
  lichess.socket = lichess.StrongSocket(
    '/tournament/' + cfg.data.id + '/socket/v5', cfg.data.socketVersion, {
      receive: (t, d) => tournament.socketReceive(t, d)
    });
  cfg.socketSend = lichess.socket.send;
  cfg.element = element;
  tournament = LichessTournament.start(cfg);
}

function startSimul(cfg) {
  cfg.element = document.querySelector('main.simul');
  $('body').data('simul-id', cfg.data.id);
  var simul;
  lichess.socket = lichess.StrongSocket(
    '/simul/' + cfg.data.id + '/socket/v5', cfg.socketVersion, {
      receive: function(t, d) {
        simul.socketReceive(t, d);
      }
    });
  cfg.socketSend = lichess.socket.send;
  cfg.$side = $('.simul__side').clone();
  simul = LichessSimul(cfg);
}

function startTeam(cfg) {
  lichess.socket = lichess.StrongSocket('/team/' + cfg.id, cfg.socketVersion);
  cfg.chat && lichess.makeChat(cfg.chat);
  $('#team-subscribe').on('change', function() {
    const v = this.checked;
    $(this).parents('form').each(function() {
      $.post($(this).attr('action'), {
        v: v
      });
    });
  });
}

function startUserAnalysis(cfg) {
  var analyse;
  cfg.initialPly = 'url';
  cfg.trans = lichess.trans(cfg.i18n);
  lichess.socket = lichess.StrongSocket('/analysis/socket/v5', false, {
    receive: function(t, d) {
      analyse.socketReceive(t, d);
    }
  });
  cfg.socketSend = lichess.socket.send;
  cfg.$side = $('.analyse__side').clone();
  analyse = LichessAnalyse.start(cfg);
}

function startStudy(cfg) {
  var analyse;
  cfg.initialPly = 'url';
  lichess.socket = lichess.StrongSocket(cfg.socketUrl, cfg.socketVersion, {
    receive: function(t, d) {
      analyse.socketReceive(t, d);
    }
  });
  cfg.socketSend = lichess.socket.send;
  cfg.trans = lichess.trans(cfg.i18n);
  analyse = LichessAnalyse.start(cfg);
}

function startPractice(cfg) {
  var analyse;
  cfg.trans = lichess.trans(cfg.i18n);
  lichess.socket = lichess.StrongSocket('/analysis/socket/v5', false, {
    receive: function(t, d) {
      analyse.socketReceive(t, d);
    }
  });
  cfg.socketSend = lichess.socket.send;
  analyse = LichessAnalyse.start(cfg);
}

function startRelay(cfg) {
  var analyse;
  cfg.initialPly = 'url';
  lichess.socket = lichess.StrongSocket(cfg.socketUrl, cfg.socketVersion, {
    receive: function(t, d) {
      analyse.socketReceive(t, d);
    }
  });
  cfg.socketSend = lichess.socket.send;
  cfg.trans = lichess.trans(cfg.i18n);
  analyse = LichessAnalyse.start(cfg);
}

function startPuzzle(cfg) {
  cfg.element = document.querySelector('main.puzzle');
  LichessPuzzle(cfg);
}
