var files = "abcdefghi".split("");
var ranks = [1, 2, 3, 4, 5, 6, 7, 8, 9];
var invRanks = [9, 8, 7, 6, 5, 4, 3, 2, 1];
var fileNumbers = {
  a: 1,
  b: 2,
  c: 3,
  d: 4,
  e: 5,
  f: 6,
  g: 7,
  h: 8,
  i: 9,
};

function pos2key(pos) {
  return files[pos[0] - 1] + pos[1];
}

function key2pos(pos) {
  return [fileNumbers[pos[0]], parseInt(pos[1])];
}

function invertKey(key) {
  return files[9 - fileNumbers[key[0]]] + (10 - parseInt(key[1]));
}

var allPos = (function () {
  var ps = [];
  invRanks.forEach(function (y) {
    ranks.forEach(function (x) {
      ps.push([x, y]);
    });
  });
  return ps;
})();
var allKeys = allPos.map(pos2key);
var invKeys = allKeys.slice(0).reverse();

function opposite(color) {
  return color === "white" ? "black" : "white";
}

function containsX(xs, x) {
  return xs && xs.indexOf(x) !== -1;
}

function distance(pos1, pos2) {
  return Math.sqrt(
    Math.pow(pos1[0] - pos2[0], 2) + Math.pow(pos1[1] - pos2[1], 2)
  );
}

// this must be cached because of the access to document.body.style
var cachedTransformProp;

function computeTransformProp() {
  return "transform" in document.body.style
    ? "transform"
    : "webkitTransform" in document.body.style
    ? "webkitTransform"
    : "mozTransform" in document.body.style
    ? "mozTransform"
    : "oTransform" in document.body.style
    ? "oTransform"
    : "msTransform";
}

function transformProp() {
  if (!cachedTransformProp) cachedTransformProp = computeTransformProp();
  return cachedTransformProp;
}

var cachedIsTrident = null;

function isTrident() {
  if (cachedIsTrident === null)
    cachedIsTrident = window.navigator.userAgent.indexOf("Trident/") > -1;
  return cachedIsTrident;
}

function translate(pos) {
  return "translate(" + pos[0] + "px," + pos[1] + "px)";
}

function eventPosition(e) {
  if (e.clientX || e.clientX === 0) return [e.clientX, e.clientY];
  if (e.touches && e.targetTouches[0])
    return [e.targetTouches[0].clientX, e.targetTouches[0].clientY];
}

function partialApply(fn, args) {
  return fn.bind.apply(fn, [null].concat(args));
}

function partial() {
  return partialApply(arguments[0], Array.prototype.slice.call(arguments, 1));
}

function isRightButton(e) {
  return e.buttons === 2 || e.button === 2;
}

function memo(f) {
  var v,
    ret = function () {
      if (v === undefined) v = f();
      return v;
    };
  ret.clear = function () {
    v = undefined;
  };
  return ret;
}

module.exports = {
  files: files,
  ranks: ranks,
  invRanks: invRanks,
  allPos: allPos,
  allKeys: allKeys,
  invKeys: invKeys,
  pos2key: pos2key,
  key2pos: key2pos,
  invertKey: invertKey,
  opposite: opposite,
  translate: translate,
  containsX: containsX,
  distance: distance,
  eventPosition: eventPosition,
  partialApply: partialApply,
  partial: partial,
  transformProp: transformProp,
  isTrident: isTrident,
  requestAnimationFrame: (
    window.requestAnimationFrame || window.setTimeout
  ).bind(window),
  isRightButton: isRightButton,
  memo: memo,
};
