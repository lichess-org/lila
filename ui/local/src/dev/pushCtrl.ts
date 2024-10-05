import type { BotInfo } from '../types';
import { defined } from 'common';
import type { AssetBlob, AssetType, ShareType } from './devAssets';
import { env } from '../localEnv';

// not sure why this is a class yet

export class PushCtrl {
  constructor() {}

  async pushBot(
    bot: BotInfo,
    progress?: (e: ProgressEvent, key: string) => void,
  ): Promise<undefined | string> {
    const localBlobs: (AssetBlob | undefined)[] = [];
    if (bot.image) localBlobs.push(env.repo.assetBlob('image', bot.image));
    for (const key of new Set(
      Object.values(bot.sounds ?? {})
        .flat()
        .map(s => s.key),
    )) {
      localBlobs.push(env.repo.assetBlob('sound', key));
    }
    for (const book of (bot.books ?? []).map(b => env.repo.assetBlob('book', b.key))) {
      if (!book) continue;
      localBlobs.push({ ...book, key: `${book.key}.bin` });
      const bookCover = env.repo.assetBlob('bookCover', book.key);
      if (!bookCover) continue;
      localBlobs.push({ ...bookCover, type: 'book', key: `${book.key}.png` });
    }

    try {
      await Promise.all(localBlobs.map(b => b && this.postFile(b, progress)));
      const res = await fetch('/local/dev/bot', {
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(bot),
      });
      if (!res.ok) throw new Error(res.statusText);

      await env.bot.setServer(await res.json());

      const clearLocals: { type: AssetType; key: string }[] = [];
      for (const b of localBlobs.filter(defined)) {
        if (b.type === 'book' || b.type === 'bookCover') b.key = b.key.slice(0, -4);
        clearLocals.push(b);
      }
      await Promise.all(clearLocals.map(b => env.repo.clearLocal(b.type, b.key)));
      return undefined;
    } catch (x) {
      console.error('share failed', x);
      return `share failed: ${JSON.stringify(x)}`;
    }
  }

  async deleteBot(uid: string): Promise<void> {
    if (await fetch(`/local/dev/bot`, { method: 'post', body: `{"uid":"${uid}"}` }).then(rsp => rsp.ok)) {
      await env.bot.delete(uid);
    }
  }

  async pushAsset(
    asset: AssetBlob | undefined,
    progress?: (e: ProgressEvent, key: string) => void,
  ): Promise<void> {
    if (!asset) return;
    const type = asset.type;
    if (type === 'bookCover' || type === 'net') throw new Error('invalid asset type');
    const key = type === 'book' ? `${asset.key}.bin` : asset.key;
    const posts = [this.postFile({ ...asset, key, type }, progress)];
    if (type === 'book')
      posts.push(
        env.push.postFile({
          ...env.repo.assetBlob('bookCover', asset.key)!,
          type: 'book',
          key: `${asset.key}.png`,
        }),
      );
    await Promise.all(posts);
    const clears = [env.repo.clearLocal(asset.type, asset.key)];
    if (type === 'book') clears.push(env.repo.clearLocal('bookCover', asset.key));
    await Promise.all(clears);
  }

  private postFile(
    { type, key, name, blob }: AssetBlob,
    progress?: (e: ProgressEvent, key: string) => void,
  ): Promise<any> {
    return new Promise((resolve, reject) =>
      blob
        .then(file => {
          const formData = new FormData();
          formData.append('file', file);
          formData.append('author', env.user);
          formData.append('name', name);
          const url = new URL(`/local/dev/asset/${type}/${key}`, window.location.origin);
          const xhr = new XMLHttpRequest();
          xhr.open('POST', url, true);
          xhr.upload.onprogress = e => progress?.(e, key);

          xhr.onload = () => {
            if (xhr.status === 200) resolve(JSON.parse(xhr.responseText));
            else {
              console.error('upload failed');
              reject(`${xhr.status} ${xhr.statusText}`);
            }
          };

          xhr.onerror = () => reject('network error');
          xhr.send(formData);
        })
        .catch(x => {
          console.error('upload failed', x);
          reject(x);
        }),
    );
  }
}
