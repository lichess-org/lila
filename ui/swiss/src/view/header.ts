import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import SwissController from '../ctrl';
import { dataIcon } from './util';

function startClock(time) {
  return {
    insert: vnode => $(vnode.elm as HTMLElement).clock({ time: time })
  };
}

const oneDayInSeconds = 60 * 60 * 24;

function clock(ctrl: SwissController): VNode | undefined {
  const d = ctrl.data;
  if (ctrl.isFinished()) return;
  if (d.secondsToStart) {
    if (d.secondsToStart > oneDayInSeconds) return h('div.clock', [
      h('time.timeago.shy', {
        attrs: {
          title: new Date(d.startsAt).toLocaleString(),
          datetime: Date.now() + d.secondsToStart * 1000
        },
        hook: {
          insert(vnode) {
            (vnode.elm as HTMLElement).setAttribute('datetime', '' + (Date.now() + d.secondsToStart * 1000));
          }
        }
      })
    ]);
    return h('div.clock.clock-created', {
      hook: startClock(d.secondsToStart)
    }, [
      h('span.shy', 'Starting in'),
      h('span.time.text')
    ]);
  }
}

export default function(ctrl: SwissController): VNode {
  const greatPlayer = ctrl.data.greatPlayer;
  return h('div.swiss__main__header', [
    h('i.img', dataIcon('g')),
    h('h1',
      (greatPlayer ? [
        h('a', {
          attrs: {
            href: greatPlayer.url,
            target: '_blank'
          }
        }, greatPlayer.name),
        ' Tournament'
      ] : [ctrl.data.name])
    ),
    clock(ctrl)
  ]);
}
