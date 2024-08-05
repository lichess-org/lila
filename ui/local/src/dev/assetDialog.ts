import { domDialog, type Dialog } from 'common/dialog';
import { DevRepo } from './devRepo';
import * as licon from 'common/licon';
import { removeButton } from './util';
import { wireCropDialog } from 'bits/crop';

type AssetType = 'image' | 'book' | 'sound';

export function assetDialog(db: DevRepo, type?: AssetType): Promise<string | undefined> {
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
    readonly db: DevRepo,
    type?: AssetType,
  ) {
    this.isChooser = type !== undefined;
    this.type = type ?? 'image';
  }

  get active() {
    return this.categories[this.type];
  }

  get remote() {
    return this.db.remote?.[this.type];
  }

  get tab() {
    return this.type;
  }

  get local() {
    return this.db.local(this.type);
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
            { selector: '[data-click="add"]', listener: this.addItem },
            { selector: '[data-click="remove"]', listener: this.remove },
            { selector: '.asset-item', listener: this.clickItem },
            { selector: '.tab', listener: this.clickTab },
          ],
        });
        this.refresh();
        this.dlg.showModal();
      })(),
    );
  }

  bodyHtml = () => {
    if (this.isChooser) return `<div class="asset-grid chooser"></div>`;
    return `<div class="tabs-horiz" role="tabList">
      <span class="tab ${this.type === 'image' ? 'active' : ''}" role="tab">images</span>
      <span class="tab ${this.type === 'sound' ? 'active' : ''}" role="tab">sounds</span>
      <span class="tab ${this.type === 'book' ? 'active' : ''}" role="tab">books</span>
      </div>
      <div class="asset-grid"></div>`;
  };

  renderAsset = (key: string) => {
    const wrap = $as<HTMLElement>(`<div class="asset-item" data-asset="${key}">
        <div class="asset-preview"></div>
        <div class="asset-label">${key}</div>
      </div>`);
    if (!this.isChooser) wrap.prepend(removeButton('upper-right'));
    wrap.querySelector('.asset-preview')!.prepend(this.active.preview(key));
    return wrap;
  };

  dragDrop = (e: DragEvent): void => {
    e.preventDefault();
    if (e.type === 'dragover') {
      e.dataTransfer!.dropEffect = 'copy';
      return;
    }
    const files = e.dataTransfer?.files;
    if (files && files.length > 0) {
      this.active.process(files[0], async (newKey: string, result: Blob) => {
        await this.db.add(this.type, newKey, result);
        if (this.resolve) this.resolve(newKey);
        else this.refresh();
      });
    }
  };

  remove = async (e: Event): Promise<void> => {
    e.stopPropagation();
    const el = (e.currentTarget as Element).closest('.asset-item')!;
    const key = el.getAttribute('data-asset')!;
    await this.db.delete(this.type, key);
    this.refresh();
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
    else if (!oldKey) this.addItem();
  };

  addItem = () => {
    const fileInputEl = document.createElement('input');
    fileInputEl.type = 'file';
    fileInputEl.accept = mimeTypes[this.type]!.join(',');
    fileInputEl.style.display = 'none';
    const onchange = () => {
      fileInputEl.removeEventListener('change', onchange);
      if (!fileInputEl.files || fileInputEl.files.length < 1) return;
      this.active.process(fileInputEl.files[0], async (newKey: string, result: Blob) => {
        await this.db.add(this.type, newKey, result);
        if (this.resolve) this.resolve(newKey);
        else this.refresh();
      });
    };
    fileInputEl.addEventListener('change', onchange);
    this.dlg.view.append(fileInputEl);
    fileInputEl.click();
    fileInputEl.remove();
  };

  refresh(): void {
    const grid = this.dlg.view.querySelector('.asset-grid') as HTMLElement;
    grid.innerHTML = `<div class="asset-item" data-click="add">
        <div class="asset-preview">${this.active.placeholder}</div>
        <div class="asset-label">Add new ${this.type}</div>
      </div></div>`;
    this.local.forEach(asset => grid.append(this.renderAsset(asset)));
    if (this.isChooser) this.remote?.forEach(asset => grid.append(this.renderAsset(asset)));
    this.dlg.updateActions();
  }

  categories = {
    image: {
      placeholder: '<img src="/assets/lifat/bots/images/black-torso.webp">',
      preview: (key: string) => $as<HTMLElement>(`<img src="${this.db.getImageUrl(key)}">`),
      process: (file: File, onSuccess: (key: string, result: Blob) => void) => {
        //if (this.isChooser && item) return onSuccess(file.name, file);
        site.asset.loadEsm('bits.cropDialog', {
          init: {
            aspectRatio: 1,
            source: file,
            max: { megabytes: 0.05, pixels: 500 },
            onCropped: (r: Blob | boolean) => {
              if (!(r instanceof Blob)) return;
              this.db.add(this.type, file.name, r).then(() => onSuccess(file.name, r));
            },
          },
        });
      },
    },
    book: {
      placeholder: '',
      preview: (key: string) => {
        const divEl = $as<HTMLElement>(`<div></div>`);
        const imgEl = $as<HTMLImageElement>('<img>');
        imgEl.src = this.db.getBookCoverUrl(key);
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
        const soundEl = $as<Element>('<span></span>');
        const audioEl = $as<HTMLAudioElement>(`<audio src="${this.db.getSoundUrl(key)}"></audio>`);
        const buttonEl = $as<Node>(
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
