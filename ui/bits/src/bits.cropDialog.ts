import { defined } from 'lib';
import { domDialog } from 'lib/view/dialog';
import { spinnerHtml } from 'lib/view/controls';
import Cropper from 'cropperjs';
import { supported, mimeAccept } from './crop';

export interface CropOpts {
  aspectRatio?: number;
  source?: Blob | string; // image or url
  max?: { megabytes?: number; pixels?: number }; // constrain size
  post?: { url: string; field?: string }; // multipart post form url and field name
  onCropped?: (result: Blob | false, error?: string) => void; // result callback
}

export async function initModule(o?: CropOpts): Promise<void> {
  if (!defined(o)) return;
  const opts: CropOpts = { ...o };

  const url =
    opts.source instanceof Blob
      ? URL.createObjectURL(opts.source)
      : typeof opts.source === 'string' && supported(opts.source)
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

  // https://github.com/fengyuanchen/cropperjs/blob/main/README.md#options
  const cropper = new Cropper(image, {
    aspectRatio: opts.aspectRatio,
    viewMode: defined(opts.aspectRatio) ? 3 : 0,
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

  const dlg = await domDialog({
    class: 'crop-viewer',
    css: [{ hashed: 'bits.cropDialog' }, { url: 'npm/cropper.min.css' }],
    modal: true,
    htmlText: $html`
      <h2>Crop image to desired shape</h2>
      <div class="crop-view"></div>
      <span class="dialog-actions">
        <button class="button button-empty cancel">cancel</button>
        <button class="button submit">submit</button>
      </span>`,
    append: [{ where: '.crop-view', node: container }],
    actions: [
      { selector: '.dialog-actions > .cancel', listener: (_, d) => d.close() },
      { selector: '.dialog-actions > .submit', listener: crop },
    ],
    onClose: () => {
      URL.revokeObjectURL(url);
      opts.onCropped?.(false, 'Cancelled');
    },
  });

  dlg.show();

  async function crop() {
    const view = dlg.view.querySelector('.crop-view') as HTMLElement;
    view.style.display = 'flex';
    view.style.alignItems = 'center';
    view.innerHTML = spinnerHtml;
    const canvas = cropper.getCroppedCanvas({
      imageSmoothingQuality: 'high',
      maxWidth: opts.max?.pixels,
      maxHeight: opts.max?.pixels,
    });
    const tryQuality = (quality = 0.9) => {
      canvas.toBlob(
        blob => {
          if (blob && blob.size < (opts.max?.megabytes ?? 100) * 1024 * 1024) submit(blob);
          else if (blob && quality > 0.05) tryQuality(quality * 0.9);
          else submit(false, 'Rendering failed');
        },
        `image/webp`,
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
      if (rsp.status / 100 === 3) redirect = rsp.headers.get('Location')!;
      else if (!rsp.ok) {
        cropped = false;
        const body = await rsp.text();
        console.error('Crop submit failed:', rsp.status, body);
      }
    }
    opts.onCropped?.(cropped, err);
    opts.onCropped = undefined;
    dlg.close();
    if (redirect) site.redirect(redirect);
  }

  function chooseImage() {
    return new Promise<File>((resolve, reject) => {
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = mimeAccept;
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
    if (bounds.width / bounds.height > aspectRatio) constrained.width = bounds.height * aspectRatio;
    else constrained.height = bounds.width / aspectRatio;
    if (!byMax) return constrained;
    const reduce = opts.max?.pixels ? Math.max(constrained.width, constrained.height) / opts.max.pixels : 1;
    constrained.width /= reduce;
    constrained.height /= reduce;
    return constrained;
  }
}
