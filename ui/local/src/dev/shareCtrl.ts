import { ZerofishBotInfo } from '../types';
import { BotCtrl } from '../botCtrl';
import { DevRepo } from './devRepo';

export type ShareType = 'image' | 'book' | 'sound';
export type AssetBlob = { type: ShareType; key: string; blob: Promise<Blob> };

export class ShareCtrl {
  private db: DevRepo;

  constructor(readonly botCtrl: BotCtrl) {
    this.db = botCtrl.assetDb as DevRepo;
  }

  async postBot(bot: ZerofishBotInfo, progress?: (e: ProgressEvent, key: string) => void): Promise<boolean> {
    const localBlobs: AssetBlob[] = [];
    if (bot.image && this.db.local('image').includes(bot.image))
      localBlobs.push({ type: 'image', key: bot.image, blob: this.db.blob('image', bot.image) });
    const soundKeys = [
      ...new Set<string>(
        Object.values(bot.sounds ?? {})
          .flat()
          .map(s => s.key),
      ),
    ];
    soundKeys.forEach(key => localBlobs.push({ type: 'sound', key, blob: this.db.blob('sound', key) }));
    for (const book of (bot.books ?? []).filter(b => this.db.local('book').includes(b.name))) {
      localBlobs.push({ type: 'book', key: `${book.name}.bin`, blob: this.db.blob('book', book.name) });
      localBlobs.push({ type: 'book', key: `${book.name}.png`, blob: this.db.blob('bookCover', book.name) });
    }
    try {
      await Promise.all(localBlobs.map(b => this.postAsset(b, progress)));
      const bots = await fetch('/local/dev/bot', {
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(bot),
      }).then(res => res.json());
      console.log('sharing is caring!', bots);
      const deletes: { type: ShareType; key: string }[] = [];
      for (const b of localBlobs.filter(b => b.type !== 'book' || b.key.endsWith('.bin'))) {
        if (b.type === 'book') b.key = b.key.slice(0, -4); // books are 2 assets but we prefer to treat as 1
        deletes.push(b);
      }
      await Promise.all(deletes.map(b => this.db.delete(b.type, b.key)));
      return true;
    } catch (x) {
      console.error('share failed', x);
      return false;
    }
  }

  async deleteBot(uid: string): Promise<void> {
    if (await fetch(`/local/dev/bot`, { method: 'post', body: `{"uid":"${uid}"}` }).then(rsp => rsp.ok)) {
      await this.botCtrl.deleteBot(uid);
    }
  }

  private postAsset = (
    { type, key, blob }: AssetBlob,
    progress?: (e: ProgressEvent, key: string) => void,
  ): Promise<any> => {
    return new Promise((resolve, reject) =>
      blob
        .then(file => {
          const formData = new FormData();
          formData.append('file', file);
          const url = new URL('/local/dev/asset', window.location.origin);
          url.searchParams.set('tpe', type);
          url.searchParams.set('name', key);
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
              this.db.update(rsp);
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
  };
}
