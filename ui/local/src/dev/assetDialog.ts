import { domDialog, alert, type Dialog } from 'common/dialog';
import { DevAssets } from './devAssets';
import { frag } from 'common';
import * as licon from 'common/licon';
import { removeButton } from './devUtil';
import { wireCropDialog } from 'bits/crop';

type AssetType = 'image' | 'book' | 'sound';

export function assetDialog(db: DevAssets, type?: AssetType): Promise<string | undefined> {
  if (!type || type === 'image') wireCropDialog();
  return new AssetDialog(db, type).show();
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

  constructor(
    readonly assets: DevAssets,
    type?: AssetType,
  ) {
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
    return this.assets.localNames(this.type);
  }

  get server() {
    return this.assets.serverNames(this.type);
  }

  get user() {
    return this.assets.user;
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
          class: 'dev-view asset-dialog',
          htmlText: this.bodyHtml(),
          onClose: () => this.resolve?.(undefined),
          actions: [
            { selector: '.asset-grid', event: ['dragover', 'drop'], listener: this.dragDrop },
            { selector: '[data-action="add"]', listener: this.addItem },
            { selector: '[data-action="remove"]', listener: this.remove },
            { selector: '[data-action="share"]', listener: this.share },
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
    const wrap = frag<HTMLElement>(`<div class="asset-item" data-asset="${key}">
        <div class="asset-preview"></div>
        <div class="asset-label">${name}</div>
      </div>`);
    if (!this.isChooser) {
      wrap.prepend(
        frag(
          `<button class="button button-empty icon-btn upper-left" tabindex="0" data-icon="${licon.UploadCloud}" data-action="share">`,
        ),
      );
      wrap.prepend(removeButton('upper-right'));
    }
    wrap.querySelector('.asset-preview')!.prepend(this.active.preview(key));
    return wrap;
  }

  refresh(): void {
    const grid = this.dlg.view.querySelector('.asset-grid') as HTMLElement;
    grid.innerHTML = `<div class="asset-item" data-action="add">
        <div class="asset-preview">${this.active.placeholder}</div>
        <div class="asset-label">Add new ${this.type}</div>
      </div></div>`;
    this.local.forEach((name, key) => grid.append(this.renderAsset([key, name])));
    if (this.isChooser) this.server.forEach((name, key) => grid.append(this.renderAsset([key, name])));
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
      this.active.process(files[0], async (key: string, result: Blob) => {
        const newKey = await this.assets.add(this.type, key, result);
        if (this.resolve) this.resolve(newKey);
        else this.refresh();
      });
    }
  };

  remove = async (e: Event): Promise<void> => {
    e.stopPropagation();
    const el = (e.currentTarget as Element).closest('.asset-item')!;
    const key = el.getAttribute('data-asset')!;
    await this.assets.delete(this.type, key);
    this.refresh();
  };

  share = async (e: Event): Promise<string | undefined> => {
    console.log('fuuuu');
    e.stopPropagation();
    const el = (e.currentTarget as Element).closest('.asset-item') as HTMLElement;
    const key = el.dataset.asset!;
    const name = this.local.get(key) ?? key;
    const assetName = (
      await domDialog({
        class: 'alert',
        htmlText: `<div>share as: <input type="text" value="${name}"></div>
        <span><button class="button">upload</button></span>`,
        actions: {
          selector: 'button',
          listener: async (_, dlg) => {
            const name = (dlg.view.querySelector('input') as HTMLInputElement).value;
            if (!name) dlg.close();
            if (name.includes('/')) return alert('name cannot contain /');
            const fullname = name; //`${this.user}/${name}${ext ? '.' + ext : ''}`;
            if ([...this.server.values()].includes(fullname)) {
              await alert('that name is already used.');
              return;
            }
            dlg.close(fullname);
          },
        },
        show: true,
      })
    ).returnValue;
    if (!assetName || assetName === 'cancel') return key;
    try {
      await this.assets.shareCtrl.postAsset({ ...this.assets.assetBlob(this.type, key)!, name: assetName });
      if (assetName !== name) await this.assets.rename(this.type, name, assetName);
    } catch (x) {
      console.error('share failed', x);
      return undefined;
    }
    this.refresh();
    return assetName;
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
      this.active.process(fileInputEl.files[0], async (key: string, result: Blob) => {
        const newKey = await this.assets.add(this.type, key, result);
        if (this.resolve) this.resolve(newKey);
        else this.refresh();
      });
    };
    fileInputEl.addEventListener('change', onchange);
    this.dlg.view.append(fileInputEl);
    fileInputEl.click();
    fileInputEl.remove();
  };

  categories = {
    image: {
      placeholder: '<img src="/assets/lifat/bots/images/gray-torso.webp">',
      preview: (key: string) => frag<HTMLElement>(`<img src="${this.assets.getImageUrl(key)}">`),
      process: (file: File, onSuccess: (key: string, result: Blob) => void) => {
        site.asset.loadEsm('bits.cropDialog', {
          init: {
            aspectRatio: 1,
            source: file,
            max: { megabytes: 0.05, pixels: 500 },
            onCropped: (r: Blob | boolean) => {
              if (!(r instanceof Blob)) return;
              this.assets.add(this.type, file.name, r).then(() => onSuccess(file.name, r));
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
        imgEl.src = this.assets.getBookCoverUrl(key);
        divEl.append(imgEl);
        return divEl;
      },
      process: (file: File, onSuccess: (key: string, result: Blob) => void) => {
        onSuccess(file.name, file);
      },
    },
    sound: {
      placeholder: '',
      preview: (key: string) => {
        const soundEl = document.createElement('span');
        const audioEl = frag<HTMLAudioElement>(`<audio src="${this.assets.getSoundUrl(key)}"></audio>`);
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
      process: (file: File, onSuccess: (key: string, result: Blob) => void) => {
        onSuccess(file.name, file);
      },
    },
  };
}
