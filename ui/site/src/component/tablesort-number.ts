import tablesort from 'tablesort';

export default function extendTablesortNumber() {
  tablesort.extend(
    'number',
    (item: string) => item.match(/^[-+]?(\d)*-?([,\.]){0,1}-?(\d)+([E,e][\-+][\d]+)?%?$/),
    (a: string, b: string) => compareNumber(cleanNumber(b), cleanNumber(a))
  );
}

const cleanNumber = (i: string) => i.replace(/[^\-?0-9.]/g, '');

const compareNumber = (a: any, b: any) => {
  a = parseFloat(a);
  b = parseFloat(b);

  a = isNaN(a) ? 0 : a;
  b = isNaN(b) ? 0 : b;

  return a - b;
};
