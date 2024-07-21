import { Pane } from './pane';
import * as licon from 'common/licon';
import { assetDialog } from './assetDialog';
import type { PaneArgs, SoundsInfo, SelectInfo, SoundInfo } from './types';
import type { Sound } from '../types';

export class SoundsPane extends Pane {
  info: SoundsInfo;

  constructor(p: PaneArgs) {
    super(p);
    this.label?.prepend(
      $as<Node>(`<i role="button" tabindex="0" data-icon="${licon.PlusButton}" data-click="add">`),
    );
    this.value?.forEach((_, index) => this.el.appendChild(this.makeSound(index)));
  }

  protected init(): void {}

  update(e?: Event): void {
    if (!(e?.target instanceof HTMLElement)) return;
    const index = this.index(e);
    if (e.target.dataset.type === 'sound') this.updateFields(index, e.target as HTMLInputElement);
    else if (e.target.dataset.click === 'add') {
      assetDialog(this.host.assetDb, 'sound').then(s => {
        if (!s) return;
        if (!this.value) this.setProperty([]);
        this.value.push({ name: s, ...this.info.value });
        this.el.appendChild(this.makeSound(this.value.length - 1));
        this.host.update();
      });
    }
  }

  private updateFields(index: number, input: HTMLInputElement): void {
    console.log('chooey!', index, input);
    switch (input.previousSibling?.textContent) {
      case 'chance':
        this.updateChance(index, input);
        break;
      case 'volume':
        this.updateVolume(index, input);
        break;
      case 'delay':
        this.updateDelay(index, input);
        break;
      case 'only':
        this.updateOnly(index, input);
        break;
      default:
        console.log('balls!', input.previousSibling?.textContent);
    }
  }

  private makeSound(index: number): Node {
    const { name, chance, volume, delay, only } = { ...this.info.value, ...this.value[index] };
    return $as<Node>(`<div class="sound setting">
        <label><span>${name}</span></label>
        <label>chance<input type="text" value="${chance}" data-type="sound"></label>
        <label>volume<input type="text" value="${volume}" data-type="sound"></label>
        <label>delay<input type="text" value="${delay}" data-type="sound"></label>
        <label>only<input type="text" value="${only}" data-type="sound"></label>
        <i role="button" tabindex="0" data-icon="${licon.Cancel}" data-click="remove">
      </div>`);
  }

  private index(e: Event): number {
    return this.soundEls.indexOf((e.target as Element).closest('.sound')!);
  }

  private removeSound(index: number): void {
    this.soundEls[index].remove();
    this.value.splice(index, 1);
  }

  private updateDelay(index: number, input: HTMLInputElement) {
    const value = Number(input.value);
    const invalid = isNaN(value) || value < 0;
    input.classList.toggle('invalid', invalid);
    if (invalid) return;
    this.value[index].delay = value;
  }

  private updateOnly(index: number, input: HTMLInputElement) {
    this.value[index].only = input.checked ? true : undefined;
  }

  private updateVolume(index: number, input: HTMLInputElement) {
    const value = Number(input.value);
    const invalid = isNaN(value) || value < 0 || value > 1;
    input.classList.toggle('invalid', invalid);
    if (invalid) return;
    this.value[index].volume = value;
  }

  private updateChance(index: number, input: HTMLInputElement) {
    const value = Number(input.value);
    const invalid = isNaN(value) || value < 0 || value > 100; //(this.info.max ?? 1);
    input.classList.toggle('invalid', invalid);
    if (invalid) return;
    this.value[index].chance = value;
  }

  setEnabled(): boolean {
    this.el.classList.toggle('disabled', !this.value?.length);
    return true;
  }

  private get value(): Sound[] {
    return this.getProperty() as Sound[];
  }

  private get soundEls() {
    return [...this.el.querySelectorAll('.sound')];
  }
}
