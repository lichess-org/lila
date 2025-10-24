import { objectStorage, type ObjectStorage } from './objectStorage';
import { memoize } from './common';
import { hasFeature } from './device';

// OPFS -> IDB fallback for large binary assets keyed by assetUrl

export const bigFileStorage: () => BigFileStorage = memoize(() => new BigFileStorage());

type U8 = Uint8Array<ArrayBuffer>;

type OnProgress = (loaded: number, total: number) => void;

class BigFileStorage {
  opfs?: FileSystemDirectoryHandle;
  idb: () => Promise<ObjectStorage<U8> | undefined> = memoize(async () =>
    this.opfs ? undefined : objectStorage<U8>({ store: 'big-file' }),
  );

  constructor() {
    if (hasFeature('originPrivateFileSystem'))
      navigator.storage
        .getDirectory()
        .then(h => (this.opfs = h))
        .catch(() => {});
  }

  opfsName(assetUrl: string): string {
    return new URL(assetUrl).pathname.replaceAll('/', '_');
  }

  async get(assetUrl: string, onProgress?: OnProgress): Promise<U8> {
    const opfsName = this.opfsName(assetUrl);
    try {
      const u8 = (await this.readOpfs(opfsName)) ?? (await this.idb().then(idb => idb?.get(assetUrl)));
      if (u8) return u8;
    } catch {}

    const u8 = await new Promise<U8>((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open('GET', assetUrl, true);
      xhr.responseType = 'arraybuffer';
      if (onProgress) xhr.onprogress = e => onProgress(e.loaded, e.total);
      xhr.onerror = () => reject(new Error(`fetch '${assetUrl}' failed: ${xhr.status}`));
      xhr.onload = () =>
        xhr.status / 100 === 2
          ? resolve(new Uint8Array(xhr.response))
          : reject(new Error(`fetch '${assetUrl}' failed: ${xhr.status}`));
      xhr.send();
    });
    try {
      if (this.opfs) {
        const out = await this.opfs.getFileHandle(opfsName, { create: true }).then(fh => fh.createWritable());
        await out.write(u8).then(() => out.close());
      } else await this.idb().then(idb => idb?.put(assetUrl, u8));
    } catch {}

    return u8;
  }

  async delete(assetUrl: string): Promise<void> {
    if (this.opfs) await this.opfs.removeEntry(this.opfsName(assetUrl)).catch(() => {});
    else await this.idb().then(idb => idb?.remove(assetUrl));
  }

  private async readOpfs(opfsName: string): Promise<U8 | undefined> {
    if (!this.opfs) return undefined;
    const file = await this.opfs.getFileHandle(opfsName, { create: false }).then(fh => fh.getFile());
    const buffer = new ArrayBuffer(file.size);
    const u8 = new Uint8Array(buffer);
    let offset = 0;
    const reader = file.stream().getReader();
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      u8.set(value, offset);
      offset += value.length;
    }
    return offset && offset === file.size ? u8 : undefined;
  }
}
