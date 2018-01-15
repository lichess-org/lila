import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { currentComments } from './studyComments';
import { nodeFullName, bind } from '../util';
import { prop, throttle, Prop } from 'common';
import AnalyseCtrl from '../ctrl';

interface Current {
  chapterId: string;
  path: Tree.Path;
  node: Tree.Node;
}

interface CommentForm {
  root: AnalyseCtrl;
  current: Prop<Current | null>;
  focus: Prop<boolean>;
  opening: Prop<boolean>;
  set(chapterId: string, path: Tree.Path, node: Tree.Node): void;
  submit(text: string): void;
  onSetPath(path: Tree.Path, node: Tree.Node, playedMyself: boolean): void;
  redraw(): void;
  set(chapterId: string, path: Tree.Path, node: Tree.Node): void;
  delete(chapterId: string, path: Tree.Path, id: string): void;
}

export function ctrl(root: AnalyseCtrl): CommentForm {

  const current = prop<Current | null>(null),
  focus = prop(false),
  opening = prop(false);

  function submit(text: string): void {
    if (!current()) return;
    doSubmit(text);
  };

  const doSubmit = throttle(500, (text: string) => {
    const cur = current();
    if (cur) root.study!.makeChange('setComment', {
      ch: cur.chapterId,
      path: cur.path,
      text
    });
  });

  function set(chapterId: string, path: Tree.Path, node: Tree.Node): void {
    opening(true);
    current({
      chapterId,
      path,
      node
    });
    root.userJump(path);
  };

  return {
    root,
    current,
    focus,
    opening,
    set,
    submit,
    onSetPath(path: Tree.Path, node: Tree.Node, playedMyself: boolean): void {
      setTimeout(() => {
        const cur = current();
        if (cur && cur.path !== path && (!focus() || playedMyself)) {
          cur.path = path;
          cur.node = node;
          root.redraw();
        }
      }, 100);
    },
    redraw: root.redraw,
    delete(chapterId: string, path: Tree.Path, id: string) {
      root.study!.makeChange('deleteComment', {
        ch: chapterId,
        path,
        id
      });
    }
  };
}

export function viewDisabled(root: AnalyseCtrl, why: string): VNode {
  return h('div.study_comment_form', [
    currentComments(root, true),
    h('div.message', h('span', why))
  ]);
}

export function view(root: AnalyseCtrl): VNode {

  const study = root.study!, ctrl = study.commentForm, current = ctrl.current();
  if (!current) return viewDisabled(root, 'Select a move to comment');

  function setupTextarea(vnode: VNode) {
    const el = vnode.elm as HTMLInputElement,
    mine = (current!.node.comments || []).find(function(c: any) {
      return c.by && c.by.id && c.by.id === ctrl.root.opts.userId;
    });
    el.value = mine ? mine.text : '';
    if (ctrl.opening() || ctrl.focus()) window.lichess.raf(() => el.focus());
    ctrl.opening(false);
  }

  return h('div.study_comment_form.underboard_form', {
    hook: {
      insert: _ => window.lichess.loadCss('/assets/stylesheets/material.form.css')
    }
  }, [
    currentComments(root, !study.members.canContribute()),
    ctrl.focus() && ctrl.root.path !== current.path ? h('p.title', [
      'Commenting position after ',
      h('button.button', {
        hook: bind('mousedown', () => ctrl.root.userJump(current.path), ctrl.redraw)
      }, nodeFullName(current.node))
    ]) : null,
    h('form.material.form', [
      h('div.form-group', [
        h('textarea#comment-text', {
          hook: {
            insert(vnode) {
              setupTextarea(vnode);
              const el = vnode.elm as HTMLInputElement;
              function onChange() {
                setTimeout(function() {
                  ctrl.submit(el.value);
                }, 50);
              }
              el.onkeyup = el.onpaste = onChange;
              el.onfocus = function() {
                ctrl.focus(true);
                ctrl.redraw();
              };
              el.onblur = function() {
                ctrl.focus(false);
                ctrl.redraw();
              };
              vnode.data!.path = current.path;
            },
            postpatch(old, vnode) {
              if (old.data!.path !== current.path) setupTextarea(vnode);
              vnode.data!.path = current.path;
            }
          }
        }),
        h('i.bar')
      ])
    ])
  ]);
}
