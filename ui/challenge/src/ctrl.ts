import notify from 'common/notification';
import { once } from 'common/storage';
import type { ChallengeData, ChallengeOpts, ChallengeUser, Ctrl } from './interfaces';

const li = window.lishogi;

export default function (opts: ChallengeOpts, data: ChallengeData, redraw: () => void): Ctrl {
  let redirecting = false;

  function update(d: ChallengeData) {
    data = d;
    opts.setCount(countActiveIn());
    notifyNew();
  }

  function countActiveIn() {
    return data.in.filter(c => !c.declined).length;
  }

  function notifyNew() {
    data.in.forEach(c => {
      if (once('c-' + c.id, false)) {
        if (!li.quietMode && data.in.length <= 3) {
          opts.show();
          li.sound.play('newChallenge');
        }
        const pushSubsribed =
          Number.parseInt(li.storage.get('push-subscribed2') || '0', 10) + 86400000 >= Date.now(); // 24h
        !pushSubsribed && c.challenger && notify(showUser(c.challenger) + ' challenges you!');
        opts.pulse();
      }
    });
  }

  function showUser(user: ChallengeUser) {
    const rating = user.rating + (user.provisional ? '?' : ''),
      fullName = (user.title ? user.title + ' ' : '') + user.name;
    return fullName + ' (' + rating + ')';
  }

  update(data);

  return {
    data: () => data,
    update,
    decline(id) {
      data.in.forEach(c => {
        if (c.id === id) {
          c.declined = true;
          window.lishogi.xhr.text('POST', `/challenge/${id}/decline`).catch(() =>
            window.lishogi.announce({
              msg: 'Failed to send challenge decline',
            }),
          );
        }
      });
    },
    cancel(id) {
      data.out.forEach(c => {
        if (c.id === id) {
          c.declined = true;
          window.lishogi.xhr.text('POST', `/challenge/${id}/cancel`).catch(() =>
            window.lishogi.announce({
              msg: 'Failed to send challenge cancellation',
            }),
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
