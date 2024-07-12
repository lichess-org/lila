import AnalyseCtrl from '../../ctrl';
import RelayCtrl, { RelayTab } from './relayCtrl';
import * as licon from 'common/licon';
import { bind, dataIcon, onInsert, looseH as h } from 'common/snabbdom';
import { VNode } from 'snabbdom';
import { innerHTML } from 'common/richText';
import { RelayData, RelayGroup, RelayRound, RelayTourDates, RelayTourInfo } from './interfaces';
import { view as multiBoardView } from '../multiBoard';
import { defined, memoize } from 'common';
import StudyCtrl from '../studyCtrl';
import { toggle } from 'common/controls';
import * as xhr from 'common/xhr';
import { teamsView } from './relayTeams';
import { statsView } from './relayStats';
import { makeChat, type RelayViewContext } from '../../view/components';
import { gamesList } from './relayGames';
import { renderStreamerMenu, renderPinnedImage } from './relayView';
import { renderVideoPlayer } from './videoPlayerView';
import { leaderboardView } from './relayLeaderboard';
import { gameLinksListener } from '../studyChapters';
import { copyMeInput } from 'common/copyMe';
import { baseUrl } from '../../view/util';

export function renderRelayTour(ctx: RelayViewContext): VNode | undefined {
  const tab = ctx.relay.tab();
  const content =
    tab == 'overview'
      ? overview(ctx)
      : tab == 'boards'
      ? games(ctx)
      : tab == 'teams'
      ? teams(ctx)
      : tab == 'stats'
      ? stats(ctx)
      : leaderboard(ctx);

  return h('div.box.relay-tour', content);
}

export const tourSide = (ctx: RelayViewContext) => {
  const { ctrl, study, relay } = ctx;
  const empty = study.chapters.list.looksNew();
  return h(
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
      h('div.chat__members', {
        hook: onInsert(el => {
          makeChat(ctrl, chat => el.parentNode!.insertBefore(chat, el));
          site.watchers(el);
        }),
      }),
    ],
  );
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

const leaderboard = (ctx: RelayViewContext) => [
  ...header(ctx),
  ctx.relay.leaderboard && leaderboardView(ctx.relay.leaderboard),
];

const showInfo = (i: RelayTourInfo, dates?: RelayTourDates) => {
  const contents = [
    ['dates', dates && showDates(dates), 'objects.spiral-calendar'],
    ['format', i.format, 'objects.crown'],
    ['tc', i.tc, 'objects.mantelpiece-clock'],
    ['players', i.players, 'activity.sparkles'],
  ]
    .map(
      ([key, value, icon]) =>
        value &&
        icon &&
        h('div.relay-tour__info__' + key, [h('img', { attrs: { src: site.asset.flairSrc(icon) } }), value]),
    )
    .filter(defined);
  return contents.length ? h('div.relay-tour__info', contents) : undefined;
};

const dateFormat = memoize(() =>
  window.Intl && Intl.DateTimeFormat
    ? new Intl.DateTimeFormat(site.displayLocale, {
        month: 'short',
        day: '2-digit',
      } as any).format
    : (d: Date) => d.toLocaleDateString(),
);

const showDates = (dates: RelayTourDates) => {
  const rendered = dates.map(date => dateFormat()(new Date(date)));
  // if the tournament only lasts one day, don't show the end date
  return rendered[1] ? `${rendered[0]} - ${rendered[1]}` : rendered[0];
};

const showSource = (data: RelayData) =>
  data.lcc
    ? h('div.relay-tour__source', [
        'PGN source: ',
        h('a', { attrs: { href: 'https://www.livechesscloud.com' } }, 'LiveChessCloud'),
      ])
    : undefined;

