import { h } from 'snabbdom'
import AnalyseController from '../../../ctrl';
import { VNode } from 'snabbdom/vnode'
import { throttle } from 'common';

export default function(ctrl: AnalyseController): VNode {

  return h('div.gamebook', {
    hook: {
      insert: _ => {
        window.lichess.loadCss('/assets/stylesheets/gamebook.css')
        window.lichess.loadCss('/assets/stylesheets/material.form.css')
      }
    }
  }, [
    h('div.editor', [
      h('span.title', 'Gamebook editor'),
      renderDeviation(ctrl)
    ])
  ]);
}

let prevPath: Tree.Path;


const save = throttle(500, false, (ctrl: AnalyseController, gamebook: Tree.Gamebook) => {
  ctrl.socket.send('setGamebook', {
    path: ctrl.path,
    ch: ctrl.study!.vm.chapterId,
    gamebook: gamebook
  });
});

function renderDeviation(ctrl: AnalyseController): VNode {
  const node = ctrl.node,
  path = ctrl.path,
  gamebook: Tree.Gamebook = node.gamebook || {},
  deviation = gamebook.deviation || '';
  return h('div.deviation', [
    h('label', {
      attrs: { for: 'gamebook-deviation' }
    }, 'When any other move is played:'),
    h('textarea#gamebook-deviation', {
      hook: {
        insert(vnode: VNode) {
          const el = vnode.elm as HTMLInputElement;
          el.value = deviation;
          function onChange() {
            node.gamebook = node.gamebook || {};
            node.gamebook.deviation = el.value.trim();
            setTimeout(() => save(ctrl, node.gamebook, 50));
          }
          el.onkeyup = onChange;
          el.onpaste = onChange;
          prevPath = path;
        },
        postpatch(_, vnode: VNode) {
          if (prevPath !== path) {
            (vnode.elm as HTMLInputElement).value = deviation;
            prevPath = path;
          }
        }
      }
    })
  ]);
}
