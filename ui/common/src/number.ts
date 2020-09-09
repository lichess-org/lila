let numberFormatter: false | Intl.NumberFormat | null = false;

export const numberFormat = (n: number) => {
  if (numberFormatter === false) numberFormatter = (window.Intl && Intl.NumberFormat) ? new Intl.NumberFormat() : null;
  if (numberFormatter === null) return '' + n;
  return numberFormatter.format(n);
};
