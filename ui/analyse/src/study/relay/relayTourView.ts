import type AnalyseCtrl from '../../ctrl';
import RelayCtrl, { type RelayTab } from './relayCtrl';
import * as licon from 'lib/licon';
import { bind, dataIcon, onInsert, hl, type LooseVNode } from 'lib/snabbdom';
import type { VNode } from 'snabbdom';
import { innerHTML, richHTML } from 'lib/richText';
import type { RelayData, RelayGroup, RelayRound, RelayTourDates, RelayTourInfo } from './interfaces';
import { view as multiBoardView } from '../multiBoard';
import { defined, memoize } from 'lib';
import type StudyCtrl from '../studyCtrl';
import { toggle, copyMeInput } from 'lib/view/controls';
import { text as xhrText } from 'lib/xhr';
import { teamsView } from './relayTeams';
import { statsView } from './relayStats';
import { type RelayViewContext } from '../../view/components';
import { gamesList } from './relayGames';
import { renderStreamerMenu } from './relayView';
import { playersView } from './relayPlayers';
import { gameLinksListener } from '../studyChapters';
import { baseUrl } from '../../view/util';
import { commonDateFormat, timeago } from 'lib/i18n';
import { renderChat } from 'lib/chat/renderChat';
import { displayColumns, isTouchDevice } from 'lib/device';
import { verticalResize } from 'lib/view/verticalResize';
import { watchers } from 'lib/view/watchers';
import { userLink } from 'lib/view/userLink';

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

  return hl('div.box.relay-tour', content);
}

