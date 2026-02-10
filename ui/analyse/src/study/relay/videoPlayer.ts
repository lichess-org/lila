import { hl, type VNode, onInsert } from 'lib/view';
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

  cover = (placeholder?: HTMLElement) => {
    if (this.animationFrameId) cancelAnimationFrame(this.animationFrameId);
    this.animationFrameId = requestAnimationFrame(() => {
      if (!placeholder) {
        this.iframe.remove();
        this.close.remove();
        return;
      }
      const position = placeholder.getBoundingClientRect();
      this.iframe.style.display = 'block';
      this.iframe.style.left = `${position.x}px`;
      this.iframe.style.top = `${position.y + window.scrollY}px`;
      this.iframe.style.width = `${position.width}px`;
      this.iframe.style.height = `${position.height}px`;
      this.close.style.left = `${position.x + position.width - 16}px`;
      this.close.style.top = `${position.y + window.scrollY - 4}px`;
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
      ? hl('div#video-player-placeholder', {
          hook: {
            insert: (vnode: VNode) => this.cover(vnode.elm as HTMLElement),
            update: (_, vnode: VNode) => this.cover(vnode.elm as HTMLElement),
          },
        })
      : hl('div#video-player-placeholder.link', [
          hl('div.image', {
            attrs: { style: `background-image: url(${this.o.image})` },
            hook: onInsert((el: HTMLElement) => {
              el.addEventListener('click', e => {
                if (e.ctrlKey || e.shiftKey) window.open(this.o.redirect, '_blank');
                else this.onEmbed('ps');
              });
              el.addEventListener('contextmenu', () => window.open(this.o.redirect, '_blank'));
            }),
          }),
          hl('img.video-player-close', {
            attrs: { src: site.asset.flairSrc('symbols.cancel') },
            hook: onInsert((el: HTMLElement) => el.addEventListener('click', () => this.onEmbed('no'))),
          }),
          this.o.text && hl('div.text-box', hl('div', this.o.text)),
          hl(
            'svg.play-button',
            {
              attrs: {
                xmlns: 'http://www.w3.org/2000/svg',
                viewBox: '0 0 200 200',
              },
            },
            [
              hl('circle', {
                attrs: {
                  cx: '100',
                  cy: '100',
                  r: '90',
                },
              }),
              hl('path', {
                attrs: {
                  d: 'M 68 52 A 5 5 0 0 1 74 46 L 154 96 A 5 5 0 0 1 154 104 L 74 154 A 5 5 0 0 1 68 148 Z',
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
