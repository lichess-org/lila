import { Pane } from './pane';
import * as licon from 'common/licon';
import { frag } from 'common';
import type { PaneArgs, SoundEventInfo, Template, SoundsInfo, Sound as TemplateSound } from './devTypes';
import type { Sound } from '../types';
import { renderRemoveButton } from './devUtil';
import { env } from '../localEnv';

export class SoundEventPane extends Pane {
  info: SoundEventInfo;
  label: HTMLLabelElement;
  template: Template<TemplateSound>;
  constructor(p: PaneArgs) {
    super(p);
    this.template = (p.parent!.info as SoundsInfo).template!;
    this.label.prepend(
      frag(`<i role="button" tabindex="0" data-icon="${licon.PlusButton}" data-action="add">`),
    );
    this.label.append(frag(`<span class="hide-disabled"><hr><span class="total-chance dim"></span></span>`));
    this.value?.forEach((_, index) => this.makeSound(index));
  }

  protected init(): void {}

  async update(e?: Event): Promise<void> {
    if (!(e?.target instanceof HTMLElement)) return;
    const index = this.index(e);
    if (e.target.dataset.type === 'sound') this.updateField(index, e.target as HTMLInputElement);
    else if (e.target.dataset.action === 'remove') this.removeSound(index);
    else if (e.target.dataset.action === 'add') {
      const s = await this.host.assetDialog('sound');
      if (!s) return;
      if (!this.value) this.setProperty([]);
      this.value.push({ ...this.template.value, key: s });
      this.makeSound(this.value.length - 1);
    }
    this.setEnabled();
    this.host.update();
  }

  private updateField(index: number, input: HTMLInputElement): void {
    const key = input.dataset.field as keyof TemplateSound;
    const value = Number(input.value);
    const invalid = isNaN(value) || value < this.template.min[key] || value > this.template.max[key];
    input.classList.toggle('invalid', invalid);
    if (invalid) return;
    this.value[index][key] = value;
  }

  private makeSound(index: number) {
    const { key, chance, delay, mix } = this.value[index];
    const soundEl = frag<Element>(`<fieldset class="sound dim">
        ${this.fieldHtml('chance', chance, 'percentage chance of this sound being played')}
        ${this.fieldHtml('delay', delay, 'delay in seconds from event trigger')}
        ${this.fieldHtml(
          'mix',
          mix,
          'mix controls the volume relationship between this and the standard board sound.\nvalues from 0 to 0.5 adjust this sound from mute to full.\nvalues from 0.5 to 1 adjust the standard board sound from full to mute.\nwhen either sound is played below full volume, the other is played at full.',
        )}
      </fieldset>`);
    const buttonEl = frag(
      `<button class="button button-empty preview-sound icon-btn" data-icon="${licon.PlayTriangle}"></button>`,
    );
    const audioEl = frag<HTMLAudioElement>(`<audio src="${env.assets.getSoundUrl(key)}"></audio>`);
    buttonEl.addEventListener('click', () => audioEl.play());
    buttonEl.appendChild(audioEl);
    soundEl.prepend(frag(`<legend>${env.repo.nameOf(key)}</legend>`), buttonEl);
    soundEl.append(renderRemoveButton());
    this.el.append(soundEl);
  }

  private fieldHtml(key: keyof TemplateSound, value: number, title: string): string {
    return `<label title="${title}\n\nvalid range ${this.template.min[key]} to ${this.template.max[key]}">
        ${key}<input type="text" value="${value}" data-type="sound" data-field="${key}"></label>`;
  }

  private index(e: Event): number {
    return this.soundEls.indexOf((e.target as Element).closest('.sound')!);
  }

  private removeSound(index: number): void {
    this.soundEls[index].remove();
    this.value.splice(index, 1);
    if (!this.value.length) this.setProperty(undefined);
  }

  setEnabled(): boolean {
    this.el.classList.toggle('disabled', !this.value?.length);
    const totalEl = this.el.querySelector('.total-chance')!;
    const pct = this.value?.reduce((acc, { chance }) => acc + chance, 0) ?? 0;
    totalEl.classList.toggle('invalid', pct > this.template.max.chance || pct < this.template.min.chance);
    totalEl.textContent = `total chance ${parseFloat(pct.toFixed(1))}%`;
    return true;
  }

  private get value(): Sound[] {
    return this.getProperty() as Sound[];
  }

  private get soundEls() {
    return [...this.el.querySelectorAll('.sound')];
  }
}
