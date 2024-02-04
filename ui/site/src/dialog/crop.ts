//import { defined } from 'common';
import Cropper from 'cropperjs';

export interface CropOpts {
  aspectRatio: number; // required
  source?: Blob | string; // image or url
  max: { megabytes: number; pixels?: number }; // constrain size
  post?: { url: string; field?: string }; // multipart post form url and field name
  onCropped?: (result: Blob | boolean, error?: string) => void; // result callback
}

// example: .on('click', () => lichess.asset.loadEsm('cropDialog', { init: { aspectRatio: 2 } }))

export default async function initModule(o?: CropOpts) {
  if (!o) return;
  const opts: CropOpts = { ...o };

  const url =
    opts.source instanceof Blob
      ? URL.createObjectURL(opts.source)
      : typeof opts.source == 'string'
      ? URL.createObjectURL((opts.source = await (await fetch(opts.source)).blob()))
      : URL.createObjectURL((opts.source = await chooseImage()));
  if (!url) {
    opts.onCropped?.(false, 'Cancelled');
    return;
  }

  const image = new Image();
  await new Promise((resolve, reject) => {
    image.src = url;
    image.onload = resolve;
    image.onerror = reject;
  }).catch(e => {
    URL.revokeObjectURL(url);
    opts.onCropped?.(false, `Image load failed: ${url} ${e.toString()}`);
    return;
  });

  const viewBounds = constrain(image.naturalWidth / image.naturalHeight, {
    width: window.innerWidth * 0.6,
    height: window.innerHeight * 0.6,
  });

  const container = document.createElement('div');
  container.appendChild(image);

  const cropper = new Cropper(image, {
    aspectRatio: opts.aspectRatio,
    viewMode: 3,
    guides: false,
    responsive: false,
    restore: false,
    checkCrossOrigin: false,
    movable: false,
    rotatable: false,
    scalable: false,
    zoomable: false,
    toggleDragModeOnDblclick: false,
    autoCropArea: 1,
    minContainerWidth: viewBounds.width,
    minContainerHeight: viewBounds.height,
  });

  const dlg = await lichess.dialog.dom({
    class: 'crop-viewer',
    css: [{ themed: 'cropDialog' }, { url: 'npm/cropper.min.css' }],
    htmlText: `<h2>Crop image to desired shape</h2>
<div class="crop-view"></div>
<span class="dialog-actions"><button class="button button-empty cancel">cancel</button>
<button class="button submit">submit</button></span>`,
    append: [{ selector: '.crop-view', node: container }],
    action: [
      { selector: '.dialog-actions > .cancel', action: d => d.close() },
      { selector: '.dialog-actions > .submit', action: crop },
    ],
    onClose: () => {
      URL.revokeObjectURL(url);
      opts.onCropped?.(false, 'Cancelled');
    },
  });

  dlg.showModal();

  async function crop() {
    const view = dlg.view.querySelector('.crop-view') as HTMLElement;
    view.style.display = 'flex';
    view.style.alignItems = 'center';
    view.innerHTML = lichess.spinnerHtml;
    const canvas = cropper.getCroppedCanvas({
      imageSmoothingQuality: 'high',
      maxWidth: opts.max?.pixels,
      maxHeight: opts.max?.pixels,
    });
    const tryQuality = (quality = 0.9) => {
      canvas.toBlob(
        blob => {
          if (blob && blob.size < opts.max.megabytes * 1024 * 1024) submit(blob);
          else if (blob && quality > 0.05) tryQuality(quality * 0.9);
          else submit(false, 'Rendering failed');
        },
        'image/jpeg',
        quality,
      );
    };
    tryQuality();
  }

  async function submit(cropped: Blob | false, err?: string) {
    let redirect: string | undefined;
    if (cropped && opts.post) {
      const formData = new FormData();
      formData.append(opts.post.field ?? 'picture', cropped);
      const rsp = await fetch(opts.post.url, { method: 'POST', body: formData });
      if (rsp.status / 100 == 3) redirect = rsp.headers.get('Location')!;
      else if (!rsp.ok) cropped = false;
    }
    opts.onCropped?.(cropped, err);
    opts.onCropped = undefined;
    dlg.close();
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

  function constrain(aspectRatio: number, bounds: { width: number; height: number }, byMax = false) {
    const constrained = { ...bounds };
    if (bounds.width / bounds.height > aspectRatio) {
      constrained.width = bounds.height * aspectRatio;
    } else {
      constrained.height = bounds.width / aspectRatio;
    }
    if (!byMax) return constrained;
    const reduce = opts.max?.pixels ? Math.max(constrained.width, constrained.height) / opts.max.pixels : 1;
    constrained.width /= reduce;
    constrained.height /= reduce;
    return constrained;
  }
}
