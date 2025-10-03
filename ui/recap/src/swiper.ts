import Swiper from 'swiper';
import type { SwiperOptions } from 'swiper/types';
import * as mod from 'swiper/modules';
import { animateNumber } from './ui';
import { get } from 'lib/data';
import { formatDuration } from './util';
import { defined } from 'lib';
import type { Opts } from './interfaces';

export const makeSwiper =
  (opts: Opts) =>
  (element: HTMLElement): void => {
    const progressDiv = element.querySelector('.autoplay-progress') as HTMLDivElement;
    const progressCircle = element.querySelector('.autoplay-progress svg') as SVGElement;
    const progressContent = element.querySelector('.autoplay-progress span') as HTMLSpanElement;
    const urlSlide: number | undefined = window.location.hash
      ? parseInt(window.location.hash.slice(1))
      : undefined;
    const autoplay = !defined(urlSlide);
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
            stopOnLastSlide: true,
          }
        : undefined,
      on: {
        autoplayTimeLeft(swiper, time, progress) {
          if (swiper.isEnd) progressDiv.style.display = 'none';
          else progressDiv.style.display = 'flex';
          progressCircle.style.setProperty('--progress', (1 - progress).toString());
          progressContent.textContent = `${Math.ceil(time / 1000)}s`;
        },
        slideChange(swiper) {
          setTimeout(() => {
            const slide = element.querySelector('.swiper-slide-active');
            if (slide) {
              if (swiper.isEnd) swiper.autoplay?.stop();
              onSlideChange(slide as HTMLElement);
              if (swiper.autoplay?.paused) swiper.autoplay?.resume();
            }
          }, 200);
        },
      },
    };
    const swiper = ((window as any).s = new Swiper(element, options));
    $(element).on('click', () => {
      if (swiper.autoplay && !swiper.isEnd) {
        if (swiper.autoplay.paused) swiper.autoplay.resume();
        else swiper.autoplay.pause();
      }
    });
  };

let lpvTimer: number | undefined;

function onSlideChange(slide: HTMLElement) {
  slide.querySelectorAll('.animated-number').forEach((counter: HTMLElement) => {
    animateNumber(counter, {});
  });
  slide.querySelectorAll('.animated-time').forEach((counter: HTMLElement) => {
    animateNumber(counter, { duration: 1000, render: formatDuration });
  });
  slide.querySelectorAll('.animated-pulse').forEach((counter: HTMLElement) => {
    counter.classList.remove('animated-pulse');
    setTimeout(() => {
      counter.classList.add('animated-pulse');
    }, 100);
  });
  slide.querySelectorAll('.lpv').forEach((el: HTMLElement) => {
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
}
