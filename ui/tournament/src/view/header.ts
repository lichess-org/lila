import { h, Hooks, VNode } from 'snabbdom';
import * as licon from 'common/licon';
import { dataIcon } from 'common/snabbdom';
import TournamentController from '../ctrl';
import perfIcons from 'common/perfIcons';
import { TournamentData } from '../interfaces';

const startClock = (time: number): Hooks => ({
  insert: vnode => site.clockWidget(vnode.elm as HTMLElement, { time }),
});

const oneDayInSeconds = 60 * 60 * 24;

const hasFreq = (freq: 'shield' | 'marathon', d: TournamentData) => d.schedule?.freq === freq;

function clock(ctrl: TournamentController): VNode | undefined {
  const d = ctrl.data;
  if (d.isFinished) return;
  if (d.secondsToFinish) return h('div.clock', [h('div.time', { hook: startClock(d.secondsToFinish) })]);
  if (d.secondsToStart) {
    if (d.secondsToStart > oneDayInSeconds)
      return h('div.clock', [
        h('time.timeago.shy', {
          attrs: {
            title: new Date(d.startsAt).toLocaleString(),
            datetime: Date.now() + d.secondsToStart * 1000,
          },
          hook: {
            insert(vnode) {
              (vnode.elm as HTMLElement).setAttribute(
                'datetime',
                '' + (Date.now() + d.secondsToStart! * 1000),
              );
            },
          },
        }),
      ]);
    return h('div.clock.clock-created', [
      h('span.shy', ctrl.trans.noarg('startingIn')),
      h('span.time.text', { hook: startClock(d.secondsToStart) }),
    ]);
  }
  return undefined;
}

function image(d: TournamentData): VNode | undefined {
  if (d.isFinished) return;
  if (hasFreq('shield', d) || hasFreq('marathon', d)) return;
  const s = d.spotlight;
  if (s && s.iconImg) return h('img.img', { attrs: { src: site.asset.url('images/' + s.iconImg) } });
  return h('i.img', { attrs: dataIcon(s?.iconFont || licon.Trophy) });
}

function title(ctrl: TournamentController) {
  const d = ctrl.data;
  if (hasFreq('marathon', d)) return h('h1', [h('i.fire-trophy', licon.Globe), d.fullName]);
  if (hasFreq('shield', d))
    return h('h1', [
      h('a.shield-trophy', { attrs: { href: '/tournament/shields' } }, perfIcons[d.perf.key]),
      d.fullName,
    ]);
  return h(
    'h1',
    (d.greatPlayer
      ? [
          h(
            'a',
            { attrs: { href: d.greatPlayer.url, target: '_blank', rel: 'noopener' } },
            d.greatPlayer.name,
          ),
          ' Arena',
        ]
      : [d.fullName]
    ).concat(d.private ? [' ', h('span', { attrs: dataIcon(licon.Padlock) })] : []),
  );
}

export default function (ctrl: TournamentController): VNode {
  return h('div.tour__main__header', [image(ctrl.data), title(ctrl), clock(ctrl)]);
}
