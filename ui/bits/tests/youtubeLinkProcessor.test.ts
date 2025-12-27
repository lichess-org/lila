import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { embedYoutubeUrl, parseYoutubeUrl } from '../src/youtubeLinkProcessor.ts';

describe('parseYoutubeUrl - realistic URLs & edge cases', () => {
  // Standard watch URL
  test('youtube.com watch URL with video ID', () => {
    const url = 'https://www.youtube.com/watch?v=dQw4w9WgXcQ';
    const result = parseYoutubeUrl(url);

    assert.deepEqual(result, {
      videoType: 'watch',
      videoId: 'dQw4w9WgXcQ',
      startTime: 0,
    });
  });

  // Watch URL with start time in seconds
  test('youtube.com watch URL with t=90 seconds', () => {
    const url = 'https://youtube.com/watch?v=dQw4w9WgXcQ&t=90';
    const result = parseYoutubeUrl(url);

    assert.deepEqual(result, {
      videoType: 'watch',
      videoId: 'dQw4w9WgXcQ',
      startTime: 90,
    });
  });

  // Watch URL with start time in h/m/s format
  test('youtube.com watch URL with t=1h2m3s', () => {
    const url = 'https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=1h2m3s';
    const result = parseYoutubeUrl(url);

    assert.deepEqual(result, {
      videoType: 'watch',
      videoId: 'dQw4w9WgXcQ',
      startTime: 3723, // 1*3600 + 2*60 + 3
    });
  });

  // Shorts URL
  test('youtube.com shorts URL', () => {
    const url = 'https://www.youtube.com/shorts/dQw4w9WgXcQ';
    const result = parseYoutubeUrl(url);

    assert.deepEqual(result, {
      videoType: 'shorts',
      videoId: 'dQw4w9WgXcQ',
      startTime: 0,
    });
  });

  // Embed URL with start parameter
  test('youtube.com embed URL', () => {
    const url = 'https://www.youtube.com/embed/dQw4w9WgXcQ?start=60';
    const result = parseYoutubeUrl(url);

    assert.deepEqual(result, {
      videoType: 'embed',
      videoId: 'dQw4w9WgXcQ',
      startTime: 60,
    });
  });

  // Live URL
  test('youtube.com live URL', () => {
    const url = 'https://www.youtube.com/live/dQw4w9WgXcQ';
    const result = parseYoutubeUrl(url);

    assert.deepEqual(result, {
      videoType: 'live',
      videoId: 'dQw4w9WgXcQ',
      startTime: 0,
    });
  });

  // youtube.com/@ChanelName URL
  test('Youtube short channel URL ignored', () => {
    const url = 'https://www.youtube.com/@ChannelName';
    const result = parseYoutubeUrl(url);

    assert.equal(result, undefined);
  });

  // youtube.com/channel/ID URL
  test('Youtube long channel URL ignored', () => {
    const url = 'https://www.youtube.com/channel/H39AHPSBcGc';
    const result = parseYoutubeUrl(url);

    assert.equal(result, undefined);
  });

  // Test without 'https://' in the beginning
  test('youtube link without protocol', () => {
    const url = 'www.youtube.com/watch/?v=dQw4w9WgXcQ';
    const result = parseYoutubeUrl(url);

    assert.equal(result, undefined);
  });

  // youtu.be short URL
  test('youtu.be URL', () => {
    const url = 'https://youtu.be/dQw4w9WgXcQ';
    const result = parseYoutubeUrl(url);

    assert.deepEqual(result, {
      videoType: 'watch',
      videoId: 'dQw4w9WgXcQ',
      startTime: 0,
    });
  });

  // youtu.be URL with start time
  test('youtu.be URL with t=1m30s', () => {
    const url = 'https://youtu.be/dQw4w9WgXcQ?t=1m30s';
    const result = parseYoutubeUrl(url);

    assert.deepEqual(result, {
      videoType: 'watch',
      videoId: 'dQw4w9WgXcQ',
      startTime: 90,
    });
  });

  // Invalid video ID (too short)
  test('invalid video ID too short', () => {
    const url = 'https://www.youtube.com/watch?v=short';
    const result = parseYoutubeUrl(url);

    assert.equal(result, undefined);
  });

  // Invalid domain
  test('unsupported domain', () => {
    const url = 'https://vimeo.com/123456';
    const result = parseYoutubeUrl(url);

    assert.equal(result, undefined);
  });

  // Malformed URL
  test('malformed URL', () => {
    const url = 'not a url';
    const result = parseYoutubeUrl(url);

    assert.equal(result, undefined);
  });

  // Trailing slashes in path
  test('youtube.com watch URL with trailing slash', () => {
    const url = 'https://www.youtube.com/watch/?v=dQw4w9WgXcQ';
    const result = parseYoutubeUrl(url);

    assert.deepEqual(result, {
      videoType: 'watch',
      videoId: 'dQw4w9WgXcQ',
      startTime: 0,
    });
  });

  // URL with extra query params
  test('youtube.com watch URL with extra params', () => {
    const url = 'https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PL123&t=30';
    const result = parseYoutubeUrl(url);

    assert.deepEqual(result, {
      videoType: 'watch',
      videoId: 'dQw4w9WgXcQ',
      startTime: 30,
    });
  });
});

describe('embedYoutubeUrl', () => {
  test('generates correct embed URL with start time', () => {
    assert.equal(
      embedYoutubeUrl({
        videoType: 'watch',
        videoId: 'dQw4w9WgXcQ',
        startTime: 90,
      }),
      'https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ?modestbranding=1&rel=0&controls=2&iv_load_policy=3&start=90',
    );
  });

  test('omits start parameter when startTime is 0', () => {
    assert.equal(
      embedYoutubeUrl({
        videoType: 'watch',
        videoId: 'dQw4w9WgXcQ',
        startTime: 0,
      }),
      'https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ?modestbranding=1&rel=0&controls=2&iv_load_policy=3',
    );
  });
});
