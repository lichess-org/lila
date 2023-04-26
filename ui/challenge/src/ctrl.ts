import notify from 'common/notification';
import { ChallengeData, ChallengeOpts, ChallengeUser, Ctrl } from './interfaces';
import * as xhr from './xhr';

const li = window.lishogi;

export default function (opts: ChallengeOpts, data: ChallengeData, redraw: () => void): Ctrl {
  let trans: Trans;
  let redirecting = false;

  function update(d: ChallengeData) {
    data = d;
    trans = li.trans(d.i18n || {});
    opts.setCount(countActiveIn());
    notifyNew();
  }

  function countActiveIn() {
    return data.in.filter(c => !c.declined).length;
  }

  function notifyNew() {
    data.in.forEach(c => {
      if (li.once('c-' + c.id)) {
        if (!li.quietMode && data.in.length <= 3) {
          opts.show();
          li.sound.newChallenge();
        }
        const pushSubsribed = parseInt(li.storage.get('push-subscribed2') || '0', 10) + 86400000 >= Date.now(); // 24h
        !pushSubsribed && c.challenger && notify(showUser(c.challenger) + ' challenges you!');
        opts.pulse();
      }
    });
  }

  function showUser(user: ChallengeUser) {
    var rating = user.rating + (user.provisional ? '?' : '');
    var fullName = (user.title ? user.title + ' ' : '') + user.name;
    return fullName + ' (' + rating + ')';
  }

  update(data);

  return {
    data: () => data,
    trans: () => trans,
    update,
    decline(id) {
      data.in.forEach(c => {
        if (c.id === id) {
          c.declined = true;
          xhr.decline(id).fail(() =>
            window.lishogi.announce({
              msg: 'Failed to send challenge decline',
            })
          );
        }
      });
    },
    cancel(id) {
      data.out.forEach(c => {
        if (c.id === id) {
          c.declined = true;
          xhr.cancel(id).fail(() =>
            window.lishogi.announce({
              msg: 'Failed to send challenge cancellation',
            })
          );
        }
      });
    },
    redirecting: () => redirecting,
    onRedirect() {
      redirecting = true;
      requestAnimationFrame(redraw);
    },
  };
}
