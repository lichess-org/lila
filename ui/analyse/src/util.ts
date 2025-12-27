export const plyColor = (ply: number): Color => (ply % 2 === 0 ? 'white' : 'black');

export function readOnlyProp<A>(value: A): () => A {
  return function (): A {
    return value;
  };
}

const ensureChildren = (node: Tree.NodeBase): Tree.Node => {
  node.children ||= [];
  return node as Tree.Node;
};

export function treeReconstruct(parts: Tree.NodeBase[], sidelines?: Tree.Node[][]): Tree.Node {
  const root = ensureChildren(parts[0]);
  root.id = '';
  let node = root;
  for (let i = 1; i < parts.length; i++) {
    const n = ensureChildren(parts[i]);
    const variations = sidelines ? sidelines[i] : [];
    node.children.unshift(n, ...variations);
    node = n;
  }
  return root;
}

export function mergeEval(into: Tree.Node, from: Tree.Node, isStudy: boolean): void {
  into.children = into.children.filter(hasUserData);
  into.eval = from.eval;

  const filteredComments = into.comments?.filter(c => String(c.by).toLowerCase() !== 'lichess');
  const filteredGlyphs = into.glyphs?.filter(g => ![2, 4, 6].includes(g.id));

  if (into.comments?.length !== filteredComments?.length) into.glyphs = filteredGlyphs; // cross fingers
  into.comments = filteredComments;

  const lastEngineComment = from.comments
    ?.slice()
    .reverse()
    .find(c => String(c.by).toLowerCase() === 'lichess');
  if (lastEngineComment) {
    into.comments ??= [];
    into.comments.push(lastEngineComment);
  }
  const lastEngineGlyph = from.glyphs
    ?.slice()
    .reverse()
    .find(g => [2, 4, 6].includes(g.id));
  if (lastEngineGlyph) {
    into.glyphs ??= [];
    into.glyphs.push(lastEngineGlyph);
  }

  for (const fromChild of from.children) {
    const intoChild = into.children.find(n => fromChild.id === n.id);
    if (intoChild) mergeEval(intoChild, fromChild, isStudy);
    else into.children.push(fromChild);
  }

  function hasUserData(node: Tree.Node): boolean {
    return (
      !node.comp ||
      node.comments?.some(c => String(c.by).toLowerCase() !== 'lichess') ||
      // no need to recurse ordinary game replays. only the first engine node is marked anyways
      (isStudy && node.children.some(hasUserData))
    );
  }
}
