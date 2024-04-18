import { isSafari } from 'common/device';
import { type CropOpts as Params } from '../bits.cropDialog';
export { type CropOpts } from '../bits.cropDialog';

export function wireCropDialog(args?: {
  aspectRatio: number;
  selectClicks?: Cash;
  selectDrags?: Cash;
  max?: { megabytes?: number; pixels?: number };
  post?: { url: string; field?: string };
  onCropped?: (result: Blob | boolean) => void;
}) {
  if (!args) {
    site.asset.loadEsm('bits.cropDialog'); // preload
    return;
  }
  const cropOpts = { ...args };
  if (!cropOpts.onCropped) cropOpts.onCropped = () => site.reload();
  cropOpts.max = { ...(cropOpts.max || {}), megabytes: 6 }; // mirrors the nginx config `client_max_body_size`
  cropOpts.selectClicks?.on('click', () => site.asset.loadEsm('bits.cropDialog', { init: cropOpts }));
  cropOpts.selectDrags?.on('dragover', e => e.preventDefault());
  cropOpts.selectDrags?.on('drop', e => {
    e.preventDefault();
    const init = { ...cropOpts } as Params;
    for (const item of e.dataTransfer.items) {
      if (item.kind === 'file' && item.type.startsWith('image/')) {
        init.source = item.getAsFile();
      } else if (item.type === 'text/uri-list') {
        item.getAsString((uri: string) => (init.source = uri));
      } else continue;

      if (init.source) {
        site.asset.loadEsm('bits.cropDialog', { init });
        break;
      }
    }
  });
}

if (isSafari()) wireCropDialog(); // preload
