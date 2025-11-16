export const plyColor = (ply: number): Color => (ply % 2 === 0 ? 'white' : 'black');

export function readOnlyProp<A>(value: A): () => A {
  return function (): A {
    return value;
  };
}

export function treeReconstruct(parts: Tree.Node[], sidelines?: Tree.Node[][]): Tree.Node {
  const root = parts[0],
    nb = parts.length;
  let node = root;
  root.id = '';
  for (let i = 1; i < nb; i++) {
    const n = parts[i];
    const variations = sidelines ? sidelines[i] : [];
    if (node.children) node.children.unshift(n, ...variations);
    else node.children = [n, ...variations];
    node = n;
  }
  node.children = node.children || [];
  return root;
}

export const acceptableEloPattern = '\\d{3,4}';

export const isAcceptableElo = (value: string): boolean =>
  new RegExp(`^${acceptableEloPattern}$`).test(value);

export const acceptableFideIdPattern = '\\d{2,9}';

export const isAcceptableFideId = (value: string): boolean =>
  new RegExp(`^${acceptableFideIdPattern}$`).test(value);

// Set of titles derived from scalachess' PlayerTitle.scala.
const titles = 'GM|WGM|IM|WIM|FM|WFM|CM|WCM|NM|WNM|LM|BOT';

export const acceptableTitlePattern = `${titles}|${titles.toLowerCase()}`;

export const isAcceptableTitle = (value: string): boolean =>
  new RegExp(`^${acceptableTitlePattern}$`).test(value);
