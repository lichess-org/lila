import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

import { Notification, Renderers } from './interfaces';

// function generic(n: Notification, url: string | undefined, icon: string, content: VNode[]): VNode {
export const renderers: Renderers = {
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
        h('span', ' mentioned you in « ' + n.content.topic + ' ».'),
      ]),
    text: n => userFullName(n.content.mentionedBy) + ' mentioned you in « ' + n.content.topic + ' ».',
  },
  invitedStudy: {
    html: n =>
      generic(n, '/study/' + n.content.studyId, '4', [
        h('span', [h('strong', userFullName(n.content.invitedBy)), drawTime(n)]),
        h('span', ' invited you to « ' + n.content.studyName + ' ».'),
      ]),
    text: n => userFullName(n.content.invitedBy) + ' invited you to « ' + n.content.studyName + ' ».',
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
        h('span', 'You are now part of the team.'),
      ]),
    text: n => 'You have joined  « ' + n.content.name + '  ».',
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
        h('span', [h('strong', 'Someone you reported was banned')]),
        h('span', 'Thank you for the help!'),
      ]),
    text: _ => 'Someone you reported was banned',
  },
  gameEnd: {
    html: n => {
      let result;
      switch (n.content.win) {
        case true:
          result = 'Congratulations, you won!';
          break;
        case false:
          result = 'You lost!';
          break;
        default:
          result = "It's a draw.";
      }
      return generic(n, '/' + n.content.id, ';', [
        h('span', [h('strong', 'Game vs ' + userFullName(n.content.opponent)), drawTime(n)]),
        h('span', result),
      ]);
    },
    text: function (n) {
      let result;
      switch (n.content.win) {
        case true:
          result = 'Victory';
          break;
        case false:
          result = 'Defeat';
          break;
        default:
          result = 'Draw';
      }
      return result + ' vs ' + userFullName(n.content.opponent);
    },
  },
  planStart: {
    html: n =>
      generic(n, '/patron', '', [
        h('span', [h('strong', 'Thank you!'), drawTime(n)]),
        h('span', 'You just became a lichess Patron.'),
      ]),
    text: _ => 'You just became a lichess Patron.',
  },
  planExpire: {
    html: n =>
      generic(n, '/patron', '', [
        h('span', [h('strong', 'Patron account expired'), drawTime(n)]),
        h('span', 'Please consider renewing it!'),
      ]),
    text: _ => 'Patron account expired',
  },
  coachReview: {
    html: n =>
      generic(n, '/coach/edit', ':', [
        h('span', [h('strong', 'New pending review'), drawTime(n)]),
        h('span', 'Someone reviewed your coach profile.'),
      ]),
    text: _ => 'New pending review',
  },
  ratingRefund: {
    html: n =>
      generic(n, '/player/myself', '', [
        h('span', [h('strong', 'You lost to someone who violated the Lichess TOS'), drawTime(n)]),
        h('span', 'Refund: ' + n.content.points + ' ' + n.content.perf + ' rating points.'),
      ]),
    text: n => 'Refund: ' + n.content.points + ' ' + n.content.perf + ' rating points.',
  },
  corresAlarm: {
    html: n =>
      generic(n, '/' + n.content.id, ';', [
        h('span', [h('strong', 'Time is almost up!'), drawTime(n)]),
        h('span', 'Game vs ' + n.content.op),
      ]),
    text: _ => 'Time is almost up!',
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
