import { isSafari } from 'common/device';
export { type CropOpts } from '../crop';

export function wireCropDialog(args?: {
  aspectRatio: number;
  selectClicks?: Cash;
  selectDrags?: Cash;
  max?: { megabytes?: number; pixels?: number };
  post?: { url: string; field?: string };
  onCropped?: (result: Blob | boolean) => void;
}) {
  if (!args) {
    site.asset.loadEsm('cropDialog'); // preload
    return;
  }
  const cropOpts = { ...args };
  if (!cropOpts.onCropped) cropOpts.onCropped = () => site.reload();
  cropOpts.max = { ...(cropOpts.max || {}), megabytes: 6 }; // mirrors the nginx config `client_max_body_size`
  cropOpts.selectClicks?.on('click', () => site.asset.loadEsm('cropDialog', { init: cropOpts }));
  cropOpts.selectDrags?.on('dragover', e => e.preventDefault());
  cropOpts.selectDrags?.on('drop', e => {
    e.preventDefault();
    for (const item of e.dataTransfer.items) {
      if (item.kind === 'file' && item.type.startsWith('image/')) {
        site.asset.loadEsm('cropDialog', { init: { ...cropOpts, source: item.getAsFile() } });
      } else if (item.kind === 'string' && item.type === 'text/uri-list') {
        item.getAsString((uri: string) =>
          site.asset.loadEsm('cropDialog', { init: { ...cropOpts, source: uri } }),
        );
      } else continue;
      break;
    }
  });
}

if (isSafari()) wireCropDialog(); // preload
