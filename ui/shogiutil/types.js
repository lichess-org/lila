"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.isNormal = exports.isDrop = void 0;
function isDrop(v) {
    return "role" in v;
}
exports.isDrop = isDrop;
function isNormal(v) {
    return "from" in v;
}
exports.isNormal = isNormal;
//# sourceMappingURL=types.js.map