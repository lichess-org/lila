"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var xhr = require("./xhr");
function default_1(opts, redraw) {
    var data;
    var initiating = true;
    var reloading = false;
    var trans = function (key) { return key; };
    function update(d) {
        data = d;
        if (d.i18n)
            trans = window.lichess.trans(d.i18n);
        initiating = false;
        reloading = false;
        opts.setCount(countActiveIn());
        notifyNew();
        redraw();
    }
    function countActiveIn() {
        return data ? data.in.filter(function (c) { return !c.declined; }).length : 0;
    }
    function notifyNew() {
        data && data.in.forEach(function (c) {
            if (window.lichess.once('c-' + c.id)) {
                if (!window.lichess.quietMode) {
                    opts.show();
                    window.lichess.sound.newChallenge();
                }
                c.challenger && window.lichess.desktopNotification(showUser(c.challenger) + ' challenges you!');
                opts.pulse();
            }
        });
    }
    function showUser(user) {
        var rating = user.rating + (user.provisional ? '?' : '');
        var fullName = (user.title ? user.title + ' ' : '') + user.name;
        return fullName + ' (' + rating + ')';
    }
    return {
        data: function () { return data; },
        initiating: function () { return initiating; },
        reloading: function () { return reloading; },
        trans: trans,
        update: update,
        decline: function (id) {
            data && data.in.forEach(function (c) {
                if (c.id === id) {
                    c.declined = true;
                    xhr.decline(id);
                }
            });
        },
        cancel: function (id) {
            data && data.out.forEach(function (c) {
                if (c.id === id) {
                    c.declined = true;
                    xhr.cancel(id);
                }
            });
        }
    };
}
exports.default = default_1;
;
