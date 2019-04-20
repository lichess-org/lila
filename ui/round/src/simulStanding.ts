export interface SimulStanding {
  w: number; // wins
  d: number; // draws
  l: number; // losses
  g: number; // ongoing
  pct?: string; // winning percentage
  rw?: number; // wins required for target (10000=success, -10000=failed)
  rd?: number; // draws required for target
}

export function updateSimulStanding(id: string, s: SimulStanding, trans: Trans) {
  $('#simul_w_' + id).text(s.w.toString());
  $('#simul_d_' + id).text(s.d.toString());
  $('#simul_l_' + id).text(s.l.toString());
  $('#simul_g_' + id).text(s.g.toString());
  $('#simul_pct_' + id).text(s.pct ? s.pct : '');
  var req = '';
  if (s.rw === 10000) {
    req += '<span class="win">' + trans('succeeded') + '</span>';
  } else if (s.rw === -10000) {
    req += '<span class="loss">' + trans('failed') + '</span>';
  } else {
    if (s.rw) req += '<span class="win req">' + trans.plural('nbVictories', s.rw) + '</span>';
    if (s.rd) req += '<span class="draw req">' + trans.plural('nbDraws', s.rd) + '</span>';
  }
  $('#simul_req_' + id).html(req);
}