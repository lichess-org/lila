import * as licon from 'common/licon';
import { looseH as h, bind, onInsert, dataIcon, MaybeVNode } from 'common/snabbdom';
import { LogEvent } from './interfaces';
import RelayCtrl from './relayCtrl';
import { memoize } from 'common';
import { side as studyViewSide } from '../studyView';
import StudyCtrl from '../studyCtrl';

export default function (ctrl: RelayCtrl, study: StudyCtrl): MaybeVNode {
  const contributor = ctrl.members.canContribute(),
    sync = ctrl.data.sync;
  return contributor || study.data.admin
    ? h('div.relay-admin__container', [
        contributor
          ? h('div.relay-admin', { hook: onInsert(_ => site.asset.loadCssPath('analyse.relay-admin')) }, [
              h('h2', [
                h('span.text', { attrs: dataIcon(licon.RadioTower) }, 'Broadcast manager'),
                h('a', { attrs: { href: `/broadcast/round/${ctrl.id}/edit`, 'data-icon': licon.Gear } }),
              ]),
              sync?.url || sync?.ids || sync?.urls ? (sync.ongoing ? stateOn : stateOff)(ctrl) : statePush(),
              renderLog(ctrl),
            ])
          : undefined,
        contributor || study.data.admin ? studyViewSide(study, false) : undefined,
      ])
    : undefined;
}

const logSuccess = (e: LogEvent) =>
  e.moves ? [h('strong', '' + e.moves), ` new move${e.moves > 1 ? 's' : ''}`] : ['Nothing new'];

function renderLog(ctrl: RelayCtrl) {
  const url = ctrl.data.sync?.url;
  const logLines = (ctrl.data.sync?.log || [])
    .slice(0)
    .reverse()
    .map(e => {
      const err =
        e.error &&
        h('a', url ? { attrs: { href: url, target: '_blank', rel: 'noopener nofollow' } } : {}, e.error);
      return h(
        'div' + (err ? '.err' : ''),
        { key: e.at, attrs: dataIcon(err ? licon.CautionCircle : licon.Checkmark) },
        [h('div', [...(err ? [err] : logSuccess(e)), h('time', dateFormatter()(new Date(e.at)))])],
      );
    });
  if (ctrl.loading()) logLines.unshift(h('div.load', [h('i.ddloader'), 'Polling source...']));
  return h('div.log', logLines);
}

function stateOn(ctrl: RelayCtrl) {
  const sync = ctrl.data.sync;
  return h(
    'div.state.on.clickable',
    { hook: bind('click', _ => ctrl.setSync(false)), attrs: dataIcon(licon.ChasingArrows) },
    [
      h('div', [
        'Connected ',
        ...(sync
          ? [
              sync.delay ? `with ${sync.delay}s delay ` : null,
              ...(sync.url
                ? ['to source', h('br'), sync.url.replace(/https?:\/\//, '')]
                : sync.ids
                ? ['to', h('br'), sync.ids.length, ' game(s)']
                : sync.urls
                ? ['to', h('br'), sync.urls.length, ' sources']
                : []),
              sync.filter ? ` (round ${sync.filter})` : null,
              sync.slices ? ` (slice ${sync.slices})` : null,
            ]
          : []),
      ]),
    ],
  );
}

const stateOff = (ctrl: RelayCtrl) =>
  h(
    'div.state.off.clickable',
    { hook: bind('click', _ => ctrl.setSync(true)), attrs: dataIcon(licon.PlayTriangle) },
    [h('div.fat', 'Click to connect')],
  );

const statePush = () =>
  h('div.state.push', { attrs: dataIcon(licon.UploadCloud) }, ['Listening to Broadcaster App']);

const dateFormatter = memoize(() =>
  window.Intl && Intl.DateTimeFormat
    ? new Intl.DateTimeFormat(document.documentElement.lang, {
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
        second: 'numeric',
      }).format
    : (d: Date) => d.toLocaleString(),
);
