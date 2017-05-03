import { Ctrl, DasherOpts, DasherData, Redraw } from './interfaces'

import { ctrl as pingCtrl } from './ping'

export default function(opts: DasherOpts, redraw: Redraw): Ctrl {

  let initiating = true;
  let data: DasherData | undefined;
  let trans: Trans = window.lichess.trans({});

  const ping = pingCtrl(() => trans, redraw);

  function update(d: DasherData) {
    data = d;
    initiating = false;
    if (d.i18n) trans = window.lichess.trans(d.i18n);
    redraw();
  }

  $.ajax({
    url: '/dasher',
    headers: { 'Accept': 'application/vnd.lichess.v2+json' }
  }).then(update);

  return {
    initiating: () => initiating,
    data: () => data,
    trans: () => trans,
    ping,
    opts,
  };
};
