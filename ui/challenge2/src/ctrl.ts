import * as xhr from './xhr'
import { Ctrl, ChallengeOpts, ChallengeData, ChallengeUser, Redraw } from './interfaces'

export default function(opts: ChallengeOpts, redraw: Redraw): Ctrl {

  let data: ChallengeData | undefined;

  let initiating = true;
  let reloading = false;
  let trans: Trans = (key: string) => key;

  function update(d: ChallengeData) {
    data = d;
    if (d.i18n) trans = window.lichess.trans(d.i18n);
    initiating = false;
    reloading = false;
    opts.setCount(countActiveIn());
    notifyNew();
    redraw();
  }

  function countActiveIn() {
    return data ? data.in.filter(c => !c.declined).length : 0;
  }

  function notifyNew() {
    data && data.in.forEach(c => {
      if (window.lichess.once('c-' + c.id)) {
        if (!window.lichess.quietMode) {
          opts.show();
          window.lichess.sound.newChallenge();
        }
        c.challenger && window.lichess.desktopNotification(showUser(c.challenger) + ' challenges you!');
        opts.pulse();
      }
    });
  }

  function showUser(user: ChallengeUser) {
    var rating = user.rating + (user.provisional ? '?' : '');
    var fullName = (user.title ? user.title + ' ' : '') + user.name;
    return fullName + ' (' + rating + ')';
  }

  return {
    data: () => data,
    initiating: () => initiating,
    reloading: () => reloading,
    trans,
    update,
    decline(id) {
      data && data.in.forEach(c => {
        if (c.id === id) {
          c.declined = true;
          xhr.decline(id);
        }
      });
    },
    cancel(id) {
      data && data.out.forEach(c => {
        if (c.id === id) {
          c.declined = true;
          xhr.cancel(id);
        }
      });
    }
  };
};
