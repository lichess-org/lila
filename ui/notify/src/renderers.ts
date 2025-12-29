import { h, type VNode } from 'snabbdom';
import * as licon from 'lib/licon';
import type { Notification, Renderer, Renderers } from './interfaces';
import { timeago } from 'lib/i18n';

export default function makeRenderers(): Renderers {
  return {
    streamStart: {
      html: n =>
        generic(n, `/streamer/${n.content.sid}/redirect`, licon.Mic, [
          h('span', [h('strong', n.content.name), drawTime(n)]),
          h('span', i18n.site.startedStreaming),
        ]),
      text: n => i18n.site.xStartedStreaming(n.content.streamerName),
    },
    genericLink: {
      html: n =>
        generic(n, n.content.url, n.content.icon, [
          h('span', [h('strong', n.content.title), drawTime(n)]),
          h('span', n.content.text),
        ]),
      text: n => n.content.title || n.content.text,
    },
    broadcastRound: {
      html: n =>
        generic(n, n.content.url, licon.RadioTower, [
          h('span', [h('strong', n.content.title), drawTime(n)]),
          h('span', n.content.text),
        ]),
      text: n => n.content.title || n.content.text,
    },
    mention: {
      html: n =>
        generic(n, `/forum/redirect/post/${n.content.postId}`, licon.BubbleConvo, [
          h('span', [h('strong', userFullName(n.content.mentionedBy)), drawTime(n)]),
          h('span', i18n.site.mentionedYouInX(n.content.topic)),
        ]),
      text: n => i18n.site.xMentionedYouInY(userFullName(n.content.mentionedBy), n.content.topic),
    },
    invitedStudy: {
      html: n =>
        generic(n, '/study/' + n.content.studyId, licon.StudyBoard, [
          h('span', [h('strong', userFullName(n.content.invitedBy)), drawTime(n)]),
          h('span', i18n.site.invitedYouToX(n.content.studyName)),
        ]),
      text: n => i18n.site.xInvitedYouToY(userFullName(n.content.invitedBy), n.content.studyName),
    },
    privateMessage: {
      html: n =>
        generic(n, '/inbox/' + n.content.user!.name, licon.BubbleSpeech, [
          h('span', [h('strong', userFullName(n.content.user)), drawTime(n)]),
          h('span', n.content.text),
        ]),
      text: n => userFullName(n.content.sender) + ': ' + n.content.text,
    },
    teamJoined: {
      html: n =>
        generic(n, '/team/' + n.content.id, licon.Group, [
          h('span', [h('strong', n.content.name), drawTime(n)]),
          h('span', i18n.site.youAreNowPartOfTeam),
        ]),
      text: n => i18n.site.youHaveJoinedTeamX(n.content.name),
    },
    titledTourney: {
      html: n =>
        generic(n, '/tournament/' + n.content.id, licon.Trophy, [
          h('span', [h('strong', 'Lichess Titled Arena'), drawTime(n)]),
          h('span', n.content.text),
        ]),
      text: _ => 'Lichess Titled Arena',
    },
    reportedBanned: {
      html: n =>
        generic(n, undefined, licon.InfoCircle, [
          h('span', [h('strong', i18n.site.someoneYouReportedWasBanned)]),
          h('span', i18n.site.thankYou),
        ]),
      text: _ => i18n.site.someoneYouReportedWasBanned,
    },
    gameEnd: {
      html: n => {
        let result;
        switch (n.content.win) {
          case true:
            result = i18n.site.congratsYouWon;
            break;
          case false:
            result = i18n.site.defeat;
            break;
          default:
            result = i18n.site.draw;
        }
        return generic(n, '/' + n.content.id, licon.PaperAirplane, [
          h('span', [h('strong', i18n.site.gameVsX(userFullName(n.content.opponent))), drawTime(n)]),
          h('span', result),
        ]);
      },
      text: n => {
        let result;
        switch (n.content.win) {
          case true:
            result = i18n.site.victory;
            break;
          case false:
            result = i18n.site.defeat;
            break;
          default:
            result = i18n.site.draw;
        }
        return i18n.site.resVsX(result, userFullName(n.content.opponent));
      },
    },
    planStart: {
      html: n =>
        generic(n, '/patron', licon.Wings, [
          h('span', [h('strong', 'You just became a lichess Patron.'), drawTime(n)]),
        ]),
      text: _ => 'You just became a lichess Patron.',
    },
    planExpire: {
      html: n =>
        generic(n, '/patron', licon.Wings, [h('span', [h('strong', 'Patron account expired'), drawTime(n)])]),
      text: _ => 'Patron account expired',
    },
    ratingRefund: {
      html: n =>
        generic(n, '/faq#rating-refund', licon.InfoCircle, [
          h('span', [h('strong', i18n.site.lostAgainstTOSViolator), drawTime(n)]),
          h('span', i18n.site.refundXpointsTimeControlY(n.content.points, n.content.perf)),
        ]),
      text: n => i18n.site.refundXpointsTimeControlY(n.content.points, n.content.perf),
    },
    corresAlarm: {
      html: n =>
        generic(n, '/' + n.content.id, licon.PaperAirplane, [
          h('span', [h('strong', i18n.site.timeAlmostUp), drawTime(n)]),
          // not a `LightUser`, could be a game against Stockfish
          h('span', i18n.site.gameVsX(n.content.op)),
        ]),
      text: _ => i18n.site.timeAlmostUp,
    },
    irwinDone: jobDone('Irwin'),
    kaladinDone: jobDone('Kaladin'),
    recap: {
      html: n => {
        site.asset.loadI18n('recap');
        const title = i18n.recap?.recapReady?.(n.content.year) || `Your ${n.content.year} recap is ready!`;
        const text = i18n.recap?.awaitQuestion || 'What have you been up to this year?';
        return generic(n, '/recap', licon.Logo, [h('span', h('strong', title)), h('span', text)]);
      },
      text: n => {
        site.asset.loadI18n('recap');
        return i18n.recap?.recapReady?.(n.content.year) || `Your ${n.content.year} recap is ready!`;
      },
    },
  };
}

const jobDone = (name: string): Renderer => ({
  html: n =>
    generic(n, '/@/' + n.content.user!.name + '?mod', licon.Agent, [
      h('span', [h('strong', userFullName(n.content.user)), drawTime(n)]),
      h('span', `${name} job complete!`),
    ]),
  text: n => `${n.content.user!.name}: ${name} job complete!`,
});

function generic(n: Notification, url: string | undefined, icon: string, content: VNode[]): VNode {
  return h(
    url ? 'a' : 'span',
    {
      class: { site_notification: true, [n.type]: true, new: !n.read },
      attrs: { key: n.date, ...(url ? { href: url } : {}) },
    },
    [h('i', { attrs: { 'data-icon': icon } }), h('span.content', content)],
  );
}

function drawTime(n: Notification) {
  const date = new Date(n.date);
  return h('time.timeago', { attrs: { title: date.toLocaleString(), datetime: n.date } }, timeago(date));
}

function userFullName(u?: LightUser) {
  if (!u) return 'Anonymous';
  return u.title ? u.title + ' ' + u.name : u.name;
}