export const tourSide = (ctx: RelayViewContext, kid: LooseVNode) => {
  const { ctrl, study, relay } = ctx;
  const empty = study.chapters.list.looksNew();
  const resizeId =
    !isTouchDevice() && displayColumns() > (ctx.hasRelayTour ? 1 : 2) && `relayTour/${relay.data.tour.id}`;
  return hl(
    'aside.relay-tour__side',
    {
      hook: {
        insert: gameLinksListener(study.chapterSelect),
      },
    },
    [
      empty
        ? [startCountdown(relay)]
        : [
            hl('div.relay-tour__side__header', [
              hl(
                'button.relay-tour__side__name',
                { hook: bind('mousedown', relay.tourShow.toggle, relay.redraw) },
                relay.roundName(),
              ),
              !ctrl.isEmbed &&
                hl('button.streamer-show.data-count', {
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
              hl('button.relay-tour__side__search', {
                attrs: { 'data-icon': licon.Search },
                hook: bind('click', study.search.open.toggle),
              }),
            ]),
          ],
      !ctrl.isEmbed && relay.showStreamerMenu() && renderStreamerMenu(relay),
      !empty ? gamesList(study, relay) : hl('div.vertical-spacer'),
      !empty &&
        resizeId &&
        verticalResize({
          key: `relay-games.${resizeId}`,
          min: () => 48,
          max: () => 48 * study.chapters.list.size(),
          initialMaxHeight: window.innerHeight / 2,
        }),
      ctx.ctrl.chatCtrl && renderChat(ctx.ctrl.chatCtrl),
      resizeId &&
        verticalResize({
          key: 'relay-chat',
          id: resizeId,
          min: () => 0,
          max: () => window.innerHeight,
          initialMaxHeight: window.innerHeight / 3,
          kid: hl('div.chat__members', { hook: onInsert(el => watchers(el, false)) }),
        }),
      kid,
    ],
  );
};

const startCountdown = (relay: RelayCtrl) => {
  const round = relay.currentRound(),
    startsAt = defined(round.startsAt) && new Date(round.startsAt),
    date = startsAt && hl('time', commonDateFormat(startsAt));
  return hl('div.relay-tour__side__empty', { attrs: dataIcon(licon.RadioTower) }, [
    hl('strong', round.name),
    startsAt
      ? startsAt.getTime() < Date.now() + 1000 * 10 * 60 // in the last 10 minutes, only say it's soon.
        ? [i18n.broadcast.startVerySoon, date]
        : [hl('strong', timeago(startsAt)), date]
      : [i18n.broadcast.notYetStarted],
  ]);
};

const players = (ctx: RelayViewContext) => [header(ctx), playersView(ctx.relay.players, ctx.relay.data.tour)];

export const showInfo = (i: RelayTourInfo, dates?: RelayTourDates) => {
  const contents = [
    ['dates', dates && showDates(dates), 'objects.spiral-calendar', 'Dates'],
    ['format', i.format, 'objects.crown', 'Format'],
    ['tc', i.tc, 'objects.mantelpiece-clock', 'Time control'],
    ['location', i.location, 'travel-places.globe-showing-europe-africa', 'Location'],
    ['players', i.players, 'activity.sparkles', 'Star players'],
    ['website', i.website, null, null, i18n.broadcast.officialWebsite],
    ['standings', i.standings, null, null, i18n.broadcast.standings],
  ]
    .map(
      ([key, value, icon, textAlternative, linkName]) =>
        key &&
        value &&
        hl('div.relay-tour__info__' + key, [
          icon && hl('img', { attrs: { src: site.asset.flairSrc(icon), alt: textAlternative! } }),
          linkName
            ? hl('a', { attrs: { href: value, target: '_blank', rel: 'nofollow noreferrer' } }, linkName)
            : value,
        ]),
    )
    .filter(defined);

  return contents.length ? hl('div.relay-tour__info', contents) : undefined;
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
    ? hl('div.relay-tour__source', [
        'PGN source: ',
        hl('a', { attrs: { href: 'https://www.livechesscloud.com' } }, 'LiveChessCloud'),
      ])
    : undefined;

const overview = (ctx: RelayViewContext) => {
  const tour = ctx.relay.data.tour;
  return [
    header(ctx),
    showInfo(tour.info, tour.dates),
    tour.description &&
      hl('div.relay-tour__markup', {
        hook: innerHTML(tour.description, () => tour.description!),
      }),
    ctx.ctrl.isEmbed || [showSource(ctx.relay.data), share(ctx)],
  ];
};

const share = (ctx: RelayViewContext) => {
  const iframe = (path: string) =>
    `<iframe src="${baseUrl()}/embed${path}" style="width: 100%; aspect-ratio: 4/3;" frameborder="0"></iframe>`;
  const iframeHelp = hl(
    'div.form-help',
    i18n.broadcast.iframeHelp.asArray(
      hl('a', { attrs: { href: '/developers#broadcast' } }, i18n.broadcast.webmastersPage),
    ),
  );
  const roundName = ctx.relay.roundName();
  return hl(
    'div.relay-tour__share',
    [
      [ctx.relay.data.tour.name, ctx.relay.tourPath()],
      [roundName, ctx.relay.roundPath()],
      [
        `${roundName} PGN`,
        `${ctx.relay.roundPath()}.pgn`,
        hl(
          'div.form-help',
          i18n.broadcast.pgnSourceHelp.asArray(
            hl(
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
      hl('div.form-group', [
        hl('label.form-label', text),
        copyMeInput(path.startsWith('/') ? `${baseUrl()}${path}` : path),
        help,
      ]),
    ),
  );
};

const groupSelect = (ctx: RelayViewContext, group: RelayGroup) => {
  const toggle = ctx.relay.groupSelectShow;
  const clickHook = { hook: bind('click', toggle.toggle, ctx.relay.redraw) };
  return hl(
    'div.mselect.relay-tour__mselect.relay-tour__group-select',
    {
      class: { mselect__active: toggle() },
    },
    [
      hl(
        'label.mselect__label',
        clickHook,
        group.tours.find(t => t.id === ctx.relay.data.tour.id)?.name || ctx.relay.data.tour.name,
      ),
      toggle() && [
        hl('label.fullscreen-mask', clickHook),
        hl(
          'nav.mselect__list',
          group.tours.map(tour =>
            hl(
              `a.mselect__item${tour.id === ctx.relay.data.tour.id ? '.current' : ''}`,
              { attrs: { href: ctx.study.embeddablePath(`/broadcast/-/${tour.id}`) } },
              tour.name,
            ),
          ),
        ),
      ],
    ],
  );
};

const roundSelect = (relay: RelayCtrl, study: StudyCtrl) => {
  const toggle = relay.roundSelectShow;
  const clickHook = { hook: bind('click', toggle.toggle, relay.redraw) };
  const round = relay.currentRound();
  const icon = roundStateIcon(round, true);
  return hl(
    'div.mselect.relay-tour__mselect.relay-tour__round-select',
    {
      class: { mselect__active: toggle() },
    },
    [
      hl('label.mselect__label.relay-tour__round-select__label', clickHook, [
        hl('span.relay-tour__round-select__name', round.name),
        hl('span.relay-tour__round-select__status', icon || (!!round.startsAt && timeago(round.startsAt))),
      ]),
      toggle() && [
        hl('label.fullscreen-mask', clickHook),
        hl(
          'div.relay-tour__round-select__list.mselect__list',
          {
            hook: onInsert(el => {
              const goTo = el.querySelector('.ongoing-round') ?? el.querySelector('.current-round');
              goTo
                ?.closest('.relay-tour__round-select')
                ?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            }),
          },
          hl(
            'table',
            hl(
              'tbody',
              {
                hook: bind('click', e => {
                  const target = e.target as HTMLElement;
                  if (target.tagName !== 'A') site.redirect($(target).parents('tr').find('a').attr('href')!);
                }),
              },
              relay.data.rounds.map((round, i) =>
                hl(
                  'tr.mselect__item',
                  {
                    class: {
                      ['current-round']: round.id === study.data.id,
                      ['ongoing-round']: !!round.ongoing,
                    },
                  },
                  [
                    hl(
                      'td.name',
                      hl(
                        'a',
                        { attrs: { href: study.embeddablePath(relay.roundUrlWithHash(round)) } },
                        round.name,
                      ),
                    ),
                    hl(
                      'td.time',
                      !!round.startsAt
                        ? commonDateFormat(new Date(round.startsAt))
                        : round.startsAfterPrevious &&
                            i18n.broadcast.startsAfter(
                              relay.data.rounds[i - 1]?.name || 'the previous round',
                            ),
                    ),
                    hl(
                      'td.status',
                      roundStateIcon(round, false) || (!!round.startsAt && timeago(round.startsAt)),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ],
    ],
  );
};

const games = (ctx: RelayViewContext) => [
  header(ctx),
  ctx.study.chapters.list.looksNew()
    ? hl(
        'div.relay-tour__note',
        hl('div', [
          hl('div', i18n.broadcast.noBoardsYet),
          ctx.study.members.myMember() &&
            hl(
              'small',
              i18n.broadcast.boardsCanBeLoaded.asArray(
                hl('a', { attrs: { href: '/broadcast/app' } }, 'Broadcaster App'),
              ),
            ),
        ]),
      )
    : multiBoardView(ctx.study.multiBoard, ctx.study),
  !ctx.ctrl.isEmbed && showSource(ctx.relay.data),
];

const teams = (ctx: RelayViewContext) => [
  header(ctx),
  ctx.relay.teams && teamsView(ctx.relay.teams, ctx.study.chapters.list, ctx.relay.players),
];

const stats = (ctx: RelayViewContext) => [header(ctx), statsView(ctx.relay.stats)];

const header = (ctx: RelayViewContext) => {
  const { ctrl, relay } = ctx;
  const d = relay.data,
    group = d.group,
    studyD = ctrl.study?.data.description;

  return [
    hl('div.relay-tour__header', [
      hl('div.relay-tour__header__content', [
        hl('h1', group?.name || d.tour.name),
        hl('div.relay-tour__header__selectors', [
          group && groupSelect(ctx, group),
          roundSelect(relay, ctx.study),
        ]),
      ]),
      broadcastImageOrStream(ctx),
    ]),
    studyD && hl('div.relay-tour__note.pinned', hl('div', [hl('div', { hook: richHTML(studyD, false) })])),
    d.tour.communityOwner &&
      hl(
        'div.relay-tour__note',
        hl('div', [
          hl('div', i18n.broadcast.communityBroadcast),
          hl('small', i18n.broadcast.createdAndManagedBy.asArray(userLink(d.tour.communityOwner))),
        ]),
      ),
    d.note &&
      hl(
        'div.relay-tour__note',
        hl('div', [
          hl('div', { hook: richHTML(d.note, false) }),
          hl('small', 'This note is visible to contributors only.'),
        ]),
      ),
    hl('div.relay-tour__nav', [makeTabs(ctrl), subscribe(relay, ctrl)]),
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
    hl(
      `span.relay-tour__tabs--${key}`,
      {
        class: { active: relay.tab() === key },
        attrs: { role: 'tab' },
        hook: bind('mousedown', () => relay.openTab(key)),
      },
      name,
    );
  return hl('nav.relay-tour__tabs', { attrs: { role: 'tablist' } }, [
    makeTab('overview', i18n.broadcast.overview),
    makeTab('boards', i18n.broadcast.boards),
    makeTab('players', i18n.site.players),
    relay.teams && makeTab('teams', i18n.broadcast.teams),
    study.members.myMember() && !!relay.data.tour.tier
      ? makeTab('stats', i18n.site.stats)
      : ctrl.isEmbed &&
        hl(
          'a.relay-tour__tabs--open.text',
          {
            attrs: { href: relay.tourPath(), target: '_blank', 'data-icon': licon.Expand },
          },
          i18n.broadcast.openLichess,
        ),
  ]);
};

const roundStateIcon = (round: RelayRound, titleAsText: boolean) =>
  round.ongoing
    ? hl(
        'span.round-state.ongoing',
        { attrs: { ...dataIcon(licon.DiscBig), title: !titleAsText && i18n.broadcast.ongoing } },
        titleAsText && i18n.broadcast.ongoing,
      )
    : round.finished &&
      hl(
        'span.round-state.finished',
        { attrs: { ...dataIcon(licon.Checkmark), title: !titleAsText && i18n.site.finished } },
        titleAsText && i18n.site.finished,
      );

const broadcastImageOrStream = (ctx: RelayViewContext) => {
  const { relay, allowVideo } = ctx;
  const d = relay.data,
    embedVideo = (d.videoUrls || relay.isPinnedStreamOngoing()) && allowVideo;

  return hl(
    `div.relay-tour__header__image${embedVideo ? '.video' : ''}`,
    embedVideo
      ? relay.videoPlayer?.render()
      : d.tour.image
        ? hl('img', { attrs: { src: d.tour.image } })
        : ctx.study.members.isOwner()
          ? hl(
              'a.button.relay-tour__header__image-upload',
              { attrs: { href: `/broadcast/${d.tour.id}/edit` } },
              i18n.broadcast.uploadImage,
            )
          : undefined,
  );
};
