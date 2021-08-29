function shuffle<T>(a: T[]) {
  let j, x, i;
  for (i = a.length; i; i -= 1) {
    j = Math.floor(Math.random() * i);
    x = a[i - 1];
    a[i - 1] = a[j];
    a[j] = x;
  }
}

const list = [
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

let it = 0;

export default function () {
  return list[it++ % list.length];
}
