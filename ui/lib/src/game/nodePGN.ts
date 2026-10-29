import type { TreeNodeLite } from '@/tree/types';

import { fixCrazySan } from './chess';

export const plyPrefix = (node: TreeNodeLite): string =>
  `${Math.floor((node.ply + 1) / 2)}${node.ply % 2 === 1 ? '. ' : '... '}`;

export function renderNodesTxt(
  node: TreeNodeLite,
  forcePly: boolean,
  includeVariations = true,
  includeAnnotations = false,
): string {
  const first = node.children[0];
  if (!first || (!includeVariations && first.forceVariation)) return '';

  let s = '';
  if (forcePly || first.ply % 2 === 1) s += plyPrefix(first);
  s += fixCrazySan(first.san!);
  if (includeAnnotations) s += renderAnnotations(first);

  if (includeVariations) {
    for (let i = 1; i < node.children.length; i++) {
      const child = node.children[i];
      s += ` (${plyPrefix(child)}${fixCrazySan(child.san!)}`;
      if (includeAnnotations) s += renderAnnotations(child);
      const variation = renderNodesTxt(child, false, includeVariations, includeAnnotations);
      if (variation) s += ' ' + variation;
      s += ')';
    }
  }

  const mainline = renderNodesTxt(first, s.endsWith(')'), includeVariations, includeAnnotations);
  if (mainline) s += ' ' + mainline;

  return s;
}

function renderAnnotations(node: TreeNodeLite): string {
  const glyphs = node.glyphs?.map(glyph => glyph.symbol).join(' ') ?? '';
  const comments = node.comments?.map(comment => comment.text.replace(/[{}]/g, '')).filter(Boolean) ?? [];
  const evaluation = node.eval ?? node.ceval;
  if (evaluation?.mate !== undefined) comments.push(`[%eval #${evaluation.mate}]`);
  else if (evaluation?.cp !== undefined) comments.push(`[%eval ${evaluation.cp / 100}]`);
  if (node.clock !== undefined) {
    const seconds = Math.floor(node.clock / 100);
    comments.push(
      `[%clk ${Math.floor(seconds / 3600)}:${String(Math.floor((seconds % 3600) / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}]`,
    );
  }
  return glyphs + (comments.length ? ` { ${comments.join(' ')} }` : '');
}
