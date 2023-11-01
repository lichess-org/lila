import { prop, Prop } from 'common';
import { onInsert } from 'common/snabbdom';
import throttle from 'common/throttle';
import { h, VNode } from 'snabbdom';
import AnalyseCtrl from '../ctrl';
import { currentComments, isAuthorObj } from './studyComments';

interface Current {
  chapterId: string;
  path: Tree.Path;
  node: Tree.Node;
}

export interface CommentForm {
  root: AnalyseCtrl;
  current: Prop<Current | null>;
  opening: Prop<boolean>;
  submit(text: string): void;
  start(chapterId: string, path: Tree.Path, node: Tree.Node): void;
  onSetPath(chapterId: string, path: Tree.Path, node: Tree.Node): void;
  redraw(): void;
  delete(chapterId: string, path: Tree.Path, id: string): void;
}

export function ctrl(root: AnalyseCtrl): CommentForm {
  const current = prop<Current | null>(null),
    opening = prop(false);

  function submit(text: string): void {
    if (!current()) return;
    doSubmit(text);
  }

  const doSubmit = throttle(500, (text: string) => {
    const cur = current();
    if (cur)
      root.study!.makeChange('setComment', {
        ch: cur.chapterId,
        path: cur.path,
        text,
      });
  });

  function start(chapterId: string, path: Tree.Path, node: Tree.Node): void {
    opening(true);
    current({
      chapterId,
      path,
      node,
    });
    root.userJump(path);
  }

  return {
    root,
    current,
    opening,
    submit,
    start,
    onSetPath(chapterId: string, path: Tree.Path, node: Tree.Node): void {
      const cur = current();
      if (cur && (path !== cur.path || chapterId !== cur.chapterId || cur.node !== node)) {
        cur.chapterId = chapterId;
        cur.path = path;
        cur.node = node;
      }
    },
    redraw: root.redraw,
    delete(chapterId: string, path: Tree.Path, id: string) {
      root.study!.makeChange('deleteComment', {
        ch: chapterId,
        path,
        id,
      });
    },
  };
}

export function viewDisabled(root: AnalyseCtrl, why: string): VNode {
  return h('div.study__comments', [currentComments(root, true), h('div.study__message', why)]);
}

export function view(root: AnalyseCtrl): VNode {
  const study = root.study!,
    ctrl = study.commentForm,
    current = ctrl.current();
  if (!current) return viewDisabled(root, 'Select a move to comment');
  const setupTextarea = (vnode: VNode, old?: VNode) => {
    const el = vnode.elm as HTMLInputElement;
    const newKey = current.chapterId + current.path;

    if (old?.data!.path !== newKey) {
      const mine = (current!.node.comments || []).find(function (c) {
        return isAuthorObj(c.by) && c.by.id && c.by.id === ctrl.root.opts.userId;
      });
      el.value = mine ? mine.text : '';
    }
    vnode.data!.path = newKey;

    if (ctrl.opening()) {
      requestAnimationFrame(() => el.focus());
      ctrl.opening(false);
    }
  };

  return h(
    'div.study__comments',
    {
      hook: onInsert(() => root.enableWiki(root.data.game.variant.key === 'standard')),
    },
    [
      currentComments(root, !study.members.canContribute()),
      h('form.form3', [
        h('textarea#comment-text.form-control', {
          hook: {
            insert(vnode) {
              setupTextarea(vnode);
              const el = vnode.elm as HTMLInputElement;
              el.oninput = () => setTimeout(() => ctrl.submit(el.value), 50);
              const heightStore = lichess.storage.make('study.comment.height');
              el.onmouseup = () => heightStore.set('' + el.offsetHeight);
              el.style.height = parseInt(heightStore.get() || '80') + 'px';

              $(el).on('keydown', e => {
                if (e.code === 'Escape') el.blur();
              });
            },
            postpatch: (old, vnode) => setupTextarea(vnode, old),
          },
        }),
      ]),
      h('div.analyse__wiki.study__wiki.force-ltr'),
    ],
  );
}
