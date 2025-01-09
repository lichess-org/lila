// https://github.com/lichess-org/lila/blob/32386c7bfd7bdd617d30a2af2838ec3c687edb6d/ui/.build/src/i18n.ts
export function quantity(lang: string): string {
  switch (lang.split('-')[0]) {
    case 'fr':
    case 'ff':
    case 'kab':
    case 'co':
    case 'ak':
    case 'am':
    case 'bh':
    case 'fil':
    case 'tl':
    case 'guw':
    case 'hi':
    case 'ln':
    case 'mg':
    case 'nso':
    case 'ti':
    case 'wa':
      return `o=>o<=1?"one":"other"`; // french
    case 'cs':
    case 'sk':
      return `o=>1==o?"one":o>=2&&o<=4?"few":"other"`; // czech
    case 'hr':
    case 'ru':
    case 'sr':
    case 'uk':
    case 'be':
    case 'bs':
    case 'sh':
    case 'ry': // balkan
      return `o=>{const e=o%100,t=o%10;return 1==t&&11!=e?"one":t>=2&&t<=4&&!(e>=12&&e<=14)?"few":0==t||t>=5&&t<=9||e>=11&&e<=14?"many":"other"}`;
    case 'lv': // latvian
      return `o=>0==o?"zero":o%10==1&&o%100!=11?"one":"other"`;
    case 'lt': // lithuanian
      return `o=>{const e=o%100,t=o%10;return 1!=t||e>=11&&e<=19?t>=2&&t<=9&&!(e>=11&&e<=19)?"few":"other":"one"}`;
    case 'pl': // polish
      return `o=>{const e=o%100,t=o%10;return 1==o?"one":t>=2&&t<=4&&!(e>=12&&e<=14)?"few":"other"}`;
    case 'ro':
    case 'mo': // romanian
      return `o=>{const e=o%100;return 1==o?"one":0==o||e>=1&&e<=19?"few":"other"}`;
    case 'sl': // slovenian
      return `o=>{const e=o%100;return 1==e?"one":2==e?"two":e>=3&&e<=4?"few":"other"}`;
    case 'ar': // arabic
      return `o=>{const e=o%100;return 0==o?"zero":1==o?"one":2==o?"two":e>=3&&e<=10?"few":e>=11&&e<=99?"many":"other"}`;
    case 'mk': // macedonian
      return `o=>o%10==1&&11!=o?"one":"other"`;
    case 'cy':
    case 'br': // welsh
      return `o=>0==o?"zero":1==o?"one":2==o?"two":3==o?"few":6==o?"many":"other"`;
    case 'mt': // maltese
      return `o=>{const e=o%100;return 1==o?"one":0==o||e>=2&&e<=10?"few":e>=11&&e<=19?"many":"other"}`;
    case 'ga':
    case 'se':
    case 'sma':
    case 'smi':
    case 'smj':
    case 'smn':
    case 'sms':
      return `o=>1==o?"one":2==o?"two":"other"`;
    case 'az':
    case 'bm':
    case 'fa':
    case 'ig':
    case 'hu':
    case 'ja':
    case 'kde':
    case 'kea':
    case 'ko':
    case 'my':
    case 'ses':
    case 'sg':
    case 'to':
    case 'tr':
    case 'vi':
    case 'wo':
    case 'yo':
    case 'zh':
    case 'bo':
    case 'dz':
    case 'id':
    case 'jv':
    case 'ka':
    case 'km':
    case 'kn':
    case 'ms':
    case 'th':
    case 'tp':
    case 'io':
    case 'ia':
      return `o=>"other"`;
    default:
      return `o=>o==1?'one':'other'`;
  }
}
