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
  commentId: string;
  focusId?: string;
}

export class CommentForm {
  currents = new Map<string, Current>();
  opening = prop<string | null>(null);
  newComment = false;

  setnewComment = (newComment: boolean) => {
    this.newComment = newComment;
  };

  constructor(readonly root: AnalyseCtrl) {}

  makeKey(chapterId: string, path: Tree.Path, commentId: string): string {
    return `${chapterId}:${path}:${commentId}`;
  }

  submit = (key: string, el: HTMLInputElement) => {
    const current = this.currents.get(key);
    if (current) {
      this.currents.forEach((cur, _) => {
        cur.focusId = undefined;
      });
      this.currents.get(key)!.focusId = el.id;
      this.doSubmit(current.chapterId, current.path, current.commentId, el.value);
    }
  };

  doSubmit = throttle(500, (chapterId: string, path: Tree.Path, commentId: string, text: string) => {
    this.root.study!.makeChange('setComment', { ch: chapterId, path, id: commentId, text });
  });

  start = (chapterId: string, path: Tree.Path, node: Tree.Node, commentId: string): void => {
    const key = this.makeKey(chapterId, path, commentId);
    this.opening(key);
    this.currents.set(key, { chapterId, path, node, commentId });
    this.root.userJump(path);
  };

  onSetPath = (chapterId: string, path: Tree.Path, node: Tree.Node): void => {
    this.currents.forEach(cur => {
      if (path !== cur.path || chapterId !== cur.chapterId || cur.node !== node) {
        cur.chapterId = chapterId;
        cur.path = path;
        cur.node = node;
      }
    });
  };

  delete = (chapterId: string, path: Tree.Path, id: string) => {
    this.root.study!.makeChange('deleteComment', { ch: chapterId, path, id });
  };

  clear = () => {
    this.currents.clear();
  };
}

export const viewDisabled = (root: AnalyseCtrl, why: string): VNode =>
  h('div.study__comments', [currentComments(root, true), h('div.study__message', why)]);

function renderTextarea(root: AnalyseCtrl, ctrl: CommentForm, current: Current, key: string): VNode {
  const study = root.study!;
  const setupTextarea = (vnode: VNode, old?: VNode) => {
    const el = vnode.elm as HTMLTextAreaElement;
    if (old?.data!.key !== key) {
      const mine = (current.node.comments || []).find(function (c) {
        return (
          isAuthorObj(c.by) && c.by.id && c.by.id === ctrl.root.opts.userId && c.id === current.commentId
        );
      });
      el.value = mine ? mine.text : '';
    }
    vnode.data!.key = key;

    if (ctrl.opening() === key) {
      requestAnimationFrame(() => el.focus());
      ctrl.opening(null);
    }
  };

  return h(
    'div.study__comments',
    { hook: onInsert(() => root.enableWiki(root.data.game.variant.key === 'standard')) },
    [
      currentComments(root, !study.members.canContribute()),
      h('form.form3', [
        h(`textarea#${key}.form-control`, {
          hook: {
            insert(vnode) {
              setupTextarea(vnode);
              const el = vnode.elm as HTMLInputElement;
              el.oninput = () => setTimeout(() => ctrl.submit(key, el), 50);
              const heightStore = storage.make('study.comment.height');
              el.onmouseup = () => heightStore.set('' + el.offsetHeight);
              el.style.height = parseInt(heightStore.get() || '80') + 'px';

              $(el).on('keydown', e => {
                if (e.code === 'Escape') el.blur();
              });
            },
            postpatch: (old, vnode) => {
              setupTextarea(vnode, old);
              const el = vnode.elm as HTMLInputElement;
              if (ctrl.currents.get(key)?.focusId === el.id) el.focus();
              el.oninput = () => setTimeout(() => ctrl.submit(key, el), 50);
            },
          },
        }),
      ]),
    ],
  );
}

export function view(root: AnalyseCtrl): VNode {
  const study = root.study!;
  const commForm = study.commentForm;

  const commentKeys = new Set(
    (root.node.comments || []).map(c => commForm.makeKey(study.vm.chapterId, root.path, c.id)),
  );

  const rerender = Array.from(commForm.currents.keys()).some(key => !commentKeys.has(key));
  const comments = root.node.comments || [];
  if (rerender || commForm.currents.size === 0) {
    if (!commForm.newComment) commForm.clear();
    commForm.setnewComment(false);
    comments.forEach(comment => {
      commForm.start(study.vm.chapterId, root.path, root.node, comment.id);
    });
  }

  if (commForm.currents.size === 0) {
    return viewDisabled(root, 'Select a move to comment');
  }

  return h(
    'div.study__comments',
    {
      hook: onInsert(() => root.enableWiki(root.data.game.variant.key === 'standard')),
    },
    [
      h(
        'div.study__comment-forms',
        Array.from(commForm.currents.entries()).map(([key, current]) => {
          return renderTextarea(root, commForm, current, key);
        }),
      ),
      h('div.analyse__wiki.study__wiki.force-ltr'),
    ],
  );
}
