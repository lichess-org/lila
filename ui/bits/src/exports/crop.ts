import { isSafari } from 'common/device';
import { type CropOpts } from '../bits.cropDialog';
export { type CropOpts } from '../bits.cropDialog';

export function wireCropDialog(
  opts?: CropOpts & {
    selectClicks?: Cash;
    selectDrags?: Cash;
  },
) {
  if (!opts) {
    site.asset.loadEsm('bits.cropDialog'); // preload
    return;
  }
  const init = { ...opts };

  if (!init.onCropped) init.onCropped = () => site.reload();

  init.max = { ...(init.max || {}), megabytes: 6 }; // nginx `client_max_body_size`

  init.selectClicks?.on('click', () => site.asset.loadEsm('bits.cropDialog', { init }));

  init.selectDrags?.on('dragover', e => e.preventDefault());

  init.selectDrags?.on('drop', e => {
    e.preventDefault();

    for (const item of e.dataTransfer.items) {
      if (item.kind === 'file' && item.type.startsWith('image/')) init.source = item.getAsFile();
      else if (item.type === 'text/uri-list') item.getAsString((uri: string) => (init.source = uri));
      else continue;

      if (init.source) return site.asset.loadEsm('bits.cropDialog', { init });
    }
  });
}

if (isSafari()) wireCropDialog(); // preload
