function shuffle<T>(a: T[]) {
  for (let i = a.length; i; i -= 1) {
    const j = Math.floor(Math.random() * i);
    const x = a[i - 1];
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

export default () => list[it++ % list.length];
