lichess.moduleLaunchers = () => {
  if (lichess.analyse) LichessAnalyse.boot(lichess.analyse);
  else if (lichess.user_analysis) startUserAnalysis(lichess.user_analysis);
  else if (lichess.study) startStudy(lichess.study);
  else if (lichess.practice) startPractice(lichess.practice);
  else if (lichess.relay) startRelay(lichess.relay);
  else if (lichess.puzzle) startPuzzle(lichess.puzzle);
  else if (lichess.tournament) startTournament(lichess.tournament);
  else if (lichess.team) startTeam(lichess.team);
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
