"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const snabbdom_1 = require("snabbdom");
function instance() {
    return {
        button() {
            return snabbdom_1.h('button.palantir', {
                attrs: { 'data-icon': '' }
            });
        }
    };
}
exports.instance = instance;
