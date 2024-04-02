import AnalyseCtrl from '../../ctrl';
import RelayCtrl, { RelayTab } from './relayCtrl';
import * as licon from 'common/licon';
import { bind, dataIcon, onInsert, looseH as h } from 'common/snabbdom';
import { VNode } from 'snabbdom';
import { innerHTML } from 'common/richText';
import { RelayGroup, RelayRound } from './interfaces';
import { view as multiBoardView } from '../multiBoard';
import { defined } from 'common';
import StudyCtrl from '../studyCtrl';
import { toggle } from 'common/controls';
import * as xhr from 'common/xhr';
import { teamsView } from './relayTeams';
import { makeChat } from '../../view/components';
import { gamesList } from './relayGames';
import { renderStreamerMenu, renderPinnedImage } from './relayView';
import { renderVideoPlayer } from './videoPlayerView';
import { leaderboardView } from './relayLeaderboard';
import { gameLinksListener } from '../studyChapters';

export default function (ctrl: AnalyseCtrl): VNode | undefined {
  const study = ctrl.study,
    relay = study?.relay;
  if (!study || !relay?.tourShow()) return undefined;
  const tab = relay.tab();
  const content =
    tab == 'overview'
      ? overview(relay, ctrl)
      : tab == 'boards'
      ? games(relay, study, ctrl)
      : tab == 'teams'
      ? teams(relay, study, ctrl)
      : leaderboard(relay, ctrl);

  return h('div.box.relay-tour', content);
}

export const tourSide = (ctrl: AnalyseCtrl, study: StudyCtrl, relay: RelayCtrl) => {
  const empty = study.chapters.list.looksNew();
  return [
    h(
      'aside.relay-tour__side',
      {
        hook: {
          insert: gameLinksListener(study.setChapter),
        },
      },
      [
        ...(empty
          ? [startCountdown(relay)]
          : [
              h('div.relay-tour__side__header', [
                h(
                  'button.relay-tour__side__name',
                  { hook: bind('mousedown', relay.tourShow.toggle, relay.redraw) },
                  study.data.name,
                ),
                h('button.streamer-show.data-count', {
                  attrs: { 'data-icon': licon.Mic, 'data-count': relay.streams.length, title: 'Streamers' },
                  class: {
                    disabled: !relay.streams.length,
                    active: relay.showStreamerMenu(),
                    streaming: relay.isStreamer(),
                  },
                  hook: bind('click', relay.showStreamerMenu.toggle, relay.redraw),
                }),
                h('button.relay-tour__side__search', {
                  attrs: { 'data-icon': licon.Search, title: 'Search' },
                  hook: bind('click', study.search.open.toggle),
                }),
              ]),
            ]),
        relay.showStreamerMenu() && renderStreamerMenu(relay),
        !empty && gamesList(study, relay),
      ],
    ),
    h('div.chat__members', {
      hook: onInsert(el => {
        makeChat(ctrl, chat => el.parentNode!.insertBefore(chat, el));
        site.watchers(el);
      }),
    }),
  ];
};

const startCountdown = (relay: RelayCtrl) => {
  const round = relay.currentRound(),
    startsAt = defined(round.startsAt) && new Date(round.startsAt),
    date = startsAt && h('time', site.dateFormat()(startsAt));
  return h('div.relay-tour__side__empty', { attrs: dataIcon(licon.RadioTower) }, [
    h('strong', round.name),
    ...(startsAt
      ? startsAt.getTime() < Date.now() + 1000 * 10 * 60 // in the last 10 minutes, only say it's soon.
        ? ['The broadcast will start very soon.', date]
        : [h('strong', site.timeago(startsAt)), date]
      : ['The broadcast has not yet started.']),
  ]);
};

const leaderboard = (relay: RelayCtrl, ctrl: AnalyseCtrl) => [
  ...header(relay, ctrl),
  relay.leaderboard && leaderboardView(relay.leaderboard),
];

const overview = (relay: RelayCtrl, ctrl: AnalyseCtrl) => [
  ...header(relay, ctrl),
  relay.data.tour.markup
    ? h('div.relay-tour__markup', {
        hook: innerHTML(relay.data.tour.markup, () => relay.data.tour.markup!),
      })
    : h('div.relay-tour__markup', relay.data.tour.description),
];

const groupSelect = (relay: RelayCtrl, group: RelayGroup) => {
  const clickHook = { hook: bind('click', relay.groupSelectShow.toggle, relay.redraw) };
  return h('div.mselect.relay-tour__mselect.relay-tour__group-select', [
    h(
      'label.mselect__label',
      clickHook,
      group.tours.find(t => t.id == relay.data.tour.id)?.name || relay.data.tour.name,
    ),
    ...(relay.groupSelectShow()
      ? [
          h('label.fullscreen-mask', clickHook),
          h(
            'nav.mselect__list',
            group.tours.map(tour =>
              h(
                `a.mselect__item${tour.id == relay.data.tour.id ? '.current' : ''}`,
                { attrs: { href: `/broadcast/-/${tour.id}` } },
                tour.name,
              ),
            ),
          ),
        ]
      : []),
  ]);
};

