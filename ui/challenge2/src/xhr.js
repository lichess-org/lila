"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
function load() {
    return $.get('/challenge');
}
exports.load = load;
function decline(id) {
    return $.post("/challenge/" + id + "/decline");
}
exports.decline = decline;
function cancel(id) {
    return $.post("/challenge/" + id + "/cancel");
}
exports.cancel = cancel;
