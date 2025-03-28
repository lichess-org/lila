import tablesort from 'tablesort';

export default function extendTablesortNumber(): void {
  tablesort.extend(
    'number',
    (item: string) => item.match(/^[-+]?(\d)*-?([,\.]){0,1}-?(\d)+([E,e][\-+][\d]+)?%?$/),
    (a: string, b: string) => validNum(b) - validNum(a),
  );
}

const validNum = (i: string): number => {
  const num = parseFloat(i.replace(/[^\-?0-9.]/g, ''));
  return isNaN(num) ? 0 : num;
};
