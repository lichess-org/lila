import configPage from './config';
import playPage from './play';

export function initModule(token?: string): void {
  token ? playPage(token) : configPage();
}
