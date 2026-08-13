import assert from 'node:assert/strict';
import { describe, test } from 'node:test';

import type { TreeNode, TreePath } from 'lib/tree/types';

import type { Game } from '../src/interfaces';
import { renderNodesPgn } from '../src/pgnExport';

const game = { variant: { key: 'standard', name: 'Standard' } } as Game;

const node = (ply: Ply, san: San, ...children: TreeNode[]): TreeNode =>
  ({ id: san.slice(0, 2), ply, san, children }) as TreeNode;

const forced = (n: TreeNode): TreeNode => ({ ...n, forceVariation: true });

const makeTree = (): TreeNode =>
  ({
    id: '',
    ply: 0,
    children: [
      node(
        1,
        'e4',
        node(2, 'e5', node(3, 'Nf3', node(4, 'Nc6'))),
        node(2, 'c5', node(3, 'Nf3'), node(3, 'd4')),
      ),
    ],
  }) as TreeNode;

const makeForcedTree = (): TreeNode =>
  ({
    id: '',
    ply: 0,
    children: [node(1, 'e4', node(2, 'e5', forced(node(3, 'Nf3', node(4, 'Nc6')))))],
  }) as TreeNode;

function nodeList(root: TreeNode, path: TreePath): TreeNode[] {
  const nodes = [root];
  for (let n = root; path; path = path.slice(2)) {
    n = n.children.find(c => c.id === path.slice(0, 2))!;
    nodes.push(n);
  }
  return nodes;
}

describe('renderNodesPgn', () => {
  const root = makeTree();
  const render = (path: TreePath, includeSubVariations: boolean) =>
    renderNodesPgn(game, nodeList(root, path), includeSubVariations).trim();

  test('renders the main line without variations', () => {
    assert.equal(render('e4e5', false), '1. e4 e5 2. Nf3 Nc6');
  });

  test('appends the sub-variations branching off the last node', () => {
    assert.equal(render('e4', true), '1. e4 e5 (1... c5 2. Nf3 (2. d4)) 2. Nf3 Nc6');
    assert.equal(render('e4c5', true), '1. e4 c5 2. Nf3 (2. d4)');
  });

  test('leaves the line leading up to the last node linear', () => {
    assert.equal(render('e4e5', true), '1. e4 e5 2. Nf3 Nc6');
    assert.equal(render('e4c5Nf', true), '1. e4 c5 2. Nf3');
  });

  test('renders the whole main line for an empty path', () => {
    assert.equal(render('', false), '1. e4 e5 2. Nf3 Nc6');
  });

  test('renders nothing when there are no moves', () => {
    const empty = { id: '', ply: 0, children: [] } as unknown as TreeNode;
    const fromFen = { ...game, initialFen: '4k3/8/8/8/8/8/8/4K3 w - - 0 1' } as Game;
    assert.equal(renderNodesPgn(fromFen, [empty], false), '');
    assert.equal(renderNodesPgn(fromFen, [empty], true), '');
  });

  test('renders the game tags', () => {
    const crazyhouse = { variant: { key: 'crazyhouse', name: 'Crazyhouse' } } as Game;
    assert.equal(
      renderNodesPgn(crazyhouse, nodeList(root, 'e4'), false).trim(),
      '[Variant "Crazyhouse"]\n\n1. e4 e5 2. Nf3 Nc6',
    );
  });

  describe('black to move from the initial fen', () => {
    const fen = 'rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1';
    const fromFen = { ...game, initialFen: fen } as Game;
    const blackFirst = { id: '', ply: 1, children: [node(2, 'e5', node(3, 'Nf3'))] } as TreeNode;

    test('numbers the first move when the path contributes none', () => {
      assert.equal(
        renderNodesPgn(fromFen, nodeList(blackFirst, ''), false).trim(),
        `[FEN "${fen}"]\n\n1... e5 2. Nf3`,
      );
    });

    test('the empty path and the full path render the same moves', () => {
      const render = (path: TreePath) => renderNodesPgn(game, nodeList(blackFirst, path), false).trim();
      assert.equal(render(''), render('e5'));
    });
  });

  describe('forced variations', () => {
    const forcedRoot = makeForcedTree();
    const renderForced = (path: TreePath, includeSubVariations: boolean) =>
      renderNodesPgn(game, nodeList(forcedRoot, path), includeSubVariations).trim();

    test('the main line stops before a forced variation', () => {
      assert.equal(renderForced('e4', false), '1. e4 e5');
      assert.equal(renderForced('', false), '1. e4 e5');
    });

    test('a variation copy runs through a forced variation', () => {
      assert.equal(renderForced('e4e5Nf', true), '1. e4 e5 2. Nf3 Nc6');
    });
  });
});
