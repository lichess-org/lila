import type AnalyseCtrl from '../../ctrl';
import RelayCtrl, { type RelayTab } from './relayCtrl';
import * as licon from 'common/licon';
import { bind, dataIcon, onInsert, looseH as h } from 'common/snabbdom';
import type { VNode } from 'snabbdom';
import { innerHTML, richHTML } from 'common/richText';
import type { RelayData, RelayGroup, RelayRound, RelayTourDates, RelayTourInfo } from './interfaces';
import { view as multiBoardView } from '../multiBoard';
import { defined, memoize } from 'common';
import type StudyCtrl from '../studyCtrl';
import { toggle } from 'common/controls';
import { text as xhrText } from 'common/xhr';
import { teamsView } from './relayTeams';
import { statsView } from './relayStats';
import { makeChatEl, type RelayViewContext } from '../../view/components';
import { gamesList } from './relayGames';
import { renderStreamerMenu } from './relayView';
import { playersView } from './relayPlayers';
import { gameLinksListener } from '../studyChapters';
import { copyMeInput } from 'common/copyMe';
import { baseUrl } from '../../view/util';
import { commonDateFormat, timeago } from 'common/i18n';
import { watchers } from 'common/watchers';

export function renderRelayTour(ctx: RelayViewContext): VNode | undefined {
  const tab = ctx.relay.tab();
  const content =
    tab === 'boards'
      ? games(ctx)
      : tab === 'teams'
        ? teams(ctx)
        : tab === 'stats'
          ? stats(ctx)
          : tab === 'players'
            ? players(ctx)
            : overview(ctx);

  return h('div.box.relay-tour', content);
}

