import { type Icon } from 'lib/icons';
import { pubsub } from 'lib/pubsub';
import { type Attrs, hl, type VNode, bind, snabIcon } from 'lib/view';
import { userLine, profileUrl } from 'lib/view/userLink';

import { type Mode, PaneCtrl } from './interfaces';

export class LinksCtrl extends PaneCtrl {
  render = (): VNode => {
    return hl('div', [
      this.userLinks(),
      hl('div.subs', [
        this.modeButton('langs', i18n.site.language, 'language'),
        this.modeButton('sound', i18n.site.sound),
        this.modeButton('theme', i18n.site.theme),
        this.modeButton('board', i18n.site.board),
        this.modeButton('piece', i18n.site.pieceSet),
        this.root.opts.zenable &&
          hl('div.zen.selector', [
            hl(
              'button',
              {
                attrs: { title: 'Keyboard: z', type: 'button' },
                hook: bind('click', () => pubsub.emit('zen')),
              },
              [snabIcon('discBigOutline'), i18n.preferences.zenMode],
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
          hl('a.user-link.online', { attrs: { href: profileUrl(d.user.name) } }, [
            userLine(d.user),
            i18n.site.profile,
          ]),

          this.link('/inbox', 'envelope', i18n.site.inbox),

          this.link(
            '/account/profile',
            'gear',
            i18n.preferences.preferences,
            this.root.opts.playing ? { target: '_blank' } : undefined,
          ),

          d.coach && this.link('/coach/edit', 'graduateCap', i18n.site.coachManager),

          d.streamer && this.link('/streamer/edit', 'mic', i18n.site.streamerManager),

          hl('form.logout', { attrs: { method: 'post', action: '/logout' } }, [
            hl('button', { attrs: { type: 'submit' } }, [snabIcon('power'), i18n.site.logOut]),
          ]),
        ])
      : null;
  }

  private readonly modeButton = (mode: Mode, label: string, icon?: Icon) =>
    hl('button.sub', { hook: bind('click', () => this.root.setMode(mode)), attrs: { type: 'button' } }, [
      icon && snabIcon(icon),
      label,
      snabIcon('greaterThan'),
    ]);

  private readonly link = (href: string, icon: Icon, label: string, more?: Attrs) =>
    hl('a', { attrs: { href, ...more } }, [snabIcon(icon), label]);
}
