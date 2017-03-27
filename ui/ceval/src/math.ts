export function median(values: number[]): number | undefined {
  values.sort(function(a, b) {
    return a - b;
  });
  var half = Math.floor(values.length / 2);
  return values.length % 2 ? values[half] :
    (values[half - 1] + values[half]) / 2.0;
}
