import { describe, expect, test } from 'vitest';
import { pubsub as typed } from '../src/pubsub';

const pubsub = typed as any;

describe("site.pubsub 'after' and 'complete' methods", async() => {

  test('after then complete', async() => {
    const event = 'normal';

    const promise = pubsub.after(event);
    pubsub.complete(event);

    await expect(promise).resolves.toBeUndefined();
  });

  test('complete then after', async() => {
    const event = 'immediate';

    pubsub.complete(event);

    await expect(pubsub.after(event)).resolves.toBeUndefined();
  });

  test('multiple afters then complete', async() => {
    const event = 'multiple-afters';

    const await1 = pubsub.after(event);
    const await2 = pubsub.after(event);
    pubsub.complete(event);

    await expect(await1).resolves.toBeUndefined();
    await expect(await2).resolves.toBeUndefined();
  });

  test('one completion is never enough', async() => {
    const event = 'multiple-completes';

    const await1 = pubsub.after(event);
    pubsub.complete(event);
    pubsub.complete(event);
    const await2 = pubsub.after(event);
    pubsub.complete(event);

    await expect(await1).resolves.toBeUndefined();
    await expect(await2).resolves.toBeUndefined();
  });

});
