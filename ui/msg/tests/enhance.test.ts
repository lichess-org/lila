import { describe, expect, test } from 'vitest';
import { enhance, imgurRegex } from '../src/view/enhance';

describe('test imgur matching', () => {
  test.each([
    ['https://i.imgur.com/HFn4lkh.jpeg', 'https://i.imgur.com/HFn4lkh.jpg'],
    ['http://imgur.com/HFn4lkh.png', 'https://i.imgur.com/HFn4lkh.jpg'],
  ])('embed individual images', (input, link) => {
    expect(imgurRegex.test(input)).toBe(true);
    expect(enhance(input)).toBe(
      `<a target="_blank" rel="nofollow noreferrer" href="${link}"><img src="${link}"/></a>`,
    );
  });

  test.each([
    ['https://imgur.com/a/this-inUquB9', 'imgur.com/a/this-inUquB9'],
    [
      'https://imgur.com/gallery/lichess-is-down-404-image-HFn4lkh',
      'imgur.com/gallery/lichess-is-down-404-image-HFn4lkh',
    ],
  ])('albums and galleries should not be embedded', (input, link) => {
    expect(imgurRegex.test(input)).toBe(false);
    expect(enhance(input)).toBe(`<a target="_blank" rel="nofollow noreferrer" href="${input}">${link}</a>`);
  });
});

describe('test bulk message ids should have a text class', () =>
  test.each(['Your game with @somebody is ready: #gameIdXX.'])('should have a text class', input => {
    expect(enhance(input)).toBe(
      'Your game with <a target="_blank" rel="nofollow noreferrer" href="/@/somebody">@somebody</a> is ready: ' +
        '<a class="text" target="_blank" rel="nofollow noreferrer" href="/gameIdXX">#gameIdXX</a>.',
    );
  }));

describe('test regular game links should not have a text class', () =>
  test.each([['I played a game: https://lichess.org/GameIdXX']])('should not have a text class', input => {
    expect(enhance(input)).toBe(
      'I played a game: <a target="_blank" rel="nofollow noreferrer" href="https://lichess.org/GameIdXX">lichess.org/GameIdXX</a>',
    );
  }));
