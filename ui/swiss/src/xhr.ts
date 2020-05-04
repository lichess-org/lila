import throttle from 'common/throttle';
import { json, form } from 'common/xhr';
import SwissCtrl from './ctrl';
import { SwissData } from './interfaces';

const headers = {
  'Accept': 'application/vnd.lichess.v5+json'
};

// when the tournament no longer exists
function onFail(err) {
  throw err;
  // window.lichess.reload();
}

const join = (ctrl: SwissCtrl) =>
  json(`/swiss/${ctrl.data.id}/join`, { method: 'post' }).catch(onFail);

const loadPage = (ctrl: SwissCtrl, p: number) =>
  thenReloadOrFail(ctrl,
    json(`/swiss/${ctrl.data.id}/standing/${p}`)
  );

const loadPageOf = (ctrl: SwissCtrl, userId: string): Promise<any> =>
  json(`/swiss/${ctrl.data.id}/page-of/${userId}`);

const reload = (ctrl: SwissCtrl) =>
  thenReloadOrFail(ctrl,
    json(`/swiss/${ctrl.data.id}?page=${ctrl.focusOnMe ? 0 : ctrl.page}`)
  );

const thenReloadOrFail = (ctrl: SwissCtrl, res: Promise<SwissData>) =>
  res.then(data => {
    ctrl.reload(data);
    ctrl.redraw();
  })
  .catch(onFail);

// function playerInfo(ctrl: SwissCtrl, userId: string) {
//   return $.ajax({
//     url: ['/swiss', ctrl.data.id, 'player', userId].join('/'),
//     headers
//   }).then(data => {
//     ctrl.setPlayerInfoData(data);
//     ctrl.redraw();
//   }, onFail);
// }

export default {
  join: throttle(1000, join),
  loadPage: throttle(1000, loadPage),
  loadPageOf,
  reloadSoon: throttle(4000, reload),
  reloadNow: reload,
  // playerInfo
};
