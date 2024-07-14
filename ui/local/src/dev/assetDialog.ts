import { domDialog, type Dialog } from 'common/dialog';
import { AssetDb, type AssetType } from '../assetDb';
import * as licon from 'common/licon';
import { wireCropDialog } from 'bits/crop';

export function assetDialog(db: AssetDb, type?: AssetType): Promise<string | undefined> {
  if (!type || type === 'image') wireCropDialog();
  return new AssetDialog(db, type).show();
}

const mimeTypes: { [type in AssetType]: string[] } = {
  image: ['image/jpeg', 'image/png', 'image/gif', 'image/webp'],
  book: ['application/x-chess-pgn', 'application/octet-stream'],
  sound: ['audio/mpeg'],
  net: [],
};

class AssetDialog {
  private dlg: Dialog;
  private resolve?: (key: string | undefined) => void;
  private local: Set<string>;
  private type: AssetType;
  private isChooser: boolean;

  constructor(
    readonly db: AssetDb,
    type?: AssetType,
  ) {
    this.isChooser = type !== undefined;
    this.type = type ?? 'image';
  }

  get flavor() {
    return this.flavors[this.type];
  }

  get remote() {
    return this.db.remote?.[this.type];
  }

  get tab() {
    return this.type;
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
        this.local = new Set(await this.db.local(this.type));
        this.dlg = await domDialog({
          class: 'dev-view asset-dialog',
          htmlText: this.bodyHtml(),
          onClose: () => this.resolve?.(undefined),
          actions: [
            { selector: '.asset-grid', event: ['dragover', 'drop'], listener: this.dragDrop },
            { selector: '.remove', listener: this.remove },
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
    if (this.isChooser) return `<div class="asset-grid"></div>`;
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
    if (!this.isChooser) wrap.prepend($as<Node>(`<i class="remove" data-icon="${licon.Cancel}"></i>`));
    wrap.querySelector('.asset-preview')!.prepend(this.flavor.preview(key));
    return wrap;
  };

  dragDrop = (e: DragEvent): void => {
    e.preventDefault();
    const assetItem = (e.target as HTMLElement).closest('.asset-item');
    if (assetItem?.getAttribute('data-asset')) return;
    if (e.type === 'dragover') {
      e.dataTransfer!.dropEffect = 'copy';
      return;
    }
    const files = e.dataTransfer?.files;
    if (files && files.length > 0) {
      if (assetItem) {
        const key = assetItem.getAttribute('data-asset');
        // ...
      }
    }
  };

  remove = (e: Event): void => {
    const el = (e.currentTarget as Element).closest('.asset-item')!;
    const key = el.getAttribute('data-asset')!;
    this.db.delete(this.type, key);
    this.local.delete(key);
    this.refresh();
    e.stopPropagation();
  };

  clickTab = async (e: Event): Promise<void> => {
    const tab = (e.currentTarget as HTMLElement).closest('.tab')!;
    const type = tab?.textContent?.slice(0, -1) as AssetType;
    if (!tab || type === this.type) return;
    this.dlg.view.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    tab.classList.add('active');
    this.type = tab.textContent?.slice(0, -1) as AssetType;
    this.local = new Set(await this.db.local(this.type));
    this.refresh();
  };

  clickItem = (e: Event): void => {
    const item = (e.currentTarget as HTMLElement).closest('.asset-item') as HTMLElement;
    const oldKey = item?.getAttribute('data-asset');
    if (oldKey && this.isChooser) return this.resolve?.(oldKey);
    const fileInputEl = document.createElement('input');
    fileInputEl.type = 'file';
    fileInputEl.accept = mimeTypes[this.type].join(',');
    fileInputEl.style.display = 'none';
    const onchange = () => {
      fileInputEl.removeEventListener('change', onchange);
      if (!fileInputEl.files || fileInputEl.files.length < 1) return;
      this.flavor.process(fileInputEl.files[0], item, async (newKey: string, result: Blob) => {
        if (oldKey && newKey !== oldKey) {
          this.db.delete(this.type, oldKey);
          this.local.delete(oldKey);
        }
        this.local.add(newKey);
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
    grid.innerHTML = `<div class="asset-item">
        <div class="asset-preview">${this.flavor.placeholder}</div>
        <div class="asset-label">Click or drag above</div>
      </div></div>`;
    this.local.forEach(asset => grid.append(this.renderAsset(asset)));
    if (this.isChooser) this.remote?.forEach(asset => grid.append(this.renderAsset(asset)));
    this.dlg.updateActions();
  }

  flavors = {
    image: {
      placeholder: '<img src="/assets/lifat/bots/images/black-torso.webp">',
      preview: (key: string) => $as<HTMLElement>(`<img src="${this.db.getImageUrl(key)}">`),
      process: (file: File, item: HTMLElement, onSuccess: (key: string, result: Blob) => void) => {
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
      preview: (key: string) => $as<HTMLElement>(`<p>Book preview for "${key}"</p>`),
      process: (file: File, item: HTMLElement, onSuccess: (key: string, result: Blob) => void) => {
        onSuccess(file.name, file);
      },
    },
    sound: {
      placeholder: '',
      preview: (key: string) =>
        $as<HTMLElement>(`<audio controls>
            <source src="${this.db.getSoundUrl(key)}" type="audio/mpeg">
            Your browser does not support the audio element.
          </audio>`),
      process: (file: File, item: HTMLElement, onSuccess: (key: string, result: Blob) => void) => {
        onSuccess(file.name, file);
      },
    },
    net: {
      placeholder: '',
      preview: () => '',
      process: (file: File, item: HTMLElement, onSuccess: (key: string, result: Blob) => void) => {
        onSuccess(file.name, file);
      },
    },
  };
}
