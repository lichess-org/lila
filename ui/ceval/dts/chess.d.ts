declare module 'chess' {
  export function fixCrazySan(san: string): string;
  export function decomposeUci(uci: string): [string, string, string];
  export function renderEval(e: number): string;
}
