const list: I18nKey[] = [
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

export default function () {
  return list[Math.floor(Math.random() * list.length)];
}
