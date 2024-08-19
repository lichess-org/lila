import { domDialog, alert, confirm, type Dialog } from 'common/dialog';
import { frag } from 'common';
import * as licon from 'common/licon';
import { removeButton } from './devUtil';
import { wireCropDialog } from 'bits/crop';
import { env } from '../localEnv';

type AssetType = 'image' | 'book' | 'sound';

export function assetDialog(type?: AssetType): Promise<string | undefined> {
  if (!type || type === 'image') wireCropDialog();
  return new AssetDialog(type).show();
}

const mimeTypes: { [type in AssetType]?: string[] } = {
  image: ['image/jpeg', 'image/png', 'image/gif', 'image/webp'],
  book: ['application/x-chess-pgn', 'application/octet-stream'],
  sound: ['audio/mpeg'],
};

class AssetDialog {
  private dlg: Dialog;
  private resolve?: (key: string | undefined) => void;
  private type: AssetType;
  private isChooser: boolean;
  constructor(type?: AssetType) {
    this.isChooser = type !== undefined;
    this.type = type ?? 'image';
  }

  get active() {
    return this.categories[this.type];
  }

  get tab() {
    return this.type;
  }

  get local() {
    return env.repo.localMap(this.type);
  }

  get server() {
    return env.repo.server[this.type];
  }

  get user() {
    return env.repo.user;
  }

  show(): Promise<string | undefined> {
    return new Promise<string | undefined>(resolve =>
      (async () => {
        if (this.isChooser)
          this.resolve = (key: string) => {
            resolve(key);
            this.resolve = undefined;
            this.dlg.close();
          };
        this.dlg = await domDialog({
          class: `dev-view asset-dialog${this.isChooser ? ' chooser' : ''}`,
          htmlText: this.bodyHtml(),
          onClose: () => this.resolve?.(undefined),
          actions: [
            { selector: '.asset-grid', event: ['dragover', 'drop'], listener: this.dragDrop },
            { selector: '[data-action="add"]', listener: this.addItem },
            { selector: '[data-action="remove"]', listener: this.delete },
            { selector: '[data-action="push"]', listener: this.push },
            { selector: '[data-type="string"]', event: 'keydown', listener: this.nameKeyDown },
            { selector: '[data-type="string"]', event: 'change', listener: this.nameChange },
            { selector: '.asset-item', listener: this.clickItem },
            { selector: '.tab', listener: this.clickTab },
          ],
        });
        this.refresh();
        this.dlg.show();
      })(),
    );
  }

  bodyHtml() {
    if (this.isChooser) return `<div class="asset-grid chooser"></div>`;
    return `<div class="tabs-horiz" role="tabList">
        <span class="tab ${this.type === 'image' ? 'active' : ''}" role="tab">images</span>
        <span class="tab ${this.type === 'sound' ? 'active' : ''}" role="tab">sounds</span>
        <span class="tab ${this.type === 'book' ? 'active' : ''}" role="tab">books</span>
      </div>
      <div class="asset-grid"></div>`;
  }

  renderAsset([key, name]: [string, string]) {
    const wrap = frag<HTMLElement>(`<div class="asset-item${
      env.repo.isLocalOnly(key) ? ' local-only' : ''
    }" data-asset="${key}">
        <div class="asset-preview"></div>
        <input type="text" class="asset-label" data-type="string" value="${name}"${
          this.isChooser ? ' disabled' : ''
        } spellcheck="false"></input>
      </div>`);
    if (!this.isChooser) {
      wrap.append(removeButton('upper-right'));
      if (env.repo.isLocalOnly(key)) {
        wrap.append(
          frag(`<button class="button button-empty icon-btn upper-left" tabindex="0"
          data-icon="${licon.UploadCloud}" data-action="push" title="upload asset to server">`),
        );
      }
    }
    wrap.querySelector('.asset-preview')!.prepend(this.active.preview(key));
    return wrap;
  }

  refresh(): void {
    const grid = this.dlg.view.querySelector('.asset-grid') as HTMLElement;
    grid.innerHTML = `<div class="asset-item local-only" data-action="add">
        <div class="asset-preview">${this.active.placeholder}</div>
        <div class="asset-label">Add new ${this.type}</div>
      </div></div>`;
    this.local.forEach((name, key) => grid.append(this.renderAsset([key, name])));
    this.server.forEach((name, key) => !name.startsWith('.') && grid.append(this.renderAsset([key, name])));
    this.dlg.updateActions();
  }

  dragDrop = (e: DragEvent): void => {
    e.preventDefault();
    if (e.type === 'dragover') {
      e.dataTransfer!.dropEffect = 'copy';
      return;
    }
    const files = e.dataTransfer?.files;
    if (files && files.length > 0) {
      this.active.process(files[0], (key: string) => {
        if (this.resolve) this.resolve(key);
        else this.refresh();
      });
    }
  };

  nameChange = (e: Event): void => {
    const el = e.target as HTMLInputElement;
    const key = el.closest('.asset-item')!.getAttribute('data-asset')!;
    if (this.local.get(key) === el.value) return;
    if (this.validName(el.value)) env.repo.rename(this.type, key, el.value);
  };

