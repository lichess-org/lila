import { describe, test } from 'node:test';
import { pubsub as typed } from '../src/pubsub';

const pubsub = typed as any;

describe("site.pubsub 'after' and 'complete' methods", () => {
  test('after then complete', async () => {
    const event = 'normal';
    const promise = pubsub.after(event);
    pubsub.complete(event);
    await promise;
  });

  test('complete then after', async () => {
    const event = 'reverse';
    pubsub.complete(event);
    const promise = pubsub.after(event);
    await promise;
  });

  test('one completion is never enough', async () => {
    const event = 'multiple-completes';
    const await1 = pubsub.after(event);
    pubsub.complete(event);
    pubsub.complete(event);
    const await2 = pubsub.after(event);
    pubsub.complete(event);
    await await1;
    await await2;
  });
});
