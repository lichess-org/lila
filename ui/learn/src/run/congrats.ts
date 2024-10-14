function shuffle<T>(a: T[]) {
  for (let i = a.length; i; i -= 1) {
    const j = Math.floor(Math.random() * i);
    const x = a[i - 1];
    a[i - 1] = a[j];
    a[j] = x;
  }
}

const list: string[] = [
  i18n.learn.awesome,
  i18n.learn.excellent,
  i18n.learn.greatJob,
  i18n.learn.perfect,
  i18n.learn.outstanding,
  i18n.learn.wayToGo,
  i18n.learn.yesYesYes,
  i18n.learn.youreGoodAtThis,
  i18n.learn.nailedIt,
  i18n.learn.rightOn,
];
shuffle(list);

let it = 0;

export default () => list[it++ % list.length];