export const tourSide = (ctx: RelayViewContext) => {
  const { ctrl, study, relay } = ctx;
  const empty = study.chapters.list.looksNew();
  return h(
    'aside.relay-tour__side',
    {
      hook: {
        insert: gameLinksListener(study.chapterSelect),
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
                relay.roundName(),
              ),
              !ctrl.isEmbed &&
                h('button.streamer-show.data-count', {
                  attrs: {
                    'data-icon': licon.Mic,
                    'data-count': relay.streams.length,
                    title: i18n.site.streamersMenu,
                  },
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
      !ctrl.isEmbed && relay.showStreamerMenu() && renderStreamerMenu(relay),
      !empty && gamesList(study, relay),
      !ctrl.isEmbed &&
        h('div.chat__members', {
          hook: onInsert(el => {
            makeChatEl(ctrl, chat => el.parentNode!.insertBefore(chat, el));
            watchers(el);
          }),
        }),
    ],
  );
};

const startCountdown = (relay: RelayCtrl) => {
  const round = relay.currentRound(),
    startsAt = defined(round.startsAt) && new Date(round.startsAt),
    date = startsAt && h('time', commonDateFormat(startsAt));
  return h('div.relay-tour__side__empty', { attrs: dataIcon(licon.RadioTower) }, [
    h('strong', round.name),
    ...(startsAt
      ? startsAt.getTime() < Date.now() + 1000 * 10 * 60 // in the last 10 minutes, only say it's soon.
        ? [i18n.broadcast.startVerySoon, date]
        : [h('strong', timeago(startsAt)), date]
      : [i18n.broadcast.notYetStarted]),
  ]);
};

const players = (ctx: RelayViewContext) => [
  ...header(ctx),
  playersView(ctx.relay.players, ctx.relay.data.tour),
];

const showInfo = (i: RelayTourInfo, dates?: RelayTourDates) => {
  const contents = [
    ['dates', dates && showDates(dates), 'objects.spiral-calendar'],
    ['format', i.format, 'objects.crown'],
    ['tc', i.tc, 'objects.mantelpiece-clock'],
    ['location', i.location, 'travel-places.globe-showing-europe-africa'],
    ['players', i.players, 'activity.sparkles'],
    ['website', i.website, null, i18n.broadcast.officialWebsite],
    ['standings', i.standings, null, i18n.broadcast.standings],
  ]
    .map(
      ([key, value, icon, linkName]) =>
        key &&
        value &&
        h('div.relay-tour__info__' + key, [
          icon && h('img', { attrs: { src: site.asset.flairSrc(icon) } }),
          linkName
            ? h('a', { attrs: { href: value, target: '_blank', rel: 'nofollow noreferrer' } }, linkName)
            : value,
        ]),
    )
    .filter(defined);

  return contents.length ? h('div.relay-tour__info', contents) : undefined;
};

const dateFormat = memoize(() =>
  window.Intl && Intl.DateTimeFormat
    ? new Intl.DateTimeFormat(site.displayLocale, {
        month: 'short',
        day: '2-digit',
      }).format
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
    tour.description
      ? h('div.relay-tour__markup', {
          hook: innerHTML(tour.description, () => tour.description!),
        })
      : undefined,
    ...(ctx.ctrl.isEmbed ? [] : [showSource(ctx.relay.data), share(ctx)]),
  ];
};

const share = (ctx: RelayViewContext) => {
  const iframe = (path: string) =>
    `<iframe src="${baseUrl()}/embed${path}" style="width: 100%; aspect-ratio: 4/3;" frameborder="0"></iframe>`;
  const iframeHelp = h(
    'div.form-help',
    i18n.broadcast.iframeHelp.asArray(
      h('a', { attrs: { href: '/developers#broadcast' } }, i18n.broadcast.webmastersPage),
    ),
  );
  const roundName = ctx.relay.roundName();
  return h(
    'div.relay-tour__share',
    [
      [ctx.relay.data.tour.name, ctx.relay.tourPath()],
      [roundName, ctx.relay.roundPath()],
      [
        `${roundName} PGN`,
        `${ctx.relay.roundPath()}.pgn`,
        h(
          'div.form-help',
          i18n.broadcast.pgnSourceHelp.asArray(
            h(
              'a',
              { attrs: { href: '/api#tag/Broadcasts/operation/broadcastStreamRoundPgn' } },
              'streaming API',
            ),
          ),
        ),
      ],
      [i18n.broadcast.embedThisBroadcast, iframe(ctx.relay.tourPath()), iframeHelp],
      [i18n.broadcast.embedThisRound(roundName), iframe(ctx.relay.roundPath()), iframeHelp],
    ].map(([text, path, help]: [string, string, VNode]) =>
      h('div.form-group', [
        h('label.form-label', text),
        copyMeInput(path.startsWith('/') ? `${baseUrl()}${path}` : path),
        help,
      ]),
    ),
  );
};

const groupSelect = (ctx: RelayViewContext, group: RelayGroup) => {
  const toggle = ctx.relay.groupSelectShow;
  const clickHook = { hook: bind('click', toggle.toggle, ctx.relay.redraw) };
  return h(
    'div.mselect.relay-tour__mselect.relay-tour__group-select',
    {
      class: { mselect__active: toggle() },
    },
    [
      h(
        'label.mselect__label',
        clickHook,
        group.tours.find(t => t.id === ctx.relay.data.tour.id)?.name || ctx.relay.data.tour.name,
      ),
      ...(toggle()
        ? [
            h('label.fullscreen-mask', clickHook),
            h(
              'nav.mselect__list',
              group.tours.map(tour =>
                h(
                  `a.mselect__item${tour.id === ctx.relay.data.tour.id ? '.current' : ''}`,
                  { attrs: { href: ctx.study.embeddablePath(`/broadcast/-/${tour.id}`) } },
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
          icon || [round.startsAt ? timeago(round.startsAt) : undefined],
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
                    hook: bind('click', e => {
                      const target = e.target as HTMLElement;
                      if (target.tagName !== 'A')
                        site.redirect($(target).parents('tr').find('a').attr('href')!);
                    }),
                  },
                  relay.data.rounds.map((round, i) =>
                    h(`tr.mselect__item${round.id === study.data.id ? '.current-round' : ''}`, [
                      h(
                        'td.name',
                        h(
                          'a',
                          { attrs: { href: study.embeddablePath(relay.roundUrlWithHash(round)) } },
                          round.name,
                        ),
                      ),
                      h(
                        'td.time',
                        round.startsAt
                          ? commonDateFormat(new Date(round.startsAt))
                          : round.startsAfterPrevious
                            ? i18n.broadcast.startsAfter(
                                relay.data.rounds[i - 1]?.name || 'the previous round',
                              )
                            : '',
                      ),
                      h(
                        'td.status',
                        roundStateIcon(round, false) ||
                          (round.startsAt ? timeago(round.startsAt) : undefined),
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
  ctx.study.chapters.list.looksNew()
    ? h(
        'div.relay-tour__note',
        h('div', [
          h('div', i18n.broadcast.noBoardsYet),
          ctx.study.members.myMember() &&
            h(
              'small',
              i18n.broadcast.boardsCanBeLoaded.asArray(
                h('a', { attrs: { href: '/broadcast/app' } }, 'Broadcaster App'),
              ),
            ),
        ]),
      )
    : multiBoardView(ctx.study.multiBoard, ctx.study),
  !ctx.ctrl.isEmbed && showSource(ctx.relay.data),
];

const teams = (ctx: RelayViewContext) => [
  ...header(ctx),
  ctx.relay.teams && teamsView(ctx.relay.teams, ctx.study.chapters.list, ctx.relay.players),
];

const stats = (ctx: RelayViewContext) => [...header(ctx), statsView(ctx.relay.stats)];

const header = (ctx: RelayViewContext) => {
  const { ctrl, relay } = ctx;
  const d = relay.data,
    group = d.group,
    studyD = ctrl.study?.data.description;

  return [
    h('div.relay-tour__header', [
      h('div.relay-tour__header__content', [
        h('h1', group?.name || d.tour.name),
        h('div.relay-tour__header__selectors', [
          group && groupSelect(ctx, group),
          roundSelect(relay, ctx.study),
        ]),
      ]),
      broadcastImageOrStream(ctx),
    ]),
    studyD && h('div.relay-tour__note.pinned', h('div', [h('div', { hook: richHTML(studyD, false) })])),
    d.note &&
      h(
        'div.relay-tour__note',
        h('div', [
          h('div', { hook: richHTML(d.note, false) }),
          h('small', 'This note is visible to contributors only.'),
        ]),
      ),
    h('div.relay-tour__nav', [makeTabs(ctrl), ...subscribe(relay, ctrl)]),
  ];
};

const subscribe = (relay: RelayCtrl, ctrl: AnalyseCtrl) =>
  defined(relay.data.isSubscribed)
    ? [
        toggle(
          {
            name: i18n.site.subscribe,
            id: 'tour-subscribe',
            title: i18n.broadcast.subscribeTitle,
            cls: 'relay-tour__subscribe',
            checked: relay.data.isSubscribed,
            change: (v: boolean) => {
              xhrText(`/broadcast/${relay.data.tour.id}/subscribe?set=${v}`, { method: 'post' });
              relay.data.isSubscribed = v;
              ctrl.redraw();
            },
          },
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
    makeTab('overview', i18n.broadcast.overview),
    makeTab('boards', i18n.broadcast.boards),
    makeTab('players', i18n.site.players),
    relay.teams && makeTab('teams', i18n.broadcast.teams),
    study.members.myMember() && relay.data.tour.tier
      ? makeTab('stats', i18n.site.stats)
      : ctrl.isEmbed
        ? h(
            'a.relay-tour__tabs--open.text',
            {
              attrs: { href: relay.tourPath(), target: '_blank', 'data-icon': licon.Expand },
            },
            i18n.broadcast.openLichess,
          )
        : undefined,
  ]);
};

const roundStateIcon = (round: RelayRound, titleAsText: boolean) =>
  round.ongoing
    ? h(
        'span.round-state.ongoing',
        { attrs: { ...dataIcon(licon.DiscBig), title: !titleAsText && i18n.broadcast.ongoing } },
        titleAsText && i18n.broadcast.ongoing,
      )
    : round.finished &&
      h(
        'span.round-state.finished',
        { attrs: { ...dataIcon(licon.Checkmark), title: !titleAsText && i18n.site.finished } },
        titleAsText && i18n.site.finished,
      );

const broadcastImageOrStream = (ctx: RelayViewContext) => {
  const { relay, allowVideo } = ctx;
  const d = relay.data,
    embedVideo = (d.videoUrls || relay.isPinnedStreamOngoing()) && allowVideo;

  return h(
    `div.relay-tour__header__image${embedVideo ? '.video' : ''}`,
    embedVideo
      ? relay.videoPlayer?.render()
      : d.tour.image
        ? h('img', { attrs: { src: d.tour.image } })
        : ctx.study.members.isOwner()
          ? h(
              'a.button.relay-tour__header__image-upload',
              { attrs: { href: `/broadcast/${d.tour.id}/edit` } },
              i18n.broadcast.uploadImage,
            )
          : undefined,
  );
};
