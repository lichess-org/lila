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

const withdraw = (ctrl: SwissCtrl) =>
  json(`/swiss/${ctrl.data.id}/withdraw`, { method: 'post' }).catch(onFail);

const loadPage = (ctrl: SwissCtrl, p: number) =>
  json(`/swiss/${ctrl.data.id}/standing/${p}`).then(data => {
    ctrl.loadPage(data);
    ctrl.redraw();
  });

const loadPageOf = (ctrl: SwissCtrl, userId: string): Promise<any> =>
  json(`/swiss/${ctrl.data.id}/page-of/${userId}`);

const reload = (ctrl: SwissCtrl) =>
  json(`/swiss/${ctrl.data.id}?page=${ctrl.focusOnMe ? 0 : ctrl.page}&playerInfo=${ctrl.playerInfoId}`).then(data => {
    ctrl.reload(data);
    ctrl.redraw();
  }).catch(onFail);

const playerInfo = (ctrl: SwissCtrl, userId: string) =>
  json(`/swiss/${ctrl.data.id}/player/${userId}`).then(data => {
    ctrl.data.playerInfo = data;
    ctrl.redraw();
  }).catch(onFail);

export default {
  join: throttle(1000, join),
  withdraw: throttle(1000, withdraw),
  loadPage: throttle(1000, loadPage),
  loadPageOf,
  reloadSoon: throttle(4000, reload),
  reloadNow: reload,
  playerInfo
};
