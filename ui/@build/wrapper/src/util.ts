import chalk from 'chalk';

export function debounce<T extends (...args: any[]) => void>(fn: T, delay: number): (...args: Parameters<T>) => void {
  let timeoutId: ReturnType<typeof setTimeout>;
  return (...args: Parameters<T>) => {
    clearTimeout(timeoutId);
    timeoutId = setTimeout(() => fn(...args), delay);
  };
}

const colors = [chalk.green, chalk.blue, chalk.yellow, chalk.cyan, chalk.magenta];
export function withColor(str: string): string {
  return colors[(str.charCodeAt(0) + ((str.charCodeAt(5) || 1) * 7 + str.length)) % colors.length](str);
}

export function errorMsg(str: string): string {
  return chalk.red(`🛑 ${str}`);
}
