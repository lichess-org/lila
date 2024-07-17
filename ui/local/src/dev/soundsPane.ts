import { Pane } from './pane';
import * as licon from 'common/licon';
import type { PaneArgs, SoundsInfo } from './types';
import type { Sounds } from '../types';

export class SoundsPane extends Pane {
  info: SoundsInfo;
  constructor(p: PaneArgs) {
    super(p);
    this.label?.prepend(
      $as<Node>(`<i role="button" tabindex="0" data-icon="${licon.PlusButton}" data-click="add">`),
    );
  }

  protected init(): void {}

  // this needs to be multipla panes, 1 per trigger

  update(e?: Event): void {
    if (!(e?.target instanceof HTMLElement)) return;
    // if (e.target.dataset.click === 'add') {
    // } else if (e.target.dataset.click === 'remove') this.removeBook(index);
    // else if (e.target.dataset.type === 'string') this.updateBook(index, (e.target as any).value);
    // else if (e.target.dataset.type === 'number') this.updateWeight(index, e.target as HTMLInputElement);
  }

  /*private updateChoices(): void {
    this.selects.forEach((s, i) => {
      s.innerHTML = '';
      s.append(...this.choices(i));
    });
    this.el.querySelector('[data-click="add"]')?.classList.toggle('disabled', !this.available.length);
    this.el.classList.toggle('disabled', !this.selects.length);
  }

  private makeSound(index: number): Node {
    const { name, weight } = this.value[index];
    return $as<Node>(`<div class="sound setting">
        <select value="${name}" data-type="string"></select>wt:
        <input type="text" value="${weight}" data-type="number">
        <i role="button" tabindex="0" data-icon="${licon.Cancel}" data-click="remove">
      </div>`);
  }*/

  private index(e: Event): number {
    return this.soundEls.indexOf((e.target as Node).parentElement!);
  }

  private updateWeight(index: number, input: HTMLInputElement) {
    /*const value = Number(input.value);
    const invalid = isNaN(value) || value < this.info.min || value > this.info.max;
    input.classList.toggle('invalid', invalid);
    if (invalid) return;
    this.value[index].weight = value;*/
  }

  setEnabled(): boolean {
    this.init();
    return true;
  }

  private get value(): Sounds {
    return this.getProperty() as Sounds;
  }

  private get selectEls() {
    return [...this.el.querySelectorAll('select')];
  }

  private get soundEls() {
    return [...this.el.querySelectorAll('.sound')];
  }
}
