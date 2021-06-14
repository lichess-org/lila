import { h, VNode } from 'snabbdom';
import RelayCtrl from './relayCtrl';
import { dataIcon, bind, onInsert } from '../../util';
import { LogEvent } from './interfaces';

export default function (ctrl: RelayCtrl): VNode | undefined {
  return ctrl.members.canContribute()
    ? h(
        'div.relay-admin',
        {
          hook: onInsert(_ => lichess.loadCssPath('analyse.relay-admin')),
        },
        [
          h('h2', [
            h('span.text', { attrs: dataIcon('') }, 'Broadcast manager'),
            h('a', {
              attrs: {
                href: `/broadcast/round/${ctrl.id}/edit`,
                'data-icon': '',
              },
            }),
          ]),
          ctrl.data.sync?.url || ctrl.data.sync?.ids ? (ctrl.data.sync.ongoing ? stateOn : stateOff)(ctrl) : null,
          renderLog(ctrl),
        ]
      )
    : undefined;
}

function logSuccess(e: LogEvent) {
  return [e.moves ? h('strong', '' + e.moves) : e.moves, ` new move${e.moves > 1 ? 's' : ''}`];
}

function renderLog(ctrl: RelayCtrl) {
  const dateFormatter = getDateFormatter();
  const url = ctrl.data.sync?.url;
  const logLines = (ctrl.data.sync?.log || [])
    .slice(0)
    .reverse()
    .map(e => {
      const err =
        e.error &&
        h(
          'a',
          url
            ? {
                attrs: {
                  href: url,
                  target: '_blank',
                  rel: 'noopener nofollow',
                },
              }
            : {},
          e.error
        );
      return h(
        'div' + (err ? '.err' : ''),
        {
          key: e.at,
          attrs: dataIcon(err ? '' : ''),
        },
        [h('div', [...(err ? [err] : logSuccess(e)), h('time', dateFormatter(new Date(e.at)))])]
      );
    });
  if (ctrl.loading()) logLines.unshift(h('div.load', [h('i.ddloader'), 'Polling source...']));
  return h('div.log', logLines);
}

function stateOn(ctrl: RelayCtrl) {
  const url = ctrl.data.sync?.url;
  const ids = ctrl.data.sync?.ids;
  return h(
    'div.state.on.clickable',
    {
      hook: bind('click', _ => ctrl.setSync(false)),
      attrs: dataIcon(''),
    },
    [
      h(
        'div',
        url
          ? ['Connected to source', h('br'), url.replace(/https?:\/\//, '')]
          : ids
          ? ['Connected to', h('br'), ids.length, ' game(s)']
          : []
      ),
    ]
  );
}

function stateOff(ctrl: RelayCtrl) {
  return h(
    'div.state.off.clickable',
    {
      hook: bind('click', _ => ctrl.setSync(true)),
      attrs: dataIcon(''),
    },
    [h('div.fat', 'Click to connect')]
  );
}

let cachedDateFormatter: (date: Date) => string;

function getDateFormatter(): (date: Date) => string {
  if (!cachedDateFormatter)
    cachedDateFormatter =
      window.Intl && Intl.DateTimeFormat
        ? new Intl.DateTimeFormat(document.documentElement!.lang, {
            month: 'short',
            day: 'numeric',
            hour: 'numeric',
            minute: 'numeric',
            second: 'numeric',
          }).format
        : function (d) {
            return d.toLocaleString();
          };

  return cachedDateFormatter;
}
