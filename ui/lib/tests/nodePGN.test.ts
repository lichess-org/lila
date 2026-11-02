import assert from 'node:assert/strict';
import { describe, test } from 'node:test';

import { renderNodesTxt } from '../src/game/nodePGN';
import type { TreeNode } from '../src/tree/types';

const node = (ply: Ply, san: San, ...children: TreeNode[]): TreeNode =>
  ({ id: san.slice(0, 2), ply, san, children }) as TreeNode;

const root = {
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
} as TreeNode;

describe('renderNodesTxt', () => {
  test('includes variations by default', () => {
    assert.equal(renderNodesTxt(root, true), '1. e4 e5 (1... c5 2. Nf3 (2. d4)) 2. Nf3 Nc6');
  });

  test('omits variations when asked to', () => {
    assert.equal(renderNodesTxt(root, true, false), '1. e4 e5 2. Nf3 Nc6');
  });

  test('stops before a forced variation only when omitting variations', () => {
    const forcedRoot = {
      id: '',
      ply: 0,
      children: [node(1, 'e4', { ...node(2, 'e5', node(3, 'Nf3')), forceVariation: true })],
    } as TreeNode;
    assert.equal(renderNodesTxt(forcedRoot, true, false), '1. e4');
    assert.equal(renderNodesTxt(forcedRoot, true), '1. e4 e5 2. Nf3');
  });

  test('numbers a black move only when a variation precedes it', () => {
    const branchingRoot = {
      id: '',
      ply: 0,
      children: [node(1, 'e4', node(2, 'e5')), node(1, 'd4')],
    } as TreeNode;
    assert.equal(renderNodesTxt(branchingRoot, true), '1. e4 (1. d4) 1... e5');
    assert.equal(renderNodesTxt(branchingRoot, true, false), '1. e4 e5');
  });

  test('returns nothing for a leaf', () => {
    assert.equal(renderNodesTxt(node(1, 'e4'), true), '');
  });
});
