import configPage from './config';
import playPage from './play';

export function initModule(token?: string): void {
  if (token) {
    playPage(token);
  } else {
    configPage();
  }
}
