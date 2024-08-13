import type { BotInfo } from '../types';
import type { BotCtrl } from '../botCtrl';
import { defined } from 'common';
import type { DevAssets, AssetBlob, AssetType } from './devAssets';

export class ShareCtrl {
  readonly user: string = document.body.getAttribute('data-user') ?? 'Anonymous';

  constructor(readonly botCtrl: BotCtrl) {}

  get assets(): DevAssets {
    return this.botCtrl.assets as DevAssets;
  }
  async postBot(bot: BotInfo, progress?: (e: ProgressEvent, key: string) => void): Promise<boolean> {
    const localBlobs: (AssetBlob | undefined)[] = [];
    if (bot.image) localBlobs.push(this.assets.assetBlob('image', bot.image));
    for (const key of new Set(
      Object.values(bot.sounds ?? {})
        .flat()
        .map(s => s.key),
    )) {
      localBlobs.push(this.assets.assetBlob('sound', key));
    }
    for (const book of (bot.books ?? []).map(b => this.assets.assetBlob('book', b.key))) {
      if (!book) continue;
      localBlobs.push({ ...book, key: `${book.key}.bin` });
      const bookCover = this.assets.assetBlob('bookCover', book.key);
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

      const clearLocals: { type: AssetType; key: string }[] = [];
      for (const b of localBlobs.filter(defined)) {
        if (b.type === 'book' || b.type === 'bookCover') b.key = b.key.slice(0, -4);
        clearLocals.push(b);
      }
      await Promise.all(clearLocals.map(b => this.assets.clearLocal(b.type, b.key)));
      return true;
    } catch (x) {
      console.error('share failed', x);
      return false;
    }
    return false;
  }

  async deleteBot(uid: string): Promise<void> {
    if (await fetch(`/local/dev/bot`, { method: 'post', body: `{"uid":"${uid}"}` }).then(rsp => rsp.ok)) {
      await this.botCtrl.deleteBot(uid);
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
          const url = new URL(`/local/dev/asset/new/${type}/${key}/${name}`, window.location.origin);
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
              //this.assets.update(rsp);
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
