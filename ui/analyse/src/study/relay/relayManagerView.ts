import * as licon from 'lib/licon';
import { hl, bind, onInsert, dataIcon, type MaybeVNode } from 'lib/snabbdom';
import type { LogEvent } from './interfaces';
import type RelayCtrl from './relayCtrl';
import { memoize } from 'lib';
import { side as studyViewSide } from '../studyView';
import type StudyCtrl from '../studyCtrl';

export default function (ctrl: RelayCtrl, study: StudyCtrl): MaybeVNode {
  const contributor = study.members.canContribute(),
    sync = ctrl.data.sync;
  return contributor || study.data.admin
    ? hl('div.relay-admin__container', [
        contributor &&
          hl('div.relay-admin', { hook: onInsert(_ => site.asset.loadCssPath('analyse.relay-admin')) }, [
            hl('h2', [
              hl('span.text', { attrs: dataIcon(licon.RadioTower) }, 'Broadcast manager'),
              hl('a', {
                attrs: { href: `/broadcast/round/${study.data.id}/edit`, 'data-icon': licon.Gear },
              }),
            ]),
            sync?.url || sync?.ids || sync?.urls || sync?.users
              ? (sync.ongoing ? stateOn : stateOff)(ctrl)
              : statePush(),
            renderLog(ctrl),
          ]),
        (contributor || study.data.admin) && studyViewSide(study, false),
      ])
    : undefined;
}

const logSuccess = (e: LogEvent) =>
  e.moves ? [hl('strong', '' + e.moves), ` new move${e.moves > 1 ? 's' : ''}`] : ['Nothing new'];

function renderLog(ctrl: RelayCtrl) {
  const url = ctrl.data.sync?.url;
  const logLines = (ctrl.data.sync?.log || [])
    .slice(0)
    .reverse()
    .map(e => {
      const err =
        e.error && hl('a', url ? { attrs: { href: url, target: '_blank', rel: 'nofollow' } } : {}, e.error);
      return hl(
        'div' + (err ? '.err' : ''),
        { key: e.at, attrs: dataIcon(err ? licon.CautionCircle : licon.Checkmark) },
        [hl('div', [err ? [err] : logSuccess(e), hl('time', dateFormatter()(new Date(e.at)))])],
      );
    });
  if (ctrl.loading()) logLines.unshift(hl('div.load', [hl('i.ddloader'), 'Polling source...']));
  return hl('div.log', logLines);
}

function stateOn(ctrl: RelayCtrl) {
  const sync = ctrl.data.sync;
  return hl(
    'div.state.on.clickable',
    { hook: bind('click', _ => ctrl.setSync(false)), attrs: dataIcon(licon.ChasingArrows) },
    [
      hl('div', [
        'Connected ',
        sync && [
          !!sync.delay && `with ${sync.delay}s delay `,
          sync.url
            ? ['to source', hl('br'), sync.url.replace(/https?:\/\//, '')]
            : sync.ids
              ? ['to', hl('br'), sync.ids.length, ' game(s)']
              : sync.users
                ? [
                    'to',
                    hl('br'),
                    sync.users.length > 4 ? `${sync.users.length} users` : sync.users.join(' '),
                  ]
                : sync.urls && ['to', hl('br'), sync.urls.length, ' sources'],
          !!sync.filter && ` (round ${sync.filter})`,
          !!sync.slices && ` (slice ${sync.slices})`,
        ],
      ]),
    ],
  );
}

const stateOff = (ctrl: RelayCtrl) =>
  hl(
    'div.state.off.clickable',
    { hook: bind('click', _ => ctrl.setSync(true)), attrs: dataIcon(licon.PlayTriangle) },
    [hl('div.fat', 'Click to connect')],
  );

const statePush = () =>
  hl('div.state.push', { attrs: dataIcon(licon.UploadCloud) }, ['Listening to Broadcaster App']);

const dateFormatter = memoize(
  () =>
    new Intl.DateTimeFormat(site.displayLocale, {
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: 'numeric',
      second: 'numeric',
    }).format,
);
