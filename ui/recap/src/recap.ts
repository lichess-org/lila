import { init, classModule, attributesModule } from 'snabbdom';
import view from './view';
import type { Recap } from './interfaces';
import { makeSwiper } from './swiper';

export function initModule(data: Recap): void {
  const patch = init([classModule, attributesModule]);
  document.querySelectorAll('#recap-swiper').forEach((el: HTMLElement) => {
    patch(el, view(data));
    makeSwiper(data)(el);
  });
}
