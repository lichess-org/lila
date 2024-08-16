import type { BotInfo } from '../types';
import { defined, deepFreeze } from 'common';
import type { AssetBlob, AssetType } from './devAssets';
import { env } from '../localEnv';

// not sure why this is a class yet

export class ShareCtrl {
  constructor() {}

  async postBot(
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
      localBlobs.push({ ...bookCover, key: `${book.key}.png` });
    }

    try {
      await Promise.all(localBlobs.map(b => b && this.postAsset(b, progress)));
      const res = await fetch('/local/dev/bot', {
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(bot),
      });
      if (!res.ok) throw new Error(res.statusText);

      await env.bot.pullBot(await res.json());

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
      await env.bot.deleteBot(uid);
    }
  }

  postAsset(
    { type, key, name, blob }: AssetBlob,
    progress?: (e: ProgressEvent, key: string) => void,
  ): Promise<any> {
    console.log('postAsset', type, key, name);
    return new Promise((resolve, reject) =>
      blob
        .then(file => {
          const formData = new FormData();
          formData.append('file', file);
          const url = new URL(
            `/local/dev/asset/new/${type}/${key}/${encodeURIComponent(name)}`,
            window.location.origin,
          );
          const xhr = new XMLHttpRequest();
          xhr.open('POST', url, true);

          xhr.upload.onprogress = e => {
            if (progress) progress(e, key);
            else if (e.lengthComputable) {
              const percentComplete = (e.loaded / e.total) * 100;
              console.log(`Upload progress: ${percentComplete}%`);
            }
          };

          xhr.onload = () => {
            if (xhr.status === 200) {
              const rsp = JSON.parse(xhr.responseText);
              console.log('upload successful:', rsp);
              //env.repo.update(rsp);
              resolve(rsp);
            } else {
              console.error('upload failed');
              reject(`${xhr.status} ${xhr.statusText}`);
            }
          };

          xhr.onerror = () => {
            console.error('network error');
            reject('network error');
          };
          xhr.send(formData);
        })
        .catch(x => {
          console.error('upload failed', x);
          reject(x);
        }),
    );
  }
}
