/// <reference types="types/lichess" />
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var snabbdom_1 = require("snabbdom");
var ctrl_1 = require("./ctrl");
var view_1 = require("./view");
var xhr_1 = require("./xhr");
var class_1 = require("snabbdom/modules/class");
var attributes_1 = require("snabbdom/modules/attributes");
var patch = snabbdom_1.init([class_1.default, attributes_1.default]);
function LichessChat(element, opts) {
    var vnode, ctrl;
    function redraw() {
        vnode = patch(vnode, view_1.default(ctrl));
    }
    ctrl = ctrl_1.default(opts, redraw);
    vnode = patch(element, view_1.default(ctrl));
    if (opts.data)
        ctrl.update(opts.data);
    else
        xhr_1.load().then(ctrl.update);
    return {
        update: ctrl.update
    };
}
exports.default = LichessChat;
;