  nameKeyDown = (e: KeyboardEvent): void => {
    const el = e.target as HTMLElement;
    if (e.key === 'Enter') {
      const key = el.closest('.asset-item')!.getAttribute('data-asset')!;
      const name = (el as HTMLInputElement).value;
      if (this.validName(name)) env.repo.rename(this.type, key, name);
      el.blur();
    } else if (e.key === 'Escape') {
      const key = el.closest('.asset-item')!.getAttribute('data-asset')!;
      (el as HTMLInputElement).value = this.local.get(key) ?? key;
      el.blur();
    }
  };

  delete = async (e: Event): Promise<void> => {
    e.stopPropagation();
    const el = (e.currentTarget as Element).closest('.asset-item')!;
    const key = el.getAttribute('data-asset')!;
    if (!env.repo.isLocalOnly(key) && !(await confirm('delete this asset from the server?'))) return;
    await env.repo.delete(this.type, key);
    this.refresh();
  };

  push = async (e: Event): Promise<string | undefined> => {
    e.stopPropagation();
    const el = (e.currentTarget as Element).closest('.asset-item') as HTMLElement;
    const key = el.dataset.asset!;
    const name = (
      await domDialog({
        class: 'alert',
        htmlText: `<div>push as: <input type="text" value="${this.local.get(key) ?? key}"></div>
          <span><button class="button">upload</button></span>`,
        actions: {
          selector: 'button',
          listener: async (_, dlg) => {
            const value = (dlg.view.querySelector('input') as HTMLInputElement).value;
            if (!this.validName(value)) return;
            dlg.close(value);
          },
        },
        show: true,
      })
    ).returnValue;
    if (!name || name === 'cancel') return key;
    try {
      await env.push.postAsset({ ...env.repo.assetBlob(this.type, key)!, name });
    } catch (x) {
      console.error('push failed', x);
      return undefined;
    }
    this.refresh();
    return name;
  };

  clickTab = (e: Event): void => {
    const tab = (e.currentTarget as HTMLElement).closest('.tab')!;
    const type = tab?.textContent?.slice(0, -1) as AssetType;
    if (!tab || type === this.type) return;
    this.dlg.view.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    tab.classList.add('active');
    this.type = tab.textContent?.slice(0, -1) as AssetType;
    this.refresh();
  };

  clickItem = (e: Event): void => {
    const item = (e.currentTarget as HTMLElement).closest('.asset-item') as HTMLElement;
    const oldKey = item?.getAttribute('data-asset');
    if (oldKey && this.isChooser) return this.resolve?.(oldKey);
  };

  addItem = () => {
    const fileInputEl = document.createElement('input');
    fileInputEl.type = 'file';
    fileInputEl.accept = mimeTypes[this.type]!.join(',');
    fileInputEl.style.display = 'none';
    const onchange = () => {
      fileInputEl.removeEventListener('change', onchange);
      if (!fileInputEl.files || fileInputEl.files.length < 1) return;
      this.active.process(fileInputEl.files[0], (key: string) => {
        if (this.resolve) this.resolve(key);
        else this.refresh();
      });
    };
    fileInputEl.addEventListener('change', onchange);
    this.dlg.view.append(fileInputEl);
    fileInputEl.click();
    fileInputEl.remove();
  };

  validName(name: string): boolean {
    const error =
      name.length < 3
        ? 'name must be three characters or more'
        : name.includes('/')
        ? 'name cannot contain /'
        : name.startsWith('.')
        ? 'name cannot start with period'
        : [...this.server.values()].includes(name)
        ? 'that name is already in use'
        : undefined;
    if (error) alert(error);
    return error === undefined;
  }

  categories = {
    image: {
      placeholder: '<img src="/assets/lifat/bots/images/gray-torso.webp">',
      preview: (key: string) => frag<HTMLElement>(`<img src="${env.repo.getImageUrl(key)}">`),
      process: (file: File, onSuccess: (key: string) => void) => {
        site.asset.loadEsm('bits.cropDialog', {
          init: {
            aspectRatio: 1,
            source: file,
            max: { megabytes: 0.05, pixels: 500 },
            onCropped: (r: Blob | boolean) => {
              if (!(r instanceof Blob)) return;
              env.repo.add('image', file.name, r).then(onSuccess);
            },
          },
        });
      },
    },
    book: {
      placeholder: '',
      preview: (key: string) => {
        const divEl = document.createElement('div');
        const imgEl = document.createElement('img');
        imgEl.src = env.repo.getBookCoverUrl(key);
        divEl.append(imgEl);
        return divEl;
      },
      process: (file: File, onSuccess: (key: string) => void) => {
        env.repo.add('book', file.name, file).then(onSuccess);
      },
    },
    sound: {
      placeholder: '',
      preview: (key: string) => {
        const soundEl = document.createElement('span');
        const audioEl = frag<HTMLAudioElement>(`<audio src="${env.repo.getSoundUrl(key)}"></audio>`);
        const buttonEl = frag<Node>(
          `<button class="button button-empty preview-sound" data-icon="${licon.PlayTriangle}" data-play="${key}">0.00s</button>`,
        );
        buttonEl.addEventListener('click', e => {
          audioEl.play();
          e.stopPropagation();
        });
        soundEl.append(audioEl);
        soundEl.append(buttonEl);
        audioEl.onloadedmetadata = () => {
          buttonEl.textContent = audioEl.duration.toFixed(2) + 's';
        };
        return soundEl;
      },
      process: (file: File, onSuccess: (key: string) => void) => {
        env.repo.add('sound', file.name, file).then(onSuccess);
      },
    },
  };
}