const roundSelect = (relay: RelayCtrl, study: StudyCtrl) => {
  const clickHook = { hook: bind('click', relay.roundSelectShow.toggle, relay.redraw) };
  const round = relay.currentRound();
  const icon = roundStateIcon(round, true);
  return h('div.mselect.relay-tour__mselect.relay-tour__round-select', [
    h('label.mselect__label.relay-tour__round-select__label', clickHook, [
      h('span.relay-tour__round-select__name', round.name),
      h(
        'span.relay-tour__round-select__status',
        icon || [round.startsAt ? site.timeago(round.startsAt) : undefined],
      ),
    ]),
    ...(relay.roundSelectShow()
      ? [
          h('label.fullscreen-mask', clickHook),
          h(
            'div.relay-tour__round-select__list.mselect__list',
            h(
              'table',
              h(
                'tbody',
                {
                  hook: bind('click', (e: MouseEvent) => {
                    const target = e.target as HTMLElement;
                    if (target.tagName != 'A') site.redirect($(target).parents('tr').find('a').attr('href')!);
                  }),
                },
                relay.data.rounds.map(round =>
                  h(`tr.mselect__item${round.id == study.data.id ? '.current-round' : ''}`, [
                    h('td.name', h('a', { attrs: { href: relay.roundPath(round) } }, round.name)),
                    h('td.time', round.startsAt ? site.dateFormat()(new Date(round.startsAt)) : '-'),
                    h(
                      'td.status',
                      roundStateIcon(round, false) ||
                        (round.startsAt ? site.timeago(round.startsAt) : undefined),
                    ),
                  ]),
                ),
              ),
            ),
          ),
        ]
      : []),
  ]);
};

const games = (relay: RelayCtrl, study: StudyCtrl, ctrl: AnalyseCtrl) => [
  ...header(relay, ctrl),
  study.chapters.list.looksNew() ? undefined : multiBoardView(study.multiBoard, study),
];

const teams = (relay: RelayCtrl, study: StudyCtrl, ctrl: AnalyseCtrl) => [
  ...header(relay, ctrl),
  relay.teams && teamsView(relay.teams, study.chapters.list),
];

const header = (relay: RelayCtrl, ctrl: AnalyseCtrl) => {
  const d = relay.data,
    study = ctrl.study!,
    group = d.group,
    embedVideo =
      d.videoUrls && window.getComputedStyle(document.body).getPropertyValue('--allow-video') === 'true';

  return [
    h('div.spacer'),
    h('div.relay-tour__header', [
      h('div.relay-tour__header__content', [
        h('h1', group?.name || d.tour.name),
        h('div.relay-tour__header__selectors', [
          group && groupSelect(relay, group),
          roundSelect(relay, study),
        ]),
      ]),
      h(
        `div.relay-tour__header__image${embedVideo ? '.video' : ''}`,
        embedVideo
          ? renderVideoPlayer(relay)
          : relay.pinStreamer() && d.pinned?.image
          ? renderPinnedImage(relay)
          : d.tour.image
          ? h('img', { attrs: { src: d.tour.image } })
          : study.members.isOwner()
          ? h(
              'a.button.relay-tour__header__image-upload',
              { attrs: { href: `/broadcast/${d.tour.id}/edit` } },
              'Upload tournament image',
            )
          : undefined,
      ),
    ]),
    h('div.relay-tour__nav', [makeTabs(ctrl), ...subscribe(relay, ctrl)]),
  ];
};

const subscribe = (relay: RelayCtrl, ctrl: AnalyseCtrl) =>
  defined(relay.data.isSubscribed)
    ? [
        toggle(
          {
            name: 'Subscribe',
            id: 'tour-subscribe',
            cls: 'relay-tour__subscribe',
            checked: relay.data.isSubscribed,
            change: (v: boolean) => {
              xhr.text(`/broadcast/${relay.data.tour.id}/subscribe?set=${v}`, { method: 'post' });
              relay.data.isSubscribed = v;
              ctrl.redraw();
            },
          },
          ctrl.trans,
          ctrl.redraw,
        ),
      ]
    : [];

const makeTabs = (ctrl: AnalyseCtrl) => {
  const study = ctrl.study,
    relay = study?.relay;
  if (!relay) return undefined;

  const makeTab = (key: RelayTab, name: string) =>
    h(
      `span.relay-tour__tabs--${key}`,
      {
        class: { active: relay.tab() === key },
        attrs: { role: 'tab' },
        hook: bind('mousedown', () => relay.openTab(key)),
      },
      name,
    );
  return h('nav.relay-tour__tabs', { attrs: { role: 'tablist' } }, [
    makeTab('overview', 'Overview'),
    makeTab('boards', 'Boards'),
    relay.teams && makeTab('teams', 'Teams'),
    relay.data.tour.leaderboard ? makeTab('leaderboard', 'Leaderboard') : undefined,
  ]);
};

const roundStateIcon = (round: RelayRound, titleAsText: boolean) =>
  round.ongoing
    ? h(
        'span.round-state.ongoing',
        { attrs: { ...dataIcon(licon.DiscBig), title: !titleAsText && 'Ongoing' } },
        titleAsText && 'Ongoing',
      )
    : round.finished &&
      h(
        'span.round-state.finished',
        { attrs: { ...dataIcon(licon.Checkmark), title: !titleAsText && 'Finished' } },
        titleAsText && 'Finished',
      );
