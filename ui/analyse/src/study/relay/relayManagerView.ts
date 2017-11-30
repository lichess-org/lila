import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import RelayCtrl from './relayCtrl';
import { iconTag, dataIcon, bind } from '../../util';
import { LogEvent } from './interfaces';

export default function(ctrl: RelayCtrl): VNode | undefined {
  const d = ctrl.data;
  if (ctrl.members.canContribute()) return h('div.relay_wrap', [
    h('h2', [
      h('span.text', { attrs: dataIcon('') }, 'Broadcast manager'),
      h('a', {
        attrs: {
          href: `/broadcast/${d.slug}/${d.id}/edit`,
          'data-icon': '%'
        }
      })
    ]),
    h('div.relay', [
      (d.sync.ongoing ? stateOn : stateOff)(ctrl),
      renderLog(ctrl)
    ])
  ]);
}

function logSuccess(e: LogEvent) {
  return [
    e.moves ? h('strong', '' + e.moves) : e.moves,
    ` new move${e.moves > 1 ? 's' : ''}`
  ];
}

function renderLog(ctrl: RelayCtrl) {
  const dateFormatter = getDateFormatter();
  const logLines = ctrl.data.sync.log.slice(0).reverse().map(e => {
    const err = e.error && h('a', {
      attrs: {
        href: ctrl.data.sync.url,
        target: '_blank'
      }
    }, e.error);
    return h('div' + (err ? '.err' : ''), {
      key: e.at
    }, [
      iconTag(err ? 'j' : 'E'),
      h('div', [
        ...(err ? [err] : logSuccess(e)),
        h('time', dateFormatter(new Date(e.at)))
      ])
    ]);
  });
  if (ctrl.loading()) logLines.unshift(h('div.load', [
    h('i.ddloader'),
    'Polling source...'
  ]));
  return h('div.log', logLines);
}

function stateOn(ctrl: RelayCtrl) {
  return h('div.state.on.clickable', {
    hook: bind('click', _ => ctrl.setSync(false))
  }, [
    iconTag('B'),
    h('div', [
      'Connected to source',
      h('br'),
      ctrl.data.sync.url.replace(/https?:\/\//, '')
    ])
  ]);
}

function stateOff(ctrl: RelayCtrl) {
  return h('div.state.off.clickable', {
    hook: bind('click', _ => ctrl.setSync(true))
  }, [
    iconTag('G'),
    h('div.fat', 'Click to connect')
  ]);
}

let cachedDateFormatter: (date: Date) => string;

function getDateFormatter(): (date: Date) => string {
  if (!cachedDateFormatter)
  cachedDateFormatter = (window.Intl && Intl.DateTimeFormat) ?
    new Intl.DateTimeFormat(document.documentElement.lang, {
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: 'numeric',
      second: 'numeric'
    }).format : function(d) { return d.toLocaleString(); }

    return cachedDateFormatter;
}
