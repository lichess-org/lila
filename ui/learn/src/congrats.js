function shuffle(a) {
  var j, x, i;
  for (i = a.length; i; i -= 1) {
    j = Math.floor(Math.random() * i);
    x = a[i - 1];
    a[i - 1] = a[j];
    a[j] = x;
  }
}

var list = [
  'awesome',
  'excellent',
  'greatJob',
  'perfect',
  'outstanding',
  'wayToGo',
  'yesYesYes',
  'youreGoodAtThis',
  'nailedIt',
  'rightOn',
];
shuffle(list);

var it = 0;

module.exports = function () {
  return list[it++ % list.length];
};
