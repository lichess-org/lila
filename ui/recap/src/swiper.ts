import Swiper from 'swiper';
import { SwiperOptions } from 'swiper/types';
import * as mod from 'swiper/modules';
import 'swiper/css';
import 'swiper/css/bundle';
import { animateNumber } from './ui';
import { get } from 'common/data';
import { formatDuration } from './util';
import { defined } from 'common';
import { Opts } from './interfaces';

export const makeSwiper =
  (opts: Opts) =>
  (element: HTMLElement): void => {
    const progressCircle = element.querySelector('.autoplay-progress svg') as SVGElement;
    const progressContent = element.querySelector('.autoplay-progress span') as HTMLSpanElement;
    const urlSlide: number | undefined = window.location.hash
      ? parseInt(window.location.hash.slice(1))
      : undefined;
    const autoplay = !defined(urlSlide);
    let lpvTimer: number | undefined;
    const options: SwiperOptions = {
      modules: [
        mod.Pagination,
        ...(opts.navigation ? [mod.Navigation] : []),
        mod.Keyboard,
        mod.Mousewheel,
        mod.Autoplay,
      ],
      initialSlide: urlSlide || 0,
      direction: 'horizontal',
      loop: false,
      cssMode: false,
      grabCursor: true,
      keyboard: { enabled: true },
      mousewheel: true,
      pagination: {
        el: '.swiper-pagination',
        clickable: true,
      },
      navigation: opts.navigation
        ? {
            nextEl: '.swiper-button-next',
            prevEl: '.swiper-button-prev',
          }
        : undefined,
      autoplay: autoplay
        ? {
            delay: 5000,
            disableOnInteraction: false,
          }
        : undefined,
      on: {
        autoplayTimeLeft(_s, time, progress) {
          progressCircle.style.setProperty('--progress', (1 - progress).toString());
          progressContent.textContent = `${Math.ceil(time / 1000)}s`;
        },
        slideChange() {
          setTimeout(() => {
            element
              .querySelectorAll('.swiper-slide-active .animated-number')
              .forEach((counter: HTMLElement) => {
                animateNumber(counter, {});
              });
            element
              .querySelectorAll('.swiper-slide-active .animated-time')
              .forEach((counter: HTMLElement) => {
                animateNumber(counter, { duration: 1000, render: formatDuration });
              });
            element
              .querySelectorAll('.swiper-slide-active .animated-pulse')
              .forEach((counter: HTMLElement) => {
                counter.classList.remove('animated-pulse');
                setTimeout(() => {
                  counter.classList.add('animated-pulse');
                }, 100);
              });
            element.querySelectorAll('.swiper-slide-active .lpv').forEach((el: HTMLElement) => {
              const lpv = get(el, 'lpv')!;
              lpv.goTo('first');
              clearTimeout(lpvTimer);
              const next = () => {
                if (!lpv.canGoTo('next')) return;
                lpvTimer = setTimeout(() => {
                  lpv.goTo('next');
                  next();
                }, 500);
              };
              next();
            });
          }, 200);
        },
      },
    };
    new Swiper(element, options);
  };
