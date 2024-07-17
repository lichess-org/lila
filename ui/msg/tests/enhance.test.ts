import { describe, expect, test } from 'vitest';
import { enhance, imgurRegex } from '../src/view/enhance';

describe('test imgur matching', () => {
  test.each([
    ['https://i.imgur.com/HFn4lkh.jpeg', 'https://i.imgur.com/HFn4lkh.jpg'],
    ['http://imgur.com/HFn4lkh.png', 'https://i.imgur.com/HFn4lkh.jpg'],
  ])('embed individual images', (input, link) => {
    expect(imgurRegex.test(input)).toBe(true);
    expect(enhance(input)).toBe(
      `<a target="_blank" rel="noopener nofollow noreferrer" href="${link}"><img src="${link}"/></a>`,
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
    expect(enhance(input)).toBe(
      `<a target="_blank" rel="noopener nofollow noreferrer" href="${input}">${link}</a>`,
    );
  });
});
