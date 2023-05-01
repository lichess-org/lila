export function getPerfIcon(key: string): string | undefined {
  return perfIcons[key.toLowerCase()];
}

const perfIcons: Record<string, string> = {
  blitz: ')',
  ultraBullet: '{',
  bullet: 'T',
  classical: '+',
  rapid: '#',
  minishogi: ',',
  chushogi: '(',
  annanshogi: '',
  kyotoshogi: '',
  correspondence: ';',
};
