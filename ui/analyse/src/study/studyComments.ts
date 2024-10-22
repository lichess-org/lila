import { h, VNode } from 'snabbdom';
import * as licon from 'common/licon';
import { bind } from 'common/snabbdom';
import { richHTML } from 'common/richText';
import AnalyseCtrl from '../ctrl';
import { nodeFullName } from '../view/util';
import StudyCtrl from './studyCtrl';

export type AuthorObj = {
  id: string;
  name: string; // contains the title
};
export type Author = AuthorObj | string;

function authorDom(author: Author): string | VNode {
  if (!author) return 'Unknown';
  if (typeof author === 'string') return author;
  return h('span.user-link.ulpt', { attrs: { 'data-href': '/@/' + author.id } }, author.name);
}

export const isAuthorObj = (author: Author): author is AuthorObj => typeof author === 'object';

export const authorText = (author?: Author): string =>
  !author ? 'Unknown' : typeof author === 'string' ? author : author.name;

export function currentComments(ctrl: AnalyseCtrl, includingMine: boolean): VNode | undefined {
  if (!ctrl.node.comments) return;
  const node = ctrl.node,
    study: StudyCtrl = ctrl.study!,
    chapter = study.currentChapter(),
    comments = node.comments!;
  if (!comments.length) return;
  return h(
    'div',
    comments.map(comment => {
      const by: Author = comment.by;
      const isMine = isAuthorObj(by) && by.id === ctrl.opts.userId;
      if (!includingMine && isMine) return;
      return h('div.study__comment.' + comment.id, [
        study.members.canContribute() && study.vm.mode.write
          ? h('a.edit', {
              attrs: { 'data-icon': licon.Trash, title: 'Delete' },
              hook: bind(
                'click',
                () => {
                  if (confirm('Delete ' + authorText(by) + "'s comment?"))
                    study.commentForm.delete(chapter.id, ctrl.path, comment.id);
                },
                ctrl.redraw,
              ),
            })
          : null,
        authorDom(by),
        ...(node.san ? [' on ', h('span.node', nodeFullName(node))] : []),
        ': ',
        h('div.text', { hook: richHTML(comment.text) }),
      ]);
    }),
  );
}
