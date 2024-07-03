import type { PaneArgs, BooksInfo } from './types';
import type { Mapping, Book } from '../types';
import { Setting, SelectSetting } from './setting';
import { Pane } from './pane';
import * as licon from 'common/licon';

export class Books extends Setting {
  info: BooksInfo;
  constructor(p: PaneArgs) {
    super(p);
    this.label?.prepend(
      $as<Node>(`<i role="button" tabindex="0" data-icon="${licon.PlusButton}" data-click="add">`),
    );
  }

  init() {
    this.books.forEach(x => x.remove());
    this.value.forEach((_, i) => this.el.appendChild(this.makeBook(i)));
    this.updateChoices();
  }

  choices(index: number) {
    const choices = this.available.slice();
    const selected = this.selected(index);
    if (!choices.includes(selected)) choices.splice(0, 0, selected);
    return choices.map(x =>
      $as<Node>(`<option value="${x}"${selected === x ? ' selected=""' : ''}>${x}</option>`),
    );
  }

  selected(index: number) {
    return this.value[index]?.name;
  }

  update(e?: Event) {
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

  updateChoices() {
    this.selects.forEach((s, i) => {
      s.innerHTML = '';
      s.append(...this.choices(i));
    });
    this.el.querySelector('[data-click="add"]')?.classList.toggle('disabled', !this.available.length);
    this.el.classList.toggle('disabled', !this.selects.length);
  }

  makeBook(index: number) {
    const { name, weight } = this.value[index];
    return $as<Node>(`<div class="book setting">
        <select value="${name}" data-type="string"></select>wt:
        <input type="text" value="${weight}" data-type="number">
        <i role="button" tabindex="0" data-icon="${licon.Cancel}" data-click="remove">
      </div>`);
  }

  index(e: Event) {
    return this.books.indexOf((e.target as Node).parentElement!);
  }

  removeBook(index: number) {
    this.books[index].remove();
    this.value.splice(index, 1);
    this.updateChoices();
  }

  updateBook(index: number, value: string) {
    this.value[index].name = value;
    this.updateChoices();
  }

  updateWeight(index: number, input: HTMLInputElement) {
    const value = Number(input.value);
    const invalid = isNaN(value) || value < this.info.min || value > this.info.max;
    input.classList.toggle('invalid', invalid);
    if (invalid) return;
    this.value[index].weight = value;
  }

  setEnabled() {
    this.init();
  }

  get value(): Book[] {
    return this.getProperty() as Book[];
  }

  get unavailable() {
    return this.value.map(b => b.name);
  }

  get available() {
    return this.info.choices.map(x => x.value).filter(c => !this.unavailable.includes(c));
  }

  get selects() {
    return [...this.el.querySelectorAll('select')];
  }

  get books() {
    return [...this.el.querySelectorAll('.book')];
  }
}
