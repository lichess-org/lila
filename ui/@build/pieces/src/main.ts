import { execSync } from 'node:child_process';
import { existsSync, mkdirSync } from 'node:fs';
import * as path from 'node:path';
import { chushogi } from './chushogi.js';
import { kyotoshogi } from './kyotoshogi.js';
import { standard } from './standard.js';

function main() {
  const lilaDir = path.dirname(execSync('pnpm root -w').toString().trim());
  const sourceDir = path.join(lilaDir, 'public/piece/');
  const destDir = path.join(lilaDir, 'public/piece-css/');

  if (!existsSync(destDir)) mkdirSync(destDir);

  console.time('Standard themes');
  standard(sourceDir, destDir);
  console.timeEnd('Standard themes');

  console.time('Kyotoshogi themes');
  kyotoshogi(sourceDir, destDir);
  console.timeEnd('Kyotoshogi themes');

  console.time('Chushogi themes');
  chushogi(sourceDir, destDir);
  console.timeEnd('Chushogi themes');
}

main();
