import { objectStorage } from './objectStorage';
import { memoize } from './common';
import { hasFeature } from './device';
import { log } from './permalog';

// url keyed storage for very large assets

export const bigFileStorage: () => BigFileStorage = memoize(() => new BigFileStorage());

type U8 = Uint8Array<ArrayBuffer>;

class BigFileStorage {
  private idb = memoize(() => objectStorage<U8>({ store: 'big-file' }));
  private opfs = memoize(() =>
    hasFeature('originPrivateFileSystem') ? navigator.storage.getDirectory() : undefined,
  );

  async get(assetUrl: string, onProgress?: (loaded: number, total: number) => void): Promise<U8> {
    const stored = await this.readFile(assetUrl).catch(() => undefined);
    if (stored) return stored;

    const fetched = await new Promise<U8>((resolve, reject) => {
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
    this.writeFile(assetUrl, fetched);
    return fetched;
  }

  async delete(assetUrl: string): Promise<void> {
    const opfs = await this.opfs();
    if (opfs) await opfs.removeEntry(opfsName(assetUrl)).catch(() => {});
    else await this.idb().then(idb => idb.remove(assetUrl));
  }

  private async readFile(assetUrl: string): Promise<U8 | undefined> {
    const opfs = await this.opfs();
    if (!opfs) return this.idb().then(idb => idb.get(assetUrl));

    const file = await opfs.getFileHandle(opfsName(assetUrl), { create: false }).then(fh => fh.getFile());
    const buffer = new ArrayBuffer(file.size);
    const u8 = new Uint8Array(buffer);
    const reader = file.stream().getReader();
    let offset = 0;

    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      u8.set(value, offset);
      offset += value.length;
    }
    return offset && offset === file.size ? u8 : undefined;
  }

  private async writeFile(assetUrl: string, u8: U8) {
    const out = await this.opfs()
      ?.then(f => f?.getFileHandle(opfsName(assetUrl), { create: true }).then(fh => fh.createWritable()))
      .catch(() => undefined);
    (out ? out.write(u8).then(() => out.close()) : this.idb().then(idb => idb.put(assetUrl, u8))).catch(e =>
      log(e),
    );
  }
}

function opfsName(assetUrl: string): string {
  return new URL(assetUrl).pathname.replaceAll('/', '_');
}
