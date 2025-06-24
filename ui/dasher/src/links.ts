import { type Attrs, hl, type VNode, bind } from 'lib/snabbdom';
import * as licon from 'lib/licon';
import { type Mode, type DasherCtrl, PaneCtrl } from './interfaces';
import { pubsub } from 'lib/pubsub';

export class LinksCtrl extends PaneCtrl {
  constructor(root: DasherCtrl) {
    super(root);
  }

  render = (): VNode => {
    const modeCfg = this.modeCfg;
    return hl('div', [
      this.userLinks(),
      hl('div.subs', [
        hl('button.sub', modeCfg('langs'), i18n.site.language),
        hl('button.sub', modeCfg('sound'), i18n.site.sound),
        hl('button.sub', modeCfg('background'), i18n.site.background),
        hl('button.sub', modeCfg('board'), i18n.site.board),
        hl('button.sub', modeCfg('piece'), i18n.site.pieceSet),
        this.root.opts.zenable &&
          hl('div.zen.selector', [
            hl(
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
      ? hl('div.links', [
          hl(
            'a.user-link.online.text.is-green',
            linkCfg(`/@/${d.user.name}`, d.user.patron ? licon.Wings : licon.Disc),
            i18n.site.profile,
          ),

          hl('a.text', linkCfg('/inbox', licon.Envelope), i18n.site.inbox),

          hl(
            'a.text',
            linkCfg(
              '/account/profile',
              licon.Gear,
              this.root.opts.playing ? { target: '_blank' } : undefined,
            ),
            i18n.preferences.preferences,
          ),

          d.coach && hl('a.text', linkCfg('/coach/edit', licon.GraduateCap), i18n.site.coachManager),

          d.streamer && hl('a.text', linkCfg('/streamer/edit', licon.Mic), i18n.site.streamerManager),

          hl('form.logout', { attrs: { method: 'post', action: '/logout' } }, [
            hl('button.text', { attrs: { type: 'submit', 'data-icon': licon.Power } }, i18n.site.logOut),
          ]),
        ])
      : null;
  }

  private modeCfg = (m: Mode): any => ({
    hook: bind('click', () => this.root.setMode(m)),
    attrs: { 'data-icon': licon.GreaterThan, type: 'button' },
  });

  private linkCfg = (href: string, icon: string, more?: Attrs) => ({
    attrs: { href, 'data-icon': icon, ...(more || {}) },
  });
}
