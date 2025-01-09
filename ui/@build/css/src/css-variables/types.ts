import type { themes } from './constants.js';

export type Theme = (typeof themes)[number];
export const defaultTheme: Theme = 'dark';

export type ThemeRecord = Record<Theme, Record<string, string>>;
