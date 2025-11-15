import { describe } from 'node:test';
import assert from 'node:assert/strict';
import { enhance, imgurRegex } from '../src/view/enhance';
import { each } from '../../.test/helpers.mts';

describe('test imgur matching', () => {
  each<[string, string]>([
    ['https://i.imgur.com/HFn4lkh.jpeg', 'https://i.imgur.com/HFn4lkh.jpg'],
    ['http://imgur.com/HFn4lkh.png', 'https://i.imgur.com/HFn4lkh.jpg'],
  ])('embed individual images', (input, link) => {
    assert.strictEqual(imgurRegex.test(input), true);
    assert.strictEqual(
      enhance(input),
      `<a target="_blank" rel="nofollow noreferrer" href="${link}"><img src="${link}"/></a>`,
    );
  });

  each<[string, string]>([
    ['https://imgur.com/a/this-inUquB9', 'imgur.com/a/this-inUquB9'],
    [
      'https://imgur.com/gallery/lichess-is-down-404-image-HFn4lkh',
      'imgur.com/gallery/lichess-is-down-404-image-HFn4lkh',
    ],
  ])('albums and galleries should not be embedded', (input, link) => {
    assert.strictEqual(imgurRegex.test(input), false);
    assert.strictEqual(
      enhance(input),
      `<a target="_blank" rel="nofollow noreferrer" href="${input}">${link}</a>`,
    );
  });
});

describe('test bulk message ids should have a text class', () => {
  each<[string]>([['Your game with @somebody is ready: #gameIdXX.']])('should have a text class', input => {
    assert.strictEqual(
      enhance(input),
      'Your game with <a target="_blank" rel="nofollow noreferrer" href="/@/somebody">@somebody</a> is ready: ' +
        '<a class="text" target="_blank" rel="nofollow noreferrer" href="/gameIdXX">#gameIdXX</a>.',
    );
  });
});

describe('test regular game links should not have a text class', () => {
  each<[string]>([['I played a game: https://lichess.org/GameIdXX']])(
    'should not have a text class',
    input => {
      assert.strictEqual(
        enhance(input),
        'I played a game: <a target="_blank" rel="nofollow noreferrer" href="https://lichess.org/GameIdXX">lichess.org/GameIdXX</a>',
      );
    },
  );
});
