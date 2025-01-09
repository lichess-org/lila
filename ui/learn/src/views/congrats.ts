import { i18n } from 'i18n';

const list = [
  i18n('learn:awesome'),
  i18n('learn:excellent'),
  i18n('learn:greatJob'),
  i18n('learn:perfect'),
  i18n('learn:outstanding'),
  i18n('learn:wayToGo'),
  i18n('learn:yesYesYes'),
  i18n('learn:youreGoodAtThis'),
  i18n('learn:nailedIt'),
  i18n('learn:rightOn'),
];

export default function (): string {
  return list[Math.floor(Math.random() * list.length)];
}
