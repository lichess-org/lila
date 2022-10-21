import * as ps from 'node:process';
import * as path from 'node:path';
import * as fs from 'node:fs';

import { build } from './build';
import { init } from './env';

export function main() {
  // plenty of room in here to add your edge case arguments.  go nuts!

  const configPath = path.resolve(__dirname, '../bleep.config.json');
  const config = fs.existsSync(configPath) ? JSON.parse(fs.readFileSync(configPath, 'utf8')) : undefined;
  init(path.resolve(__dirname, '../../../..'), config);

  if (ps.argv.length > 2 && (ps.argv[2] == '--help' || ps.argv[2] == 'help')) {
    console.log(fs.readFileSync(path.resolve(__dirname, '../readme'), 'utf8'));
    return;
  }

  build(ps.argv.slice(2));
}

main();
