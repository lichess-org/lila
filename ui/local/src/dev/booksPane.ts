import { Pane, RangeSetting } from './pane';
import * as licon from 'common/licon';
import { frag } from 'common';
import type { PaneArgs, BooksInfo, RangeInfo } from './devTypes';
import type { Book } from '../types';
import { removeButton } from './devUtil';
import { assetDialog } from './assetDialog';

export class BookPane extends RangeSetting {
  label: HTMLLabelElement;
  parent: BooksPane;
  constructor(p: PaneArgs) {
    super(p);
    this.el.append(removeButton());
    const span = this.label.firstElementChild as HTMLElement;
    span.dataset.src = this.host.assets.getBookCoverUrl(span.textContent!);
    span.classList.add('imagept');
    this.rangeInput.insertAdjacentHTML('afterend', 'wt');
  }

  getProperty(): number {
    return this.parent.getWeight(this) ?? this.info.value ?? 1;
  }

  setProperty(value: number): void {
    this.parent.setWeight(this, value);
  }

  update(e?: Event): void {
    if (!(e?.target instanceof HTMLElement)) return;
    if (e.target.dataset.action === 'remove') {
      this.parent.removeBook(this);
      this.el.remove();
      this.host.update();
    } else super.update(e);
  }
}

export class BooksPane extends Pane {
  info: BooksInfo;
  template: RangeInfo = {
    type: 'range',
    class: ['setting', 'book'],
    value: 1,
    min: 1,
    max: 10,
    step: 1,
    required: true,
  };
  constructor(p: PaneArgs) {
    super(p);
    this.label?.prepend(
      frag(`<i role="button" tabindex="0" data-icon="${licon.PlusButton}" data-action="add">`),
    );
    if (!this.value) this.setProperty([]);
    this.value.forEach((_, index) => this.makeBook(index));
  }

  update(e?: Event): void {
    if (!(e?.target instanceof HTMLElement)) return;
    if (e.target.dataset.action === 'add') {
      assetDialog(this.host.assets, 'book').then(b => {
        if (!b) return;
        this.value.push({ key: b, weight: this.template?.value ?? 1 });
        this.makeBook(this.value.length - 1);
      });
    }
  }

  setWeight(pane: BookPane, value: number): void {
    this.value[this.index(pane)].weight = value;
  }

  getWeight(pane: BookPane): number | undefined {
    const index = this.index(pane);
    return index == -1 ? undefined : this.value[this.index(pane)]?.weight ?? 1;
  }

  setEnabled(): boolean {
    this.el.classList.toggle('disabled', !this.value.length);
    return true;
  }

  removeBook(pane: BookPane): void {
    this.value.splice(this.index(pane), 1);
    this.setEnabled();
  }

  index(pane: BookPane): number {
    return this.bookEls.indexOf(pane.el);
  }

  private makeBook(index: number): void {
    const book = this.value[index];
    const pargs = {
      host: this.host,
      info: {
        ...this.template,
        label: this.host.assets.nameOf(book.key),
        value: book.weight,
        id: `${this.id}_${counter++}`,
      },
      parent: this,
    };
    this.el.appendChild(new BookPane(pargs).el);
    this.setEnabled();
    this.host.update();
  }

  private get value(): Book[] {
    return this.getProperty() as Book[];
  }

  private get bookEls() {
    return [...this.el.querySelectorAll('.book')];
  }
}

let counter = 0;
