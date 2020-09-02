import tablesort from 'tablesort';

function cleanNumber(i) {
  return i.replace(/[^\-?0-9.]/g, '');
}

function compareNumber(a, b) {
  a = parseFloat(a);
  b = parseFloat(b);

  a = isNaN(a) ? 0 : a;
  b = isNaN(b) ? 0 : b;

  return a - b;
};

export default function extendTablesortNumber() {
  tablesort.extend('number', function(item) {
    return item.match(/^[-+]?(\d)*-?([,\.]){0,1}-?(\d)+([E,e][\-+][\d]+)?%?$/); // Number
  }, function(a, b) {
    return compareNumber(cleanNumber(b), cleanNumber(a));
  });
}
