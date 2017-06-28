import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { nodeFullName, bind } from '../util';
import { prop, throttle } from 'common';
import AnalyseController from '../ctrl';

interface Current {
  chapterId: string;
  path: Tree.Path;
  node: Tree.Node;
}

export function ctrl(root: AnalyseController) {

  const current = prop<Current | null>(null);
  const dirty = prop(true);
  const focus = prop(false);
  const opening = prop(false);

  function submit(text) {
    if (!current()) return;
    if (!dirty()) {
      dirty(true);
      root.redraw();
    }
    doSubmit(text);
  };

  const doSubmit = throttle(500, false, function(text) {
    const cur = current();
    if (cur) root.study!.makeChange('setComment', {
      ch: cur.chapterId,
      path: cur.path,
      text: text
    });
  });

  function open(chapterId: string, path: Tree.Path, node: Tree.Node) {
    dirty(true);
    opening(true);
    current({
      chapterId: chapterId,
      path: path,
      node: node
    });
    root.userJump(path);
  };

  function close() {
    current(null);
  };

  function onSetPath(path: Tree.Path, node: Tree.Node) {
    const cur = current();
    if (cur && cur.path !== path && !focus()) {
      cur.path = path;
      cur.node = node;
      current(cur);
      dirty(true);
    }
    root.redraw();
  };

  return {
    root,
    current,
    dirty,
    focus,
    opening,
    open,
    close,
    submit,
    onSetPath,
    redraw: root.redraw,
    toggle(chapterId: string, path: Tree.Path, node: Tree.Node) {
      if (current()) close();
      else open(chapterId, path, node);
    },
    delete(chapterId: string, path: Tree.Path, id: string) {
      root.study.makeChange('deleteComment', {
        ch: chapterId,
        path: path,
        id: id
      });
    }
  };
}

export function view(ctrl) {

  const current = ctrl.current();
  if (!current) return;

  function setupTextarea(vnode: VNode) {
    const el = vnode.elm as HTMLInputElement;
    const vData = vnode.data!;
    vData!.path = current.path;
    const mine = (current.node.comments || []).find(function(c) {
      return c.by && c.by.id && c.by.id === ctrl.root.userId;
    });
    el.value = mine ? mine.text : '';
    if (ctrl.opening() || ctrl.focus()) el.focus();
    ctrl.opening(false);
    if (!vData!.trap) {
      vData.trap = window.Mousetrap(el);
      vData.trap.bind(['ctrl+enter', 'command+enter'], ctrl.close);
      vData.trap.stopCallback = () => false;
    }
  }

  return h('div.study_comment_form.underboard_form', {
    hook: {
      insert: _ => window.lichess.loadCss('/assets/stylesheets/material.form.css')
    }
  }, [
    h('p.title', [
      h('button.button.frameless.close', {
        attrs: {
          'data-icon': 'L',
          title: 'Close'
        },
        hook: bind('click', ctrl.close, ctrl.redraw)
      }),
      'Commenting position after ',
      h('button.button', {
        class: { active: ctrl.root.vm.path === current.path },
        hook: bind('click', _ => ctrl.root.userJump(current.path), ctrl.redraw)
      }, nodeFullName(current.node)),
      h('span.saved', {
        hook: {
          postpatch: (_, vnode) => {
            const el = vnode.elm as HTMLElement;
            if (ctrl.dirty()) el.classList.remove('visible');
            else el.classList.add('visible');
          }
        }
      }, 'Saved.')
    ]),
    h('form.material.form', [
      h('div.form-group', [
        h('textarea#comment-text', {
          hook: {
            insert: vnode => {
              setupTextarea(vnode);
              const el = vnode.elm as HTMLInputElement;
              function onChange() {
                setTimeout(function() {
                  ctrl.submit(el.value);
                  ctrl.redraw();
                }, 50);
              }
              el.onkeyup = onChange;
              el.onpaste = onChange;
              el.onfocus = function() {
                ctrl.focus(true);
                ctrl.redraw();
              };
              el.onblur = function() {
                ctrl.focus(false);
                ctrl.redraw();
              };
            },
            postpatch: (_, vnode) => {
              if (vnode.data!.path !== current.path) setupTextarea(vnode);
            },
            destroy: vnode => {
              if (vnode.data!.trap) vnode.data!.trap.reset();
            }
          }
        }),
        h('i.bar')
      ])
    ])
  ]);
}
