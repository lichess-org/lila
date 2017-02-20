import { h } from 'snabbdom'
import { Ctrl, Line } from './interfaces'
import { skip } from './spam'

export function renderDiscussion(ctrl: Ctrl) {
  if (!ctrl.vm.enabled) [];
  return [
    h('ol.messages.content.scroll-shadow-soft', {
      hook: {
        insert: (vnode) => {
          $(vnode.elm as HTMLElement).on('click', 'a.jump', (e: Event) => {
            window.lichess.pubsub.emit('jump')((e.target as HTMLElement).getAttribute('data-ply'));
          })
        },
        postpatch: (_, vnode) => {
          const el = vnode.elm as HTMLElement
          if (ctrl.data.lines.length > 5) {
            const autoScroll = (el.scrollTop === 0 || (el.scrollTop > (el.scrollHeight - el.clientHeight - 100)));
            el.scrollTop = 999999
            if (autoScroll) setTimeout(() => el.scrollTop = 999999, 500)
          }
        }
      }
    }, selectLines(ctrl).map(line => renderLine(ctrl, line)))
  ]
  //   ),
  //   input(ctrl),
  //   presetView(ctrl.preset)
  // ];
}

function sameLines(l1: Line, l2: Line) {
  return l1.d && l2.d && l1.u === l2.u;
}

function selectLines(ctrl: Ctrl): Array<Line> {
  let prev: Line, ls: Array<Line> = [];
  ctrl.data.lines.forEach(function(line) {
    if (!line.d &&
        (!prev || !sameLines(prev, line)) &&
          (!line.r || ctrl.opts.kobold) &&
            !skip(line.t)
       ) ls.push(line);
     prev = line;
  });
  return ls;
}

function renderLine(ctrl: Ctrl, line: Line) {
  return h('li', line.t);
  // if (!line.html) line.html = enhance(line.t, {
  //   parseMoves: ctrl.vm.parseMoves
  // });
  // if (line.u === 'lichess') return m('li.system', line.html);
  // if (line.c) return m('li', [
  //   m('span', '[' + line.c + ']'),
  //   line.t
  // ]);
  // return m('li', {
  //   'data-username': line.u
  // }, [
  //   ctrl.vm.isMod ? moderationView.lineAction() : null,
  //   m.trust(
  //     $.userLinkLimit(line.u, 14) + line.html
  //   )
  // ]);
}
