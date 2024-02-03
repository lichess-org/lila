import { domDialog } from 'common/dialog';
import Cropper from 'cropperjs';

export interface CropOpts {
  aspectRatio: number;
  source?: Blob | string;
  max?: { megabytes?: number; pixels?: number };
  post?: { url: string; field?: string };
  onCropped?: (result: Blob | boolean) => void;
}

/** myButton.on('click', () => lichess.asset.loadEsm('cropDialog', { init: { aspectRatio: 2 } }))
 *
 * if no source is provided, the user will be prompted to select a file
 * if post is provided, the cropped image will be posted to that url with given field name or 'picture'
 * max constrains the size of the cropped image, either in MB, pixels along the longest extent, or both
 * if onCropped is provided, the cropped image will be passed to that function
 * if something fails, onCropped will be called with false
 */

export default async function initModule(o: CropOpts) {
  const url =
    o.source instanceof Blob
      ? URL.createObjectURL(o.source)
      : typeof o.source == 'string'
      ? URL.createObjectURL((o.source = await (await fetch('o.url')).blob()))
      : URL.createObjectURL((o.source = await chooseImage()));

  if (!url) return;

  const image = new Image();
  await new Promise((resolve, reject) => {
    image.src = url;
    image.onload = resolve;
    image.onerror = reject;
  }).catch(() => {
    URL.revokeObjectURL(url);
    return;
  });

  let viewWidth = window.innerWidth * 0.6,
    viewHeight = window.innerHeight * 0.6;

  const srcRatio = image.naturalWidth / image.naturalHeight;
  if (srcRatio > viewWidth / viewHeight) viewHeight = viewWidth / srcRatio;
  else viewWidth = viewHeight * srcRatio;

  const container = document.createElement('div');
  container.appendChild(image);
  const cropper = new Cropper(image, {
    aspectRatio: o.aspectRatio,
    viewMode: 1,
    guides: false,
    zoomable: false,
    autoCropArea: 1,
    minContainerWidth: viewWidth,
    minContainerHeight: viewHeight,
  });

  const dlg = await domDialog({
    class: 'crop-viewer',
    css: [{ themed: 'cropDialog' }, { url: 'npm/cropper.min.css' }],
    htmlText: `<h2>Crop image to desired shape</h2>
<div class="crop-view" style="width: ${viewWidth}px; height: ${viewHeight}px;"></div>
<span class="dialog-actions"><button class="button button-empty cancel">cancel</button>
<button class="button submit">submit</button></span>`,
    append: [{ selector: '.crop-view', node: container }],
    action: [
      { selector: '.dialog-actions > .cancel', action: d => d.close() },
      { selector: '.dialog-actions > .submit', action: crop },
    ],
    onClose: () => {
      cropper?.destroy();
      URL.revokeObjectURL(url);
    },
  });

  dlg.showModal();

  function crop() {
    const view = dlg.view.querySelector('.crop-view') as HTMLElement;
    view.style.display = 'flex';
    view.style.alignItems = 'center';
    view.innerHTML = lichess.spinnerHtml;
    const canvas = cropper!.getCroppedCanvas({
      imageSmoothingQuality: 'high',
      maxWidth: o.max?.pixels,
      maxHeight: o.max?.pixels,
    });
    const tryQuality = (quality = 0.8) => {
      canvas.toBlob(
        blob => {
          if (blob && (!o.max?.megabytes || blob.size < o.max.megabytes * 1024 * 1024)) submit(blob);
          else if (blob && quality > 0.05) tryQuality(quality * 0.5);
          else submit(false);
        },
        'image/webp',
        quality,
      );
    };
    tryQuality();
  }

  async function submit(cropped: Blob | false) {
    let redirect: string | undefined;
    if (cropped && o.post) {
      const formData = new FormData();
      formData.append(o.post.field ?? 'picture', cropped);
      const rsp = await fetch(o.post.url, { method: 'POST', body: formData });
      if (rsp.status / 100 == 3) redirect = rsp.headers.get('Location')!;
      else if (!rsp.ok) cropped = false;
    }
    dlg.close();
    o.onCropped?.(cropped);
    if (redirect) lichess.redirect(redirect);
  }

  function chooseImage() {
    return new Promise<File>((resolve, reject) => {
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = 'image/*';
      input.onchange = () => {
        const file = input.files?.[0];
        if (file) resolve(file);
        else reject();
      };
      input.click();
    });
  }
}
