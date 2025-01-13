import { isEmpty } from 'common/common';
import { enrichText, innerHTML } from 'common/rich-text';
import type { MaybeVNodes } from 'common/snabbdom';
import { plyColor } from 'shogi/common';
import { usiToNotation } from 'shogi/notation';
import { h } from 'snabbdom';

export function renderMainlineCommentsOf(
  node: Tree.Node,
  parentNode: Tree.Node | undefined,
  variant: VariantKey,
  withColor = true,
): MaybeVNodes {
  if (isEmpty(node.comments)) return [];

  const withAuthor = node.comments!.some(c => c.by !== node.comments![0].by);
  const color = withColor ? `.${plyColor(node.ply)}` : '';

  return node.comments!.map(comment => {
    let sel = `comment${color}`;
    if (comment.text.startsWith('Inaccuracy.')) sel += '.inaccuracy';
    else if (comment.text.startsWith('Mistake.')) sel += '.mistake';
    else if (comment.text.startsWith('Blunder.')) sel += '.blunder';
    const by = withAuthor ? `<span class="by">${authorText(comment.by)}</span>` : '';
    const truncated = truncateComment(comment.text, 200);
    return h(sel, {
      hook: innerHTML(by + truncated, text => {
        const s = text.split('</span>');
        return by + enrichText(usiToNotation(node, parentNode, variant, s[s.length - 1]));
      }),
    });
  });
}

export function renderInlineCommentsOf(
  node: Tree.Node,
  parentNode: Tree.Node | undefined,
  variant: VariantKey,
): MaybeVNodes {
  if (isEmpty(node.comments)) return [];
  return node
    .comments!.map(comment => {
      const by = node.comments![1] ? `<span class="by">${authorText(comment.by)}</span>` : '';
      const truncated = truncateComment(comment.text, 150);
      return h('comment', {
        hook: innerHTML(
          truncated,
          text => by + enrichText(usiToNotation(node, parentNode, variant, text)),
        ),
      });
    })
    .filter(c => !!c);
}

function authorText(author: Tree.CommentAuthor): string {
  if (!author) return '';
  if (typeof author === 'string') return `[${author}]`;
  else return `[${author.name}]`;
}

function truncateComment(text: string, len: number): string {
  return text.length > len ? `${text.slice(0, len - 10)} [...]` : text;
}
