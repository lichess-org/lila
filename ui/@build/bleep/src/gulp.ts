import * as cps from 'node:child_process';
import { env, colors as c } from './main';

export function gulpWatch(numTries = 0) {
  const gulp = cps.spawn('yarn', ['gulp', 'css'], { cwd: env.uiDir });
  gulp.stdout?.on('data', txt => env.log(txt, { ctx: 'gulp' }));
  gulp.stderr?.on('data', txt => env.log(txt, { ctx: 'gulp' }));
  gulp.on('close', (code: number) => {
    if (code == 1) {
      // gulp fails to watch on macos with exit code 1 pretty randomly
      if (numTries < 3) {
        env.log(c.red('Retrying gulp watch'), { ctx: 'gulp' });
        gulpWatch(numTries + 1);
      } else throw 'gulp fail';
    }
  });
}
