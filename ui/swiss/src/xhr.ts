import throttle from 'common/throttle';
import { json, form } from 'common/xhr';
import SwissCtrl from './ctrl';
import { SwissData } from './interfaces';

// when the tournament no longer exists
function onFail(err) {
  throw err;
  // window.lichess.reload();
}

const join = (ctrl: SwissCtrl) =>
  json(`/swiss/${ctrl.data.id}/join`, { method: 'post' }).catch(onFail);

const loadPage = (ctrl: SwissCtrl, p: number) =>
  json(`/swiss/${ctrl.data.id}/standing/${p}`).then(data => {
    ctrl.loadPage(data);
    ctrl.redraw();
  });

const loadPageOf = (ctrl: SwissCtrl, userId: string): Promise<any> =>
  json(`/swiss/${ctrl.data.id}/page-of/${userId}`);

const reload = (ctrl: SwissCtrl) =>
  json(`/swiss/${ctrl.data.id}?page=${ctrl.focusOnMe ? 0 : ctrl.page}`).then(data => {
    ctrl.reload(data);
    ctrl.redraw();
  }).catch(onFail);

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
