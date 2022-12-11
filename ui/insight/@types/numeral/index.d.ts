export default function numeral(n: number): Numeral;

export interface Numeral {
  format(f: string): string;
}
