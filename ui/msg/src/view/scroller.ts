import throttle from 'common/throttle';

class Scroller {
  enabled: boolean = false;
  element?: HTMLElement;
  marker?: HTMLElement;

  init = (e: HTMLElement) => {
    this.enabled = true;
    this.element = e;
    this.element.addEventListener(
      'scroll',
      throttle(500, _ => {
        const el = this.element;
        this.enable(!!el && el.offsetHeight + el.scrollTop > el.scrollHeight - 20);
      }),
      { passive: true }
    );
    window.el = this.element;
  };
  auto = () => {
    if (this.element && this.enabled) requestAnimationFrame(() => (this.element!.scrollTop = 9999999));
  };
  enable = (v: boolean) => {
    this.enabled = v;
  };
  setMarker = () => {
    this.marker = this.element && (this.element.querySelector('mine,their') as HTMLElement);
  };
  toMarker = (): boolean => {
    if (this.marker && this.to(this.marker)) {
      this.marker = undefined;
      return true;
    }
    return false;
  };
  to = (target: HTMLElement) => {
    if (this.element) {
      const top = target.offsetTop - this.element.offsetHeight / 2 + target.offsetHeight / 2;
      if (top > 0) this.element.scrollTop = top;
      return top > 0;
    }
    return false;
  };
}

export const scroller = new Scroller();
