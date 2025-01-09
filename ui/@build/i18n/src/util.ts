import vm from 'node:vm';
import { basename } from 'path';

export function isValidJs(code: string): boolean {
  try {
    new vm.Script(code);
    return true;
  } catch {
    return false;
  }
}

export function categoryName(name: string): string {
  const noExt = basename(name, '.xml'); // if file is given
  return noExt === 'class' ? 'clas' : noExt;
}
