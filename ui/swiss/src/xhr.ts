import { throttlePromiseDelay } from 'common/throttle';
import { json, form } from 'common/xhr';
import SwissCtrl from './ctrl';
import { isOutcome } from './util';

// when the tournament no longer exists
const onFail = () => site.reload();

const join = (ctrl: SwissCtrl, password?: string) =>
  json(`/api/swiss/${ctrl.data.id}/join`, {
    method: 'post',
    body: form({
      password: password || '',
    }),
  }).catch(onFail);

const withdraw = (ctrl: SwissCtrl) =>
  json(`/api/swiss/${ctrl.data.id}/withdraw`, { method: 'post' }).catch(onFail);

const loadPage = (ctrl: SwissCtrl, p: number, callback?: () => void) =>
  json(`/swiss/${ctrl.data.id}/standing/${p}`).then(data => {
    ctrl.loadPage(data);
    callback?.();
    ctrl.redraw();
  });

const loadPageOf = (ctrl: SwissCtrl, userId: string): Promise<any> =>
  json(`/swiss/${ctrl.data.id}/page-of/${userId}`);

const reload = (ctrl: SwissCtrl) =>
  json(
    `/swiss/${ctrl.data.id}?page=${ctrl.focusOnMe ? '' : ctrl.page}&playerInfo=${ctrl.playerInfoId || ''}`,
  ).then(data => {
    ctrl.reload(data);
    ctrl.redraw();
  }, onFail);

const playerInfo = (ctrl: SwissCtrl, userId: string) =>
  json(`/swiss/${ctrl.data.id}/player/${userId}`).then(data => {
    ctrl.data.playerInfo = data;
    ctrl.redraw();
  }, onFail);

const readSheetMin = (str: string) =>
  str
    ? str.split('|').map(s =>
        isOutcome(s)
          ? s
          : {
              g: s.slice(0, 8),
              o: s[8] == 'o',
              w: s[8] == 'w' ? true : s[8] == 'l' ? false : undefined,
            },
      )
    : [];

export default {
  join: throttlePromiseDelay(() => 1000, join),
  withdraw: throttlePromiseDelay(() => 1000, withdraw),
  loadPage: throttlePromiseDelay(() => 1000, loadPage),
  loadPageOf,
  reloadNow: reload,
  playerInfo,
  readSheetMin,
};
