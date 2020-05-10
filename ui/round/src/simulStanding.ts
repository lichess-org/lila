export interface SimulStanding {
  id: string;
  w: number; // wins
  d: number; // draws
  l: number; // losses
  g: number; // ongoing
  r: number; // relative score required
  pct?: string; // winning percentage
  tpct?: string; // target winning percentage
  rw?: number; // wins required for target (10000=success, -10000=failed)
  rd?: number; // draws required for target
  fg?: string; // id of game that just finished
}

export function updateSimulStanding(s: SimulStanding, trans: Trans, draughtsResult: boolean) {
  const finished = s.w + s.d + s.l,
    score = draughtsResult ? s.r * 2 : s.r,
    $sideStats = $('.game__simul__infos .simul-stats'),
    $ongoing = $('.game__simul-link .simul-ongoing');

  // update stats in sidepanel
  if ($sideStats) {
    let statsHtml = s.tpct ? trans('targetWinningPercentage', s.tpct + '%') + '<br>' : '';
    statsHtml += trans('currentWinningPercentage', finished && s.pct ? s.pct : '-');
    if (s.tpct) {
      statsHtml += '<br>' + trans('relativeScoreRequired', (score < 0 ? '' : '+') + Math.round(score * 10) / 10);
    }
    $sideStats.html(statsHtml);
  }

  if (!s.g) {
    //the  simul is finished
    if ($ongoing) $ongoing.remove();
    $('.simul-tomove').remove();
  } else {
    if ($ongoing) {
      $ongoing.text(trans.plural('nbGamesOngoing', s.g));
    }
    if (s.g === 1) {
      // only one game left,
      $('.simul-tomove').remove();
    }
  }

  // a game is finished
  if (s.fg) {
    // remove game
    $('#others_' + s.fg).remove();
    // remove timeout overview if this was last game in timeout
    if (!$('.round__now-playing .now-playing > a.game-timeout').length) {
      $('.round__now-playing .simul-timeouts').hide();
    }
    // lower amount of games to move
    const curToMove = getToMove();
    if (curToMove) {
      setToMove(curToMove - 1, trans);
    }
  }

  // update simul standings
  $('.round__now-playing .simul-standings span.win').text(s.w + ' W');
  $('.round__now-playing .simul-standings span.draw').text(s.d + ' D');
  $('.round__now-playing .simul-standings span.loss').text(s.l + ' L');
  $('.round__now-playing .simul-standings span.ongoing').text(trans('ongoing', s.g));

  // update distance to target
  const $targets = $('.round__now-playing .simul-targets');
  if ($targets) {
    var req = '';
    if (s.rw === 10000) {
      req += '<span class="win">' + trans('succeeded') + '</span>';
    } else if (s.rw === -10000) {
      req += '<span class="loss">' + trans('failed') + '</span>';
    } else {
      if (s.rw) req += '<span class="win">' + trans.plural('nbVictories', s.rw) + '</span>';
      if (s.rd) req += '<span class="draw">' + trans.plural('nbDraws', s.rd) + '</span>';
    }
    $targets.html(req);
  }
}

function getToMove(): number | undefined {
  const elm = $('.simul-tomove .tomove-count');
  if (elm && elm.text()) {
    const curToMove = Number(elm.text());
    if (!isNaN(curToMove))
      return curToMove;
  }
  return;
}

function setToMove(toMove: number, trans: Trans) {
  $('.simul-tomove .tomove-count').text(toMove);
  $('.simul-tomove .simul-tomove-count').text(trans.plural('nbGames', toMove));
}

export function incSimulToMove(trans: Trans) {
  const curToMove = getToMove();
  if (curToMove !== undefined)
    setToMove(curToMove + 1, trans);
}

export function decSimulToMove(trans: Trans) {
  const curToMove = getToMove();
  if (curToMove && curToMove > 0)
    setToMove(curToMove - 1, trans);
}