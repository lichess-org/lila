import type { VNode } from 'snabbdom';
import * as licon from 'lib/licon';
import { domDialog } from 'lib/view/dialog';
import { confirm } from 'lib/view/dialogs';

import { bind, hl } from 'lib/snabbdom';
import type SimulCtrl from '../ctrl';
import type { Applicant } from '../interfaces';
import xhr from '../xhr';
import * as util from './util';

export default function (showText: (ctrl: SimulCtrl) => VNode | false) {
  return (ctrl: SimulCtrl) => {
    const candidates = ctrl.candidates().sort(byName),
      accepted = ctrl.accepted().sort(byName),
      isHost = ctrl.createdByMe(),
      canJoin = ctrl.data.canJoin;
    const variantIconFor = (a: Applicant) => {
      const variant = ctrl.data.variants.find(v => a.variant === v.key);
      return variant && hl('td.variant', { attrs: { 'data-icon': variant.icon } });
    };
    return [
      hl('div.box__top', [
        util.title(ctrl),
        hl(
          'div.box__top__actions',
          ctrl.opts.userId
            ? isHost
              ? [startOrCancel(ctrl, accepted), randomButton(ctrl)]
              : ctrl.containsMe()
                ? hl(
                    'a.button',
                    { hook: bind('click', () => xhr.withdraw(ctrl.data.id)) },
                    i18n.site.withdraw,
                  )
                : hl(
                    'a.button.text' + (canJoin ? '' : '.disabled'),
                    {
                      attrs: { disabled: !canJoin, 'data-icon': licon.PlayTriangle },
                      hook: canJoin
                        ? bind('click', () => {
                            if (ctrl.data.variants.length === 1)
                              xhr.join(ctrl.data.id, ctrl.data.variants[0].key);
                            else
                              domDialog({
                                cash: $('.simul .continue-with'),
                                modal: true,
                              }).then(dlg => {
                                $('button.button', dlg.view).on('click', function (this: HTMLButtonElement) {
                                  xhr.join(ctrl.data.id, this.dataset.variant as VariantKey);
                                  dlg.close();
                                });
                                dlg.show();
                              });
                          })
                        : {},
                    },
                    i18n.site.join,
                  )
            : hl(
                'a.button.text',
                {
                  attrs: {
                    'data-icon': licon.PlayTriangle,
                    href: '/login?referrer=' + window.location.pathname,
                  },
                },
                i18n.site.signIn,
              ),
        ),
      ]),
      showText(ctrl),
      ctrl.acceptedContainsMe()
        ? hl('p.instructions', 'You have been selected! Hold still, the simul is about to begin.')
        : isHost &&
          ctrl.data.applicants.length < 6 &&
          hl('p.instructions', 'Share this page URL to let people enter the simul!'),
      hl(
        'div.halves',
        { hook: { postpatch: (_old, vnode) => site.powertip.manualUserIn(vnode.elm as HTMLElement) } },
        [
          hl(
            'div.half.candidates',
            hl(
              'table.slist.slist-pad',
              hl(
                'thead',
                hl(
                  'tr',
                  hl('th', { attrs: { colspan: 3 } }, [
                    hl('strong', `${candidates.length}`),
                    ' candidate players',
                  ]),
                ),
              ),
              hl(
                'tbody',
                candidates.map(applicant => {
                  return hl(
                    'tr',
                    { key: applicant.player.id, class: { me: ctrl.opts.userId === applicant.player.id } },
                    [
                      hl('td', util.player(applicant.player, ctrl)),
                      variantIconFor(applicant),
                      hl(
                        'td.action',
                        isHost &&
                          hl('a.button', {
                            attrs: { 'data-icon': licon.Checkmark, title: 'Accept' },
                            hook: bind('click', () => xhr.accept(applicant.player.id)(ctrl.data.id)),
                          }),
                      ),
                    ],
                  );
                }),
              ),
            ),
          ),
          hl('div.half.accepted', [
            hl(
              'table.slist.user_list',
              hl('thead', [
                hl(
                  'tr',
                  hl('th', { attrs: { colspan: 3 } }, [
                    hl('strong', `${accepted.length}`),
                    ' accepted players',
                  ]),
                ),
                isHost &&
                  candidates.length > 0 &&
                  !accepted.length &&
                  hl('tr.help', hl('th', 'Now you get to accept some players, then start the simul')),
              ]),
              hl(
                'tbody',
                accepted.map(applicant => {
                  return hl(
                    'tr',
                    { key: applicant.player.id, class: { me: ctrl.opts.userId === applicant.player.id } },
                    [
                      hl('td', util.player(applicant.player, ctrl)),
                      variantIconFor(applicant),
                      hl(
                        'td.action',
                        isHost &&
                          hl('a.button.button-red', {
                            attrs: { 'data-icon': licon.X },
                            hook: bind('click', () => xhr.reject(applicant.player.id)(ctrl.data.id)),
                          }),
                      ),
                    ],
                  );
                }),
              ),
            ),
          ]),
        ],
      ),
      ctrl.data.quote &&
        hl('blockquote.pull-quote', [hl('p', ctrl.data.quote.text), hl('footer', ctrl.data.quote.author)]),
      hl(
        'div.continue-with.none',
        ctrl.data.variants.map(variant =>
          hl('button.button', { attrs: { 'data-variant': variant.key } }, variant.name),
        ),
      ),
    ];
  };
}

const byName = (a: Applicant, b: Applicant) => (a.player.name > b.player.name ? 1 : -1);

const randomButton = (ctrl: SimulCtrl) =>
  ctrl.candidates().length > 0 &&
  hl(
    'a.button.text',
    {
      attrs: { 'data-icon': licon.Checkmark },
      hook: bind('click', () => {
        const candidates = ctrl.candidates();
        const randomCandidate = candidates[Math.floor(Math.random() * candidates.length)];
        xhr.accept(randomCandidate.player.id)(ctrl.data.id);
      }),
    },
    'Accept random candidate',
  );

const startOrCancel = (ctrl: SimulCtrl, accepted: Applicant[]) =>
  accepted.length > 1
    ? hl(
        'a.button.button-green.text',
        { attrs: { 'data-icon': licon.PlayTriangle }, hook: bind('click', () => xhr.start(ctrl.data.id)) },
        `Start (${accepted.length})`,
      )
    : hl(
        'a.button.button-red.text',
        {
          attrs: { 'data-icon': licon.X },
          hook: bind('click', async () => {
            if (await confirm('Delete this simul?')) xhr.abort(ctrl.data.id);
          }),
        },
        i18n.site.cancel,
      );