const overview = (ctx: RelayViewContext) => {
  const tour = ctx.relay.data.tour;
  return [
    ...header(ctx),
    showInfo(tour.info, tour.dates),
    tour.markup
      ? h('div.relay-tour__markup', {
          hook: innerHTML(tour.markup, () => tour.markup!),
        })
      : undefined,
    showSource(ctx.relay.data),
    h('div.relay-tour__share', [
      h('h2.text', { attrs: dataIcon(licon.Heart) }, 'Sharing is caring'),
      ...[
        [tour.name, ctx.relay.tourPath()],
        [ctx.study.data.name, ctx.relay.roundPath()],
        [
          `${ctx.study.data.name} PGN`,
          `${ctx.relay.roundPath()}.pgn`,
          h('div.form-help', [
            'A public, real-time PGN source for this round. We also offer a ',
            h(
              'a',
              { attrs: { href: 'https://lichess.org/api#tag/Broadcasts/operation/broadcastStreamRoundPgn' } },
              'streaming API',
            ),
            ' for faster and more efficient synchronisation.',
          ]),
        ],
      ].map(([i18n, path, help]: [string, string, VNode]) =>
        h('div.form-group', [
          h('label.form-label', ctx.ctrl.trans.noarg(i18n)),
          copyMeInput(`${baseUrl()}${path}`),
          help,
        ]),
      ),
    ]),
  ];
};

const groupSelect = (relay: RelayCtrl, group: RelayGroup) => {
  const toggle = relay.groupSelectShow;
  const clickHook = { hook: bind('click', toggle.toggle, relay.redraw) };
  return h(
    'div.mselect.relay-tour__mselect.relay-tour__group-select',
    {
      class: { mselect__active: toggle() },
    },
    [
      h(
        'label.mselect__label',
        clickHook,
        group.tours.find(t => t.id == relay.data.tour.id)?.name || relay.data.tour.name,
      ),
      ...(toggle()
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
    ],
  );
};

const roundSelect = (relay: RelayCtrl, study: StudyCtrl) => {
  const toggle = relay.roundSelectShow;
  const clickHook = { hook: bind('click', toggle.toggle, relay.redraw) };
  const round = relay.currentRound();
  const icon = roundStateIcon(round, true);
  return h(
    'div.mselect.relay-tour__mselect.relay-tour__round-select',
    {
      class: { mselect__active: toggle() },
    },
    [
      h('label.mselect__label.relay-tour__round-select__label', clickHook, [
        h('span.relay-tour__round-select__name', round.name),
        h(
          'span.relay-tour__round-select__status',
          icon || [round.startsAt ? site.timeago(round.startsAt) : undefined],
        ),
      ]),
      ...(toggle()
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
                      if (target.tagName != 'A')
                        site.redirect($(target).parents('tr').find('a').attr('href')!);
                    }),
                  },
                  relay.data.rounds.map(round =>
                    h(`tr.mselect__item${round.id == study.data.id ? '.current-round' : ''}`, [
                      h('td.name', h('a', { attrs: { href: relay.roundUrlWithHash(round) } }, round.name)),
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
    ],
  );
};

const games = (ctx: RelayViewContext) => [
  ...header(ctx),
  ctx.study.chapters.list.looksNew() ? undefined : multiBoardView(ctx.study.multiBoard, ctx.study),
  showSource(ctx.relay.data),
];

const teams = (ctx: RelayViewContext) => [
  ...header(ctx),
  ctx.relay.teams && teamsView(ctx.relay.teams, ctx.study.chapters.list),
];

const stats = (ctx: RelayViewContext) => [...header(ctx), statsView(ctx.relay.stats)];

const header = (ctx: RelayViewContext) => {
  const { ctrl, relay, allowVideo } = ctx;
  const d = relay.data,
    study = ctrl.study!,
    group = d.group,
    embedVideo = d.videoUrls && allowVideo;

  return [
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
          ? renderPinnedImage(ctx)
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
            title:
              'Subscribe to be notified when each round starts. You can toggle bell or push ' +
              'notifications for broadcasts in your account preferences.',
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
    study.members.myMember() && relay.data.tour.tier ? makeTab('stats', 'Stats') : undefined,
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
