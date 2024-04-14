import configPage from './config';
import playPage from './play';

export function initModule(token?: string) {
  token ? playPage(token) : configPage();
}
