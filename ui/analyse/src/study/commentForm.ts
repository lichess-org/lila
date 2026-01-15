import { prop } from 'lib';
import { onInsert } from 'lib/view';
import { throttle } from 'lib/async';
import { h, type VNode } from 'snabbdom';
import type AnalyseCtrl from '../ctrl';
import { currentComments, isAuthorObj } from './studyComments';
import { storage } from 'lib/storage';

interface Current {
  chapterId: string;
  path: Tree.Path;
  node: Tree.Node;
}

export class CommentForm {
  current = prop<Current | null>(null);
  opening = prop(false);
  constructor(readonly root: AnalyseCtrl) {}

  submit = (commentId: string, text: string) => this.current() && this.doSubmit(commentId, text);

  doSubmit = throttle(500, (commentId: string, text: string) => {
    const cur = this.current();
    if (cur)
      this.root.study!.makeChange('setComment', { ch: cur.chapterId, path: cur.path, text, id: commentId });
  });

  start = (chapterId: string, path: Tree.Path, node: Tree.Node): void => {
    this.opening(true);
    this.current({ chapterId, path, node });
    this.root.userJump(path);
  };

  onSetPath = (chapterId: string, path: Tree.Path, node: Tree.Node): void => {
    const cur = this.current();
    if (cur && (path !== cur.path || chapterId !== cur.chapterId || cur.node !== node)) {
      cur.chapterId = chapterId;
      cur.path = path;
      cur.node = node;
    }
  };
  delete = (chapterId: string, path: Tree.Path, id: string) => {
    this.root.study!.makeChange('deleteComment', { ch: chapterId, path, id });
  };
}

export const viewDisabled = (root: AnalyseCtrl, why: string): VNode =>
  h('div.study__comments', [currentComments(root, true), h('div.study__message', why)]);

export function view(root: AnalyseCtrl): VNode {
  const study = root.study!,
    ctrl = study.commentForm,
    current = ctrl.current();
  if (!current) return viewDisabled(root, 'Select a move to comment');

  const setupTextarea = (vnode: VNode, comment: Tree.Comment, old?: VNode) => {
    const el = vnode.elm as HTMLInputElement;
    const newKey = current.chapterId + current.path + (comment.id || '');

    if (old?.data!.path !== newKey) {
      const mine = (current.node.comments || []).find(
        c => isAuthorObj(c.by) && c.by.id && c.by.id === ctrl.root.opts.userId && c.id === comment.id,
      );
      el.value = mine?.text || '';
    }
    vnode.data!.path = newKey;

    if (ctrl.opening()) {
      requestAnimationFrame(() => el.focus());
      ctrl.opening(false);
    }
  };

  const commentTextareas = () => {
    let comments = current.node.comments || [];
    comments = comments.filter(comment => isAuthorObj(comment.by) && comment.by.id === ctrl.root.opts.userId);
    if (comments.length === 0) comments = [{ id: '', by: '', text: '' }];

    return comments.map(comment =>
      h('div.study__comment-edit', [
        h('textarea.form-control', {
          key: comment.id,
          props: { value: comment.text },
          hook: {
            insert(vnode) {
              setupTextarea(vnode, comment);
              const el = vnode.elm as HTMLInputElement;
              el.oninput = () => setTimeout(() => ctrl.submit(comment.id, el.value), 50);
              const heightStore = storage.make('study.comment.height.' + comment.id);
              el.onmouseup = () => heightStore.set('' + el.offsetHeight);
              el.style.height = parseInt(heightStore.get() || '80') + 'px';

              $(el).on('keydown', e => {
                if (e.code === 'Escape') el.blur();
              });
            },
            postpatch: (old, vnode) => setupTextarea(vnode, comment, old),
          },
        }),
      ]),
    );
  };

  return h(
    'div.study__comments',
    { hook: onInsert(() => root.enableWiki(root.data.game.variant.key === 'standard')) },
    [
      currentComments(root, !study.members.canContribute()),
      h('form.form3', commentTextareas()),
      h('div.analyse__wiki.study__wiki.force-ltr'),
    ],
  );
}
