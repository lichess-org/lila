import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import SwissCtrl from '../ctrl';
import { dataIcon } from './util';

function startClock(time) {
  return {
    insert: vnode => $(vnode.elm as HTMLElement).clock({ time: time })
  };
}

const oneDayInSeconds = 60 * 60 * 24;

function clock(ctrl: SwissCtrl): VNode | undefined {
  const seconds = ctrl.data.secondsToNextRound;
  if (!seconds) return;
  if (seconds > oneDayInSeconds) return h('div.clock', [
    h('time.timeago.shy', {
      attrs: {
        datetime: Date.now() + seconds * 1000
      },
      hook: {
        insert(vnode) {
          (vnode.elm as HTMLElement).setAttribute('datetime', '' + (Date.now() + seconds * 1000));
        }
      }
    })
  ]);
  return h('div.clock.clock-created', {
    hook: startClock(seconds)
  }, [
    h('span.shy', ctrl.data.status == 'created' ? 'Starting in' : 'Next round'),
    h('span.time.text')
  ]);
}

export default function(ctrl: SwissCtrl): VNode {
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
