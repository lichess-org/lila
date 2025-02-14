export const colors = ['sente', 'gote'] as const;
export const types: { [key: string]: string } = {
  svg: 'svg+xml;base64,',
  png: 'png;base64,',
};

export type Theme = [string, 'svg' | 'png'];
export type RoleDict = {
  [key: string]: string;
};
