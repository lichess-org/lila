import { h, VNode } from 'snabbdom';

import { Notification, Renderers } from './interfaces';

// function generic(n: Notification, url: string | undefined, icon: string, content: VNode[]): VNode {
export default function makeRenderers(trans: Trans): Renderers {
  return {
    genericLink: {
      html: n =>
        generic(n, n.content.url, n.content.icon, [
          h('span', [h('strong', n.content.title), drawTime(n)]),
          h('span', n.content.text),
        ]),
      text: n => n.content.title || n.content.text,
    },
    mention: {
      html: n =>
        generic(n, '/forum/redirect/post/' + n.content.postId, 'd', [
          h('span', [h('strong', userFullName(n.content.mentionedBy)), drawTime(n)]),
          h('span', trans('mentionedYouInX', n.content.topic)),
        ]),
      text: n => trans('xMentionedYouInY', userFullName(n.content.mentionedBy), n.content.topic),
    },
    invitedStudy: {
      html: n =>
        generic(n, '/study/' + n.content.studyId, '4', [
          h('span', [h('strong', userFullName(n.content.invitedBy)), drawTime(n)]),
          h('span', trans('invitedYouToX', n.content.studyName)),
        ]),
      text: n => trans('xInvitedYouToY', userFullName(n.content.invitedBy), n.content.studyName),
    },
    privateMessage: {
      html: n =>
        generic(n, '/inbox/' + n.content.user.name, 'c', [
          h('span', [h('strong', userFullName(n.content.user)), drawTime(n)]),
          h('span', n.content.text),
        ]),
      text: n => userFullName(n.content.sender) + ': ' + n.content.text,
    },
    teamJoined: {
      html: n =>
        generic(n, '/team/' + n.content.id, 'f', [
          h('span', [h('strong', n.content.name), drawTime(n)]),
          h('span', trans.noarg('youAreNowPartOfTeam')),
        ]),
      text: n => trans('youHaveJoinedTeamX', n.content.name),
    },
    titledTourney: {
      html: n =>
        generic(n, '/tournament/' + n.content.id, 'g', [
          h('span', [h('strong', 'Lichess Titled Arena'), drawTime(n)]),
          h('span', n.content.text),
        ]),
      text: _ => 'Lichess Titled Arena',
    },
    reportedBanned: {
      html: n =>
        generic(n, undefined, '', [
          h('span', [h('strong', trans.noarg('someoneYouReportedWasBanned'))]),
          h('span', trans.noarg('thankYou')),
        ]),
      text: _ => trans.noarg('someoneYouReportedWasBanned'),
    },
    gameEnd: {
      html: n => {
        let result;
        switch (n.content.win) {
          case true:
            result = trans.noarg('congratsYouWon');
            break;
          case false:
            result = trans.noarg('defeat');
            break;
          default:
            result = trans.noarg('draw');
        }
        return generic(n, '/' + n.content.id, ';', [
          h('span', [h('strong', trans('gameVsX', userFullName(n.content.opponent))), drawTime(n)]),
          h('span', result),
        ]);
      },
      text: n => {
        let result;
        switch (n.content.win) {
          case true:
            result = trans.noarg('victory');
            break;
          case false:
            result = trans.noarg('defeat');
            break;
          default:
            result = trans.noarg('draw');
        }
        return trans('resVsX', result, userFullName(n.content.opponent));
      },
    },
    planStart: {
      html: n =>
        generic(n, '/patron', '', [
          h('span', [h('strong', trans.noarg('thankYou')), drawTime(n)]),
          h('span', trans.noarg('youJustBecamePatron')),
        ]),
      text: _ => trans.noarg('youJustBecamePatron'),
    },
    planExpire: {
      html: n =>
        generic(n, '/patron', '', [
          h('span', [h('strong', trans.noarg('patronAccountExpired')), drawTime(n)]),
          h('span', trans.noarg('pleaseReconsiderRenewIt')),
        ]),
      text: _ => trans.noarg('patronAccountExpired'),
    },
    coachReview: {
      html: n =>
        generic(n, '/coach/edit', ':', [
          h('span', [h('strong', 'New pending review'), drawTime(n)]),
          h('span', trans.noarg('someoneReviewedYourCoachProfile')),
        ]),
      text: _ => trans.noarg('newPendingReview'),
    },
    ratingRefund: {
      html: n =>
        generic(n, '/player/myself', '', [
          h('span', [h('strong', trans.noarg('lostAgainstTOSViolator')), drawTime(n)]),
          h('span', trans('refundXpointsTimeControlY', n.content.points, n.content.perf)),
        ]),
      text: n => trans('refundXpointsTimeControlY', n.content.points, n.content.perf),
    },
    corresAlarm: {
      html: n =>
        generic(n, '/' + n.content.id, ';', [
          h('span', [h('strong', trans.noarg('timeAlmostUp')), drawTime(n)]),
          // not a `LightUser`, could be a game against Stockfish
          h('span', trans('gameVsX', n.content.op)),
        ]),
      text: _ => trans.noarg('timeAlmostUp'),
    },
    irwinDone: {
      html: n =>
        generic(n, '/@/' + n.content.user.name + '?mod', '', [
          h('span', [h('strong', userFullName(n.content.user)), drawTime(n)]),
          h('span', 'Irwin job complete!'),
        ]),
      text: n => n.content.user.name + ': Irwin job complete!',
    },
  };
}

function generic(n: Notification, url: string | undefined, icon: string, content: VNode[]): VNode {
  return h(
    url ? 'a' : 'span',
    {
      class: {
        site_notification: true,
        [n.type]: true,
        new: !n.read,
      },
      attrs: url ? { href: url } : undefined,
    },
    [
      h('i', {
        attrs: { 'data-icon': icon },
      }),
      h('span.content', content),
    ]
  );
}

function drawTime(n: Notification) {
  var date = new Date(n.date);
  return h(
    'time.timeago',
    {
      attrs: {
        title: date.toLocaleString(),
        datetime: n.date,
      },
    },
    lichess.timeago(date)
  );
}

function userFullName(u?: LightUser) {
  if (!u) return 'Anonymous';
  return u.title ? u.title + ' ' + u.name : u.name;
}
