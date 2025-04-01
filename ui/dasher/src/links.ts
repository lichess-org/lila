import { type Attrs, looseH as h, type VNode, bind, onInsert } from 'lib/snabbdom';
import * as licon from 'lib/licon';
import { type Mode, type DasherCtrl, PaneCtrl } from './interfaces';
import { pubsub } from 'lib/pubsub';

export class LinksCtrl extends PaneCtrl {
  private pointerDown: number = performance.now();

  constructor(root: DasherCtrl) {
    super(root);
  }

  render = (): VNode => {
    const modeCfg = this.modeCfg;
    return h('div', [
      this.userLinks(),
      h('div.subs', [
        h('button.sub', modeCfg('langs'), i18n.site.language),
        h('button.sub', modeCfg('sound'), i18n.site.sound),
        h('button.sub', modeCfg('background'), i18n.site.background),
        h('button.sub', modeCfg('board'), i18n.site.board),
        h('button.sub', modeCfg('piece'), i18n.site.pieceSet),
        this.root.opts.zenable &&
          h('div.zen.selector', [
            h(
              'button.text',
              {
                attrs: { 'data-icon': licon.DiscBigOutline, title: 'Keyboard: z', type: 'button' },
                hook: bind('click', () => pubsub.emit('zen')),
              },
              i18n.preferences.zenMode,
            ),
          ]),
      ]),
      this.root.ping.render(),
    ]);
  };

  private get data() {
    return this.root.data;
  }

  private userLinks(): VNode | null {
    const d = this.data,
      linkCfg = this.linkCfg;
    return d.user
      ? h('div.links', [
          h(
            'a.user-link.online.text.is-green',
            linkCfg(`/@/${d.user.name}`, d.user.patron ? licon.Wings : licon.Disc),
            i18n.site.profile,
          ),

          h('a.text', linkCfg('/inbox', licon.Envelope), i18n.site.inbox),

          h(
            'a.text',
            linkCfg(
              '/account/profile',
              licon.Gear,
              this.root.opts.playing ? { target: '_blank' } : undefined,
            ),
            i18n.preferences.preferences,
          ),

          d.coach && h('a.text', linkCfg('/coach/edit', licon.GraduateCap), i18n.site.coachManager),

          d.streamer && h('a.text', linkCfg('/streamer/edit', licon.Mic), i18n.site.streamerManager),

          h('form.logout', { attrs: { method: 'post', action: '/logout' } }, [
            h('button.text', { attrs: { type: 'submit', 'data-icon': licon.Power } }, i18n.site.logOut),
          ]),
        ])
      : null;
  }

  private modeCfg = (m: Mode): any => ({
    hook: onInsert(el => {
      el.addEventListener('pointerdown', () => (this.pointerDown = performance.now()));
      el.addEventListener('pointerup', e => {
        this.root.setMode(m, performance.now() - this.pointerDown > 500);
        e.preventDefault();
      });
      el.addEventListener('click', () => this.root.setMode(m)); // compat, accessibility, etc
    }),
    attrs: { 'data-icon': licon.GreaterThan, type: 'button' },
  });

  private linkCfg = (href: string, icon: string, more?: Attrs) => ({
    attrs: { href, 'data-icon': icon, ...(more || {}) },
  });
}
