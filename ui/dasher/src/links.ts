import { icons, type Icon } from 'lib/icons';
import { pubsub } from 'lib/pubsub';
import { type Attrs, hl, type VNode, bind, snabIcon } from 'lib/view';
import { userLine } from 'lib/view/userLink';

import { type Mode, PaneCtrl } from './interfaces';

export class LinksCtrl extends PaneCtrl {
  render = (): VNode => {
    return hl('div', [
      this.userLinks(),
      hl('div.subs', [
        this.modeButton('langs', i18n.site.language, icons.Language),
        this.modeButton('sound', i18n.site.sound),
        this.modeButton('theme', i18n.site.theme),
        this.modeButton('board', i18n.site.board),
        this.modeButton('piece', i18n.site.pieceSet),
        this.root.opts.zenable &&
          hl('div.zen.selector', [
            hl(
              'button.text',
              {
                attrs: { title: 'Keyboard: z', type: 'button' },
                hook: bind('click', () => pubsub.emit('zen')),
              },
              [snabIcon(icons.DiscBigOutline), i18n.preferences.zenMode],
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
    const d = this.data;
    return d.user
      ? hl('div.links', [
          hl('a.user-link.online', { attrs: { href: `/@/${d.user.name}` } }, [
            userLine(d.user),
            i18n.site.profile,
          ]),

          this.link('/inbox', icons.Envelope, i18n.site.inbox),

          this.link(
            '/account/profile',
            icons.Gear,
            i18n.preferences.preferences,
            this.root.opts.playing ? { target: '_blank' } : undefined,
          ),

          d.coach && this.link('/coach/edit', icons.GraduateCap, i18n.site.coachManager),

          d.streamer && this.link('/streamer/edit', icons.Mic, i18n.site.streamerManager),

          hl('form.logout', { attrs: { method: 'post', action: '/logout' } }, [
            hl('button.text', { attrs: { type: 'submit' } }, [snabIcon(icons.Power), i18n.site.logOut]),
          ]),
        ])
      : null;
  }

  private readonly modeButton = (mode: Mode, label: string, icon?: Icon) =>
    hl('button.sub', { hook: bind('click', () => this.root.setMode(mode)), attrs: { type: 'button' } }, [
      icon && snabIcon(icon),
      label,
      snabIcon(icons.GreaterThan, 'mirror-rtl'),
    ]);

  private readonly link = (href: string, icon: Icon, label: string, more?: Attrs) =>
    hl('a.text', { attrs: { href, ...more } }, [snabIcon(icon), label]);
}
