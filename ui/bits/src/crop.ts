import { isSafari, isFirefox } from 'lib/device';
import { type CropOpts } from './bits.cropDialog';
export { type CropOpts } from './bits.cropDialog';

export function wireCropDialog(
  opts?: CropOpts & {
    selectClicks?: Cash;
    selectDrags?: Cash;
  },
): void {
  if (!opts) {
    site.asset.loadEsm('bits.cropDialog'); // preload
    return;
  }
  const init = { ...opts };

  if (!init.onCropped) init.onCropped = () => site.reload();

  init.max = { ...(init.max || {}), megabytes: 6 }; // nginx `client_max_body_size`

  init.selectClicks?.on('click', () => site.asset.loadEsm('bits.cropDialog', { init }));

  init.selectDrags?.on('dragover', e => e.preventDefault());

  init.selectDrags?.on('drop', async e => {
    e.preventDefault();
    init.source = undefined;

    for (const item of e.dataTransfer.items) {
      if (item.kind === 'file' && mimeAccept.includes(item.type)) {
        init.source = item.getAsFile();
        return site.asset.loadEsm('bits.cropDialog', { init });
      } else if (item.type === 'text/uri-list' || item.type === 'text/plain') {
        const uri = await new Promise<string>(res => item.getAsString(res));
        if (supported(uri)) {
          init.source = uri;
          return site.asset.loadEsm('bits.cropDialog', { init });
        }
      }
    }
  });
}

const imageTypes = ['jpeg', 'jpg', 'png', 'webp', 'gif', 'tiff', 'avif'].concat(
  isSafari() ? ['heic', 'heif', 'svg+xml'] : isFirefox() ? ['svg+xml'] : [],
); // chrome canvas butchers svgs without intrinsic size

export const mimeAccept: string = imageTypes.map(t => `image/${t}`).join(',');

export function supported(src: string): boolean {
  const ext = src.split('.').pop()?.toLowerCase();
  return Boolean(ext && imageTypes.find(t => ext.startsWith(t.split('+')[0])));
}

if (isSafari()) wireCropDialog(); // preload
