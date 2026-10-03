import type { VNode, VNodeChildren } from 'snabbdom';

export type InputCommand = {
  cmd: string;
  help: string | VNodeChildren | VNodeChildren[];
  invalid?: (ctrl: any) => boolean;
};

/**
 * Recursively extract every text node from a snabbdom VNode tree,
 * joining them with a single space.
 */
function extractText(node: VNodeChildren | VNodeChildren[]): string {
  if (node === null || node === undefined) return '';
  if (typeof node === 'string' || typeof node === 'number') return String(node);
  if (Array.isArray(node))
    return node
      .map(extractText)
      .join(' ')
      .replace(/\s{2,}/g, ' ')
      .trim();
  // VNode
  const vnode = node as VNode;
  if (vnode.text !== undefined) return vnode.text;
  if (vnode.children) return extractText(vnode.children);
  return '';
}

export function buildInputHelpString(inputCommands: InputCommand[]): string {
  const cmds = inputCommands.map(c => {
    const help = typeof c.help === 'string' ? c.help : extractText(c.help);
    return `${c.cmd}: ${help}`;
  });

  return [cmds].join('. ');
}

/**
 * Build a plain-text help string from the `boardCommands()` VNode list.
 *
 * `boardCommands()` returns an array of VNodes (an <h2> heading and a <p>
 * containing the shortcut lines separated by <br>).  We extract all text
 * content from those nodes and normalise whitespace so the result reads as a
 * continuous, screenreader-friendly sentence list.
 */
export function buildBoardHelpString(nodes: VNode[]): string {
  // Collect every text fragment from the VNode tree.
  const raw = nodes.map(extractText).join(' ');

  // Collapse repeated whitespace that can appear around <br> boundaries.
  return raw.replace(/\s{2,}/g, ' ').trim();
}
