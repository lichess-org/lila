"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var snabbdom_1 = require("snabbdom");
function default_1(ctrl) {
    var d = ctrl.data();
    return snabbdom_1.h('div#challenge_app.links.dropdown', d && !ctrl.initiating() ? renderContent(ctrl, d) : [snabbdom_1.h('div.initiating', spinner())]);
}
exports.default = default_1;
function renderContent(ctrl, d) {
    var nb = d.in.length + d.out.length;
    return nb ? allChallenges(ctrl, d, nb) : empty();
}
function userPowertips(vnode) {
    window.lichess.powertip.manualUserIn(vnode.elm);
}
function allChallenges(ctrl, d, nb) {
    return snabbdom_1.h('div', {
        key: 'all',
        class: {
            challenges: true,
            reloading: ctrl.reloading(),
            many: nb > 3
        },
        hook: {
            insert: userPowertips,
            postpatch: userPowertips
        }
    }, d.in.map(challenge(ctrl, 'in')).concat(d.out.map(challenge(ctrl, 'out'))));
}
function challenge(ctrl, dir) {
    return function (c) {
        return snabbdom_1.h('div', {
            key: c.id,
            class: (_a = {
                    challenge: true
                },
                _a[dir] = true,
                _a.declined = c.declined,
                _a)
        }, [
            snabbdom_1.h('div.content', [
                snabbdom_1.h('span.title', renderUser(dir === 'in' ? c.challenger : c.destUser)),
                snabbdom_1.h('span.desc', [
                    window.lichess.globalTrans(c.rated ? 'Rated' : 'Casual'),
                    timeControl(c.timeControl),
                    c.variant.name
                ].join(' • '))
            ]),
            snabbdom_1.h('i', {
                attrs: { 'data-icon': c.perf.icon }
            }),
            snabbdom_1.h('div.buttons', (dir === 'in' ? inButtons : outButtons)(ctrl, c))
        ]);
        var _a;
    };
}
function inButtons(ctrl, c) {
    return [
        snabbdom_1.h('form', {
            attrs: {
                method: 'post',
                action: "/challenge/" + c.id + "/accept"
            }
        }, [
            snabbdom_1.h('button.button.accept', {
                attrs: {
                    'type': 'submit',
                    'data-icon': 'E'
                }
            })
        ]),
        snabbdom_1.h('button.submit.button.decline', {
            attrs: {
                'type': 'submit',
                'data-icon': 'L'
            },
            hook: {
                insert: function (vnode) {
                    vnode.elm.addEventListener('click', function () { return ctrl.decline(c.id); });
                }
            }
        })
    ];
}
function outButtons(ctrl, c) {
    return [
        snabbdom_1.h('div.owner', [
            snabbdom_1.h('span.waiting', ctrl.trans('waiting')),
            snabbdom_1.h('a.view', {
                attrs: {
                    'data-icon': 'v',
                    href: '/' + c.id
                }
            })
        ]),
        snabbdom_1.h('button.button.decline', {
            attrs: { 'data-icon': 'L' },
            hook: {
                insert: function (vnode) {
                    vnode.elm.addEventListener('click', function () { return ctrl.cancel(c.id); });
                }
            }
        })
    ];
}
function timeControl(c) {
    switch (c.type) {
        case 'unlimited':
            return 'Unlimited';
        case 'correspondence':
            return c.daysPerTurn + ' days';
        case 'clock':
            return c.show || '-';
    }
}
function renderUser(u) {
    if (!u)
        return snabbdom_1.h('span', 'Open challenge');
    var rating = u.rating + (u.provisional ? '?' : '');
    return snabbdom_1.h('a', {
        attrs: { href: "/@/" + u.name },
        class: {
            ulpt: true,
            user_link: true,
            online: u.online
        }
    }, [
        snabbdom_1.h('i.line' + (u.patron ? '.patron' : '')),
        snabbdom_1.h('name', (u.title ? u.title + ' ' : '') + u.name + ' (' + rating + ')')
    ]);
}
function empty() {
    return snabbdom_1.h('div.empty.text', {
        key: 'empty',
        attrs: {
            'data-icon': '',
        }
    }, 'No challenges.');
}
function spinner() {
    return snabbdom_1.h('div.spinner', [
        snabbdom_1.h('svg', { attrs: { viewBox: '0 0 40 40' } }, [
            snabbdom_1.h('circle', {
                attrs: { cx: 20, cy: 20, r: 18, fill: 'none' }
            })
        ])
    ]);
}
