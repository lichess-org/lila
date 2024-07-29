import { domDialog, type Dialog } from 'common/dialog';
import { ZerofishBotInfo } from '../types';
import { BotCtrl } from '../botCtrl';
import { DevAssetDb } from './devAssetDb';
import * as xhr from 'common/xhr';
import * as licon from 'common/licon';
import { removeButton } from './util';
import { wireCropDialog } from 'bits/crop';

type AssetType = 'image' | 'book' | 'sound';

export function shareDialog(botCtrl: BotCtrl, uid?: string): Promise<ShareDialog> {
  return new ShareDialog(botCtrl, uid).show();
}

class ShareDialog {
  private dlg: Dialog;
  private db: DevAssetDb;

  constructor(
    readonly botCtrl: BotCtrl,
    readonly uid?: string,
  ) {
    this.db = botCtrl.assetDb as DevAssetDb;
  }

  async show(): Promise<this> {
    this.dlg = await domDialog({
      class: 'dev-view share-dialog',
      htmlText: this.bodyHtml(),
      onClose: () => {},
      actions: [{ selector: '[data-click="share"]', listener: this.share }],
    });
    this.refresh();
    this.dlg.showModal();
    return this;
  }

  bodyHtml = () => {
    return `<div>
      <button class="button" data-click="share">share</button>

    </div>`;
  };

  share = async (_: Event): Promise<void> => {
    console.log('hooey');
    const bot = this.botCtrl.bot(this.uid) as ZerofishBotInfo;
    console.log(bot);
    if (!bot) return;
    const uploads: Promise<any>[] = [];
    if (bot.image && this.db.local('image').includes(bot.image))
      uploads.push(this.uploadAsset(this.db.blob('image', bot.image), 'image', bot.image));
    const soundKeys = [
      ...new Set<string>(
        Object.values(bot.sounds ?? {})
          .flat()
          .map(s => s.key),
      ),
    ];
    soundKeys.forEach(key => uploads.push(this.uploadAsset(this.db.blob('sound', key), 'sound', key)));
    for (const book of (bot.books ?? []).filter(b => this.db.local('book').includes(b.name))) {
      uploads.push(this.uploadAsset(this.db.blob('book', book.name), 'book', `${book.name}.bin`));
      uploads.push(this.uploadAsset(this.db.blob('bookCover', book.name), 'book', `${book.name}.png`));
    }
    try {
      await Promise.all(uploads);
      const bots = await fetch('/local/dev/bot', {
        method: 'post',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(bot),
      }).then(res => res.json());
      console.log('sharing is caring!', bots);
      this.refresh();
    } catch (x) {
      console.error('share failed', x);
    }
  };

  refresh(): void {}

  private uploadAsset = (
    blobPromise: Promise<Blob>,
    type: AssetType,
    key: string,
    progress?: (e: ProgressEvent) => void,
  ): Promise<any> => {
    return new Promise((resolve, reject) =>
      blobPromise
        .then(blob => {
          const formData = new FormData();
          formData.append('file', blob);
          const url = new URL('/local/dev/asset', window.location.origin);
          url.searchParams.set('tpe', type);
          url.searchParams.set('name', key);
          const xhr = new XMLHttpRequest();
          xhr.open('POST', url, true);

          xhr.upload.onprogress = e => {
            if (progress) progress(e);
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
