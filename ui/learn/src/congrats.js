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
  'Awesome!',
  'Excellent!',
  'Great job!',
  'Perfect!',
  'Outstanding!',
  'Way to go!',
  'Yes, yes, yes!',
  'You\'re good at this!'
];
shuffle(list);

var it = 0;

module.exports = function() {
  return list[it++];
};
