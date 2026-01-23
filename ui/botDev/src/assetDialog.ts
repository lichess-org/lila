import { domDialog, type Dialog, alert, confirm } from 'lib/view';
import { frag } from 'lib';
import * as licon from 'lib/licon';
import { renderRemoveButton } from './devUtil';
import { wireCropDialog } from 'bits/crop';
import { env } from './devEnv';

export type AssetType = 'image' | 'book' | 'sound';

const mimeTypes: { [type in AssetType]?: string[] } = {
  image: ['image/jpeg', 'image/png', 'image/webp'],
  book: ['application/x-chess-pgn', 'application/vnd.chess-pgn', 'application/octet-stream', '.pgn'],
  sound: ['audio/mpeg', 'audio/aac'],
};

export class AssetDialog {
  private dlg: Dialog;
  private resolve?: (key: string | undefined) => void;
  private type: AssetType;
  private isChooser: boolean;
  constructor(type?: AssetType) {
    if (!type || type === 'image') wireCropDialog();
    this.isChooser = type !== undefined;
    this.type = type ?? 'image';
  }

  private get active() {
    return this.categories[this.type];
  }

  private get local() {
    return env.assets.localKeyNames(this.type);
  }

  private get server() {
    return env.assets.serverKeyNames(this.type);
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
            { event: ['dragover', 'drop'], listener: this.dragDrop },
            { selector: '[data-action="add"]', listener: this.addItem },
            { selector: '[data-action="remove"]', listener: this.delete },
            { selector: '[data-action="push"]', listener: this.push },
            { selector: '[data-type="string"]', event: 'keydown', listener: this.nameKeyDown },
            { selector: '[data-type="string"]', event: 'change', listener: this.nameChange },
            { selector: '.asset-item', listener: this.clickItem },
            { selector: '.tab', listener: this.clickTab },
          ],
        });
        this.update();
        this.dlg.show();
      })(),
    );
  }

  update(type?: AssetType): void {
    if (type && type !== this.type) return;
    const grid = this.dlg.view.querySelector('.asset-grid') as HTMLElement;
    grid.innerHTML = `<div class="asset-item local-only" data-action="add">
        <div class="asset-preview">${this.active.placeholder}</div>
        <div class="asset-label">Add new ${this.type}</div>
      </div></div>`;
    this.local.forEach((name, key) => grid.append(this.renderAsset([key, name])));
    this.server.forEach((name, key) => !name.startsWith('.') && grid.append(this.renderAsset([key, name])));
    this.dlg.updateActions();
  }

  private bodyHtml() {
    if (this.isChooser) return `<div class="asset-grid chooser"></div>`;
    return `<div class="tabs-horiz" role="tabList">
        <span class="tab ${this.type === 'image' ? 'active' : ''}" role="tab">images</span>
        <span class="tab ${this.type === 'sound' ? 'active' : ''}" role="tab">sounds</span>
        <span class="tab ${this.type === 'book' ? 'active' : ''}" role="tab">books</span>
      </div>
      <div class="asset-grid"></div>`;
  }

  private renderAsset([key, name]: [string, string]) {
    const wrap = frag<HTMLElement>(`<div class="asset-item${
      env.assets.isLocalOnly(key) ? ' local-only' : ''
    }" data-asset="${key}">
        <div class="asset-preview"></div>
        <input type="text" class="asset-label" data-type="string" value="${name}"
${this.isChooser || !env.canPost ? ' disabled' : ''} spellcheck="false"></input>
      </div>`);
    if (!this.isChooser) {
      const localOnly = env.assets.isLocalOnly(key);
      if (localOnly || env.canPost) wrap.append(renderRemoveButton('upper-right'));
      if (localOnly && env.canPost) {
        wrap.append(
          frag(`<button class="button button-empty icon-btn upper-left" tabindex="0"
          data-icon="${licon.UploadCloud}" data-action="push" title="upload asset to server">`),
        );
      }
    }
    wrap.querySelector('.asset-preview')!.prepend(this.active.preview(key));
    return wrap;
  }

  private dragDrop = (e: DragEvent): void => {
    e.preventDefault();
    if (e.type === 'dragover') {
      e.dataTransfer!.dropEffect = 'copy';
      return;
    }
    const files = e.dataTransfer?.files;
    if (files && files.length > 0) {
      const type = this.category(files[0].type);
      if (!type || (this.resolve && type !== this.type)) return;

      this.categories[type].process(files[0], (key: string) => {
        if (this.resolve) this.resolve(key);
        else this.update();
      });
    }
  };

  private nameChange = (e: Event): void => {
    const el = e.target as HTMLInputElement;
    const key = el.closest('.asset-item')!.getAttribute('data-asset')!;
    if (this.local.get(key) === el.value) return;
    if (this.validName(el.value)) env.assets.rename(this.type, key, el.value);
  };

  private nameKeyDown = (e: KeyboardEvent): void => {
    const el = e.target as HTMLElement;
    if (e.key === 'Enter') {
      const key = el.closest('.asset-item')!.getAttribute('data-asset')!;
      const name = (el as HTMLInputElement).value;
      if (this.validName(name)) env.assets.rename(this.type, key, name);
      el.blur();
    } else if (e.key === 'Escape') {
      const key = el.closest('.asset-item')!.getAttribute('data-asset')!;
      (el as HTMLInputElement).value = this.local.get(key) ?? key;
      el.blur();
    }
  };

  private delete = async (e: Event): Promise<void> => {
    e.stopPropagation();
    const el = (e.currentTarget as Element).closest('.asset-item')!;
    const key = el.getAttribute('data-asset')!;
    if (!env.assets.isLocalOnly(key) && !(await confirm('delete this asset from the server?'))) return;
    await env.assets.delete(this.type, key);
    this.update();
  };

  private push = async (e: Event): Promise<string | undefined> => {
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
      await env.push.pushAsset(env.assets.assetBlob(this.type, key));
    } catch (x) {
      console.error('push failed', x);
      return undefined;
    }
    await env.assets.update();
    this.update();
    return name;
  };

  private clickTab = (e: Event): void => {
    const tab = (e.currentTarget as HTMLElement).closest('.tab')!;
    const type = tab?.textContent?.slice(0, -1) as AssetType;
    if (!tab || type === this.type) return;
    this.dlg.view.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    tab.classList.add('active');
    this.type = tab.textContent?.slice(0, -1) as AssetType;
    this.update();
  };

  private clickItem = (e: Event): void => {
    const item = (e.currentTarget as HTMLElement).closest('.asset-item') as HTMLElement;
    const oldKey = item?.getAttribute('data-asset');
    if (oldKey && this.isChooser) return this.resolve?.(oldKey);
  };

  private addItem = () => {
    const fileInputEl = document.createElement('input');
    fileInputEl.type = 'file';
    fileInputEl.accept = mimeTypes[this.type]!.join(',');
    fileInputEl.style.display = 'none';
    const onchange = () => {
      fileInputEl.removeEventListener('change', onchange);
      if (!fileInputEl.files || fileInputEl.files.length < 1) return;
      this.active.process(fileInputEl.files[0], (key: string) => {
        if (this.resolve) this.resolve(key);
        else this.update();
      });
    };
    fileInputEl.addEventListener('change', onchange);
    this.dlg.view.append(fileInputEl);
    fileInputEl.click();
    fileInputEl.remove();
  };

  private validName(name: string): boolean {
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

  private category(mimeType: string): AssetType | undefined {
    for (const type in mimeTypes)
      if (mimeTypes[type as AssetType]?.includes(mimeType)) return type as AssetType;
    return undefined;
  }

  private categories = {
    image: {
      placeholder: `<img src="/${env.assets.path}/image/gray-torso.webp">`,
      preview: (key: string) => frag<HTMLElement>(`<img src="${env.bot.getImageUrl(key)}">`),
      process: (file: File, onSuccess: (key: string) => void) => {
        if (!file.type.startsWith('image/')) return;
        // TODO this doesn't seem to always work. find out why.
        site.asset.loadEsm('bits.cropDialog', {
          init: {
            aspectRatio: 1,
            source: file,
            max: { megabytes: 0.05, pixels: 512 },
            onCropped: (r: Blob | boolean) => {
              if (!(r instanceof Blob)) return;
              env.assets.import('image', file.name, r).then(onSuccess);
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
        imgEl.src = env.assets.getBookCoverUrl(key);
        divEl.append(imgEl);
        return divEl;
      },
      process: (file: File, onSuccess: (key: string) => void) => {
        if (file.type === 'application/octet-stream' || file.name.endsWith('.bin')) {
          env.assets.importPolyglot(file.name, file).then(onSuccess);
        } else if (file.type.endsWith('chess-pgn') || file.name.endsWith('.pgn')) {
          const suggested = file.name.endsWith('.pgn') ? file.name.slice(0, -4) : file.name;
          domDialog({
            class: 'dev-view import-dialog',
            htmlText: `<h2>import opening book</h2>
              <div class="options">
                <span>
                  <label>as: <input type="text" value="${suggested}" class="name" style="width: 160px"></label>
                  <label>max ply: <input type="text" value="8" class="ply" style="width: 50px"></label>
                </span>
                <button class="button" data-action="import">import</button>
              </div>
              <div class="progress none">
                <div class="bar"></div>
                <div class="text"></div>
                <button class="button button-empty button-red" data-action="cancel">cancel</button>
              </div>`,
            show: true,
            modal: true,
            focus: '.name',
            noClickAway: true,
            actions: [
              {
                selector: '.options',
                event: 'keydown',
                listener: (e: KeyboardEvent) => {
                  if (!(e.target instanceof HTMLElement) || e.key !== 'Enter') return;
                  e.preventDefault();
                  e.stopPropagation();
                  e.target.closest('.options')?.querySelector<HTMLElement>('[data-action="import"]')?.click();
                },
              },
              { selector: '[data-action="cancel"]', result: 'cancel' },
              {
                selector: '[data-action="import"]',
                listener: async (_, dlg) => {
                  const name = (dlg.view.querySelector('.name') as HTMLInputElement).value;
                  const ply = Number((dlg.view.querySelector('.ply') as HTMLInputElement).value);
                  if (name.length < 4 || name.includes('/') || name.startsWith('.'))
                    alert(`bad name: ${name}`);
                  else if (!Number.isInteger(ply) || ply < 1 || ply > 16) alert(`bad ply: ${ply}`);
                  else {
                    (dlg.view.querySelector('.options') as HTMLElement).classList.add('none');
                    const progress = dlg.view.querySelector('.progress') as HTMLElement;
                    const bar = progress.querySelector('.bar') as HTMLElement;
                    const text = progress.querySelector('.text') as HTMLElement;
                    progress.classList.remove('none');
                    const key = await env.assets.importPgn(
                      name,
                      file,
                      ply,
                      false,
                      (processed: number, total: number) => {
                        bar.style.width = `${(processed / total) * 100}%`;
                        processed = Math.round(processed / (1024 * 1024));
                        total = Math.round(total / (1024 * 1024));
                        text.textContent = `processed ${processed} out of ${total} MB`;
                        return dlg.dialog.open;
                      },
                    );
                    if (dlg.returnValue !== 'cancel' && key) onSuccess(key);
                  }
                  dlg.close();
                },
              },
            ],
          });
        }
      },
    },
    sound: {
      placeholder: '',
      preview: (key: string) => {
        const soundEl = document.createElement('span');
        const audioEl = frag<HTMLAudioElement>(`<audio src="${env.bot.getSoundUrl(key)}"></audio>`);
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
        if (!file.type.startsWith('audio/')) return;
        env.assets.import('sound', file.name, file).then(onSuccess);
      },
    },
  };
}
