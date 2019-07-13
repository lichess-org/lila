export interface SimulStanding {
  id: string;
  w: number; // wins
  d: number; // draws
  l: number; // losses
  g: number; // ongoing
  r: number; // relative score required
  pct?: string; // winning percentage
  rw?: number; // wins required for target (10000=success, -10000=failed)
  rd?: number; // draws required for target
  fg?: string; // id of game that just finished
}

export function updateSimulStanding(s: SimulStanding, trans: Trans, draughtsResult: boolean) {
  $('#simul_w_' + s.id).text(s.w.toString());
  $('#simul_d_' + s.id).text(s.d.toString());
  $('#simul_l_' + s.id).text(s.l.toString());
  $('#simul_g_' + s.id).text(s.g.toString());
  $('.simul_pct_' + s.id).text(s.pct ? s.pct : '');
  const score = draughtsResult ? s.r * 2 : s.r;
  $('.simul_rel_' + s.id).text((score < 0 ? '' : '+') + Math.round(score * 10) / 10);
  var req = '';
  if (s.rw === 10000) {
    req += '<span class="win">' + trans('succeeded') + '</span>';
  } else if (s.rw === -10000) {
    req += '<span class="loss">' + trans('failed') + '</span>';
  } else {
    if (s.rw) req += '<span class="win req">' + trans.plural('nbVictories', s.rw) + '</span>';
    if (s.rd) req += '<span class="draw req">' + trans.plural('nbDraws', s.rd) + '</span>';
  }
  $('#simul_req_' + s.id).html(req);
  const curToMove = getToMove();
  if (s.fg && curToMove !== undefined) {
    if (curToMove > s.g)
      setToMove(curToMove - 1, trans);
  }
}

function getToMove(): number | undefined {
  const elm = $('.simul_tomove .tomove_count');
  if (elm && elm.text()) {
    const curToMove = Number(elm.text());
    if (!isNaN(curToMove))
      return curToMove;
  }
  return;
}

function setToMove(toMove: number, trans: Trans) {
  $('.simul_tomove .tomove_count').text(toMove);
  $('.simul_tomove span').text(trans.plural('nbGames', toMove));
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