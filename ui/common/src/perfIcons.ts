export function getPerfIcon(key: VariantKey | Perf): string;
export function getPerfIcon(key: string | undefined): string | undefined;
export function getPerfIcon(key: string | undefined): string | undefined {
  return perfIcons[(key || '').toLowerCase()];
}

const perfIcons: Record<string, string> = {
  blitz: ')',
  ultrabullet: '{',
  bullet: 'T',
  classical: '+',
  rapid: '#',
  standard: 'C',
  minishogi: ',',
  chushogi: '(',
  annanshogi: '',
  kyotoshogi: '',
  correspondence: ';',
};
