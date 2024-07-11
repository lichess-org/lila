import { Pane } from './pane';
import * as licon from 'common/licon';
import type { PaneArgs, BooksInfo } from './types';
import type { Book } from '../types';

export class BooksPane extends Pane {
  info: BooksInfo;
  constructor(p: PaneArgs) {
    super(p);
    this.label?.prepend(
      $as<Node>(`<i role="button" tabindex="0" data-icon="${licon.PlusButton}" data-click="add">`),
    );
  }

  protected init(): void {
    this.books.forEach(x => x.remove());
    this.value.forEach((_, i) => this.el.appendChild(this.makeBook(i)));
    this.updateChoices();
  }

  private choices(index: number): Node[] {
    const choices = this.available.slice();
    const selected = this.selected(index);
    if (!choices.includes(selected)) choices.splice(0, 0, selected);
    return choices.map(x =>
      $as<Node>(`<option value="${x}"${selected === x ? ' selected=""' : ''}>${x}</option>`),
    );
  }

  update(e?: Event): void {
    if (!(e?.target instanceof HTMLElement)) return;
    const index = this.index(e);
    if (e.target.dataset.click === 'add') {
      this.value.push({ name: this.available[0], weight: 1 });
      this.el.append(this.makeBook(this.value.length - 1));
      this.updateChoices();
    } else if (e.target.dataset.click === 'remove') this.removeBook(index);
    else if (e.target.dataset.type === 'string') this.updateBook(index, (e.target as any).value);
    else if (e.target.dataset.type === 'number') this.updateWeight(index, e.target as HTMLInputElement);
  }

  private updateChoices(): void {
    this.selects.forEach((s, i) => {
      s.innerHTML = '';
      s.append(...this.choices(i));
    });
    this.el.querySelector('[data-click="add"]')?.classList.toggle('disabled', !this.available.length);
    this.el.classList.toggle('disabled', !this.selects.length);
  }

  private makeBook(index: number): Node {
    const { name, weight } = this.value[index];
    return $as<Node>(`<div class="book setting">
        <select value="${name}" data-type="string"></select>wt:
        <input type="text" value="${weight}" data-type="number">
        <i role="button" tabindex="0" data-icon="${licon.Cancel}" data-click="remove">
      </div>`);
  }

  private index(e: Event): number {
    return this.books.indexOf((e.target as Node).parentElement!);
  }

  private removeBook(index: number): void {
    this.books[index].remove();
    this.value.splice(index, 1);
    this.updateChoices();
  }

  private updateBook(index: number, value: string) {
    this.value[index].name = value;
    this.updateChoices();
  }

  private updateWeight(index: number, input: HTMLInputElement) {
    const value = Number(input.value);
    const invalid = isNaN(value) || value < this.info.min || value > this.info.max;
    input.classList.toggle('invalid', invalid);
    if (invalid) return;
    this.value[index].weight = value;
  }

  setEnabled(): boolean {
    this.init();
    return true;
  }

  private get value(): Book[] {
    return this.getProperty() as Book[];
  }

  private get unavailable() {
    return this.value.map(b => b.name);
  }

  private get available() {
    return this.info.choices.map(x => x.value).filter(c => !this.unavailable.includes(c));
  }

  private get selects() {
    return [...this.el.querySelectorAll('select')];
  }

  private get books() {
    return [...this.el.querySelectorAll('.book')];
  }

  private selected(index: number): string {
    return this.value[index]?.name ?? '';
  }
}
