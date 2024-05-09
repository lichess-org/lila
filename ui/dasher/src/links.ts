import { Attrs, looseH as h, VNode, bind } from 'common/snabbdom';
import * as licon from 'common/licon';
import { Mode, DasherCtrl, PaneCtrl } from './interfaces';

export class LinksCtrl extends PaneCtrl {
  constructor(root: DasherCtrl) {
    super(root);
  }

  render = () => {
    const modeCfg = this.modeCfg,
      noarg = this.trans.noarg;
    return h('div', [
      this.userLinks(),
      h('div.subs', [
        h('button.sub', modeCfg('langs'), noarg('language')),
        h('button.sub', modeCfg('sound'), noarg('sound')),
        h('button.sub', modeCfg('background'), noarg('background')),
        h('button.sub', modeCfg('board'), noarg('board')),
        h('button.sub', modeCfg('piece'), noarg('pieceSet')),
        this.root.opts.zenable &&
          h('div.zen.selector', [
            h(
              'button.text',
              {
                attrs: { 'data-icon': licon.DiscBigOutline, title: 'Keyboard: z', type: 'button' },
                hook: bind('click', () => site.pubsub.emit('zen')),
              },
              this.trans.noarg('zenMode'),
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
      noarg = this.trans.noarg,
      linkCfg = this.linkCfg;
    return d.user
      ? h('div.links', [
          h(
            'a.user-link.online.text.is-green',
            linkCfg(`/@/${d.user.name}`, d.user.patron ? licon.Wings : licon.Disc),
            noarg('profile'),
          ),

          h('a.text', linkCfg('/inbox', licon.Envelope), noarg('inbox')),

          h(
            'a.text',
            linkCfg(
              '/account/profile',
              licon.Gear,
              this.root.opts.playing ? { target: '_blank', rel: 'noopener' } : undefined,
            ),
            noarg('preferences'),
          ),

          d.coach && h('a.text', linkCfg('/coach/edit', licon.GraduateCap), noarg('coachManager')),

          d.streamer && h('a.text', linkCfg('/streamer/edit', licon.Mic), noarg('streamerManager')),

          h('form.logout', { attrs: { method: 'post', action: '/logout' } }, [
            h('button.text', { attrs: { type: 'submit', 'data-icon': licon.Power } }, noarg('logOut')),
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
