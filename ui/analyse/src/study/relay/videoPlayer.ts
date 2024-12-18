import { looseH as h, type Redraw, type VNode, onInsert } from 'common/snabbdom';
import { allowVideo } from './relayView';

export class VideoPlayer {
  private iframe: HTMLIFrameElement;
  private close: HTMLImageElement;
  private animationFrameId?: number;

  constructor(
    private o: { embed: string | false; redirect?: string; image?: string; text?: string },
    private redraw: Redraw,
  ) {
    if (!o.embed) return;

    this.iframe = document.createElement('iframe');
    this.iframe.setAttribute('credentialless', '');
    this.iframe.style.display = 'none';
    this.iframe.id = 'video-player';
    this.iframe.src = o.embed;
    this.iframe.allow = 'autoplay';

    this.close = document.createElement('img');
    this.close.src = site.asset.flairSrc('symbols.cancel');
    this.close.className = 'video-player-close';
    this.close.addEventListener('click', () => this.onEmbed('no'), true);

    this.addWindowResizer();
  }

  cover = (el?: HTMLElement) => {
    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId);
    }
    this.animationFrameId = requestAnimationFrame(() => {
      if (!el) {
        this.iframe.remove();
        this.close.remove();
        return;
      }
      this.iframe.style.display = 'block';
      this.iframe.style.left = `${el.offsetLeft}px`;
      this.iframe.style.top = `${el.offsetTop}px`;
      this.iframe.style.width = `${el.offsetWidth}px`;
      this.iframe.style.height = `${el.offsetHeight}px`;
      this.close.style.left = `${el.offsetLeft + el!.offsetWidth - 16}px`;
      this.close.style.top = `${el.offsetTop - 4}px`;
      if (document.body.contains(this.iframe)) return;
      document.body.appendChild(this.iframe);
      document.body.appendChild(this.close);
    });
  };

  addWindowResizer = () => {
    let showingVideo = false;
    window.addEventListener(
      'resize',
      () => {
        const allow = allowVideo();
        const placeholder = document.getElementById('video-player-placeholder') ?? undefined;
        this.cover(allow ? placeholder : undefined);
        if (showingVideo === allow && !!placeholder) return;
        showingVideo = allow && !!placeholder;
        this.redraw();
      },
      { passive: true },
    );
  };

  render = () => {
    return this.o.embed
      ? h('div#video-player-placeholder', {
          hook: {
            insert: (vnode: VNode) => this.cover(vnode.elm as HTMLElement),
            update: (_, vnode: VNode) => this.cover(vnode.elm as HTMLElement),
          },
        })
      : h('div#video-player-placeholder.link', {}, [
          h('img.image', {
            attrs: { src: this.o.image! },
            hook: onInsert((el: HTMLElement) => {
              el.addEventListener('click', e => {
                if (e.ctrlKey || e.shiftKey) window.open(this.o.redirect, '_blank');
                else this.onEmbed('ps');
              });
              el.addEventListener('contextmenu', () => window.open(this.o.redirect, '_blank'));
            }),
          }),
          h('img.video-player-close', {
            attrs: { src: site.asset.flairSrc('symbols.cancel') },
            hook: onInsert((el: HTMLElement) => el.addEventListener('click', () => this.onEmbed('no'))),
          }),
          this.o.text && h('div.text', h('div', this.o.text)),
          h(
            'svg.play-button',
            {
              attrs: {
                xmlns: 'http://www.w3.org/2000/svg',
                viewBox: '0 0 100 100',
                width: '100',
                height: '100',
              },
            },
            [
              h('circle', {
                attrs: {
                  cx: '50',
                  cy: '50',
                  r: '40',
                  stroke: '#666',
                  'stroke-width': '10',
                  fill: '#dddd',
                },
              }),
              h('path', {
                attrs: {
                  d: 'M 32 28 A 5 5 0 0 1 37 23 L 75 45 A 6 6 0 0 1 75 55 L 37 77 A 5 5 0 0 1 32 72 Z',
                  fill: '#666',
                },
              }),
            ],
          ),
        ]);
  };

  onEmbed = (stream: 'ps' | 'no') => {
    const urlWithEmbed = new URL(location.href);
    urlWithEmbed.searchParams.set('embed', stream);
    window.location.href = urlWithEmbed.toString();
  };
}
