import * as xhr from 'common/xhr';
import { Ctrl, ChallengeOpts, ChallengeData, Reasons } from './interfaces';

export default function (opts: ChallengeOpts, data: ChallengeData, redraw: () => void): Ctrl {
  let trans = (key: string) => key;
  let redirecting = false;
  let reasons: Reasons = {};

  const showRatings = !document.body.classList.contains('no-rating');

  function update(d: ChallengeData) {
    data = d;
    if (d.i18n) trans = lichess.trans(d.i18n).noarg;
    if (d.reasons) reasons = d.reasons;
    opts.setCount(countActiveIn());
    notifyNew();
  }

  function countActiveIn() {
    return data.in.filter(c => !c.declined).length;
  }

  function notifyNew() {
    data.in.forEach(c => {
      if (lichess.once('c-' + c.id)) {
        if (!lichess.quietMode && data.in.length <= 3) {
          opts.show();
          lichess.sound.playOnce('newChallenge');
        }
        opts.pulse();
      }
    });
  }

  update(data);

  return {
    data: () => data,
    trans: () => trans,
    reasons: () => reasons,
    showRatings,
    update,
    decline(id, reason) {
      data.in.forEach(c => {
        if (c.id === id) {
          c.declined = true;
          xhr
            .text(`/challenge/${id}/decline`, { method: 'post', body: xhr.form({ reason }) })
            .catch(() => lichess.announce({ msg: 'Failed to send challenge decline' }));
        }
      });
    },
    cancel(id) {
      data.out.forEach(c => {
        if (c.id === id) {
          c.declined = true;
          xhr
            .text(`/challenge/${id}/cancel`, { method: 'post' })
            .catch(() => lichess.announce({ msg: 'Failed to send challenge cancellation' }));
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
