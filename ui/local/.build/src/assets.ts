import * as fs from 'node:fs';
import * as path from 'node:path';
import * as crypto from 'node:crypto';
import * as ps from 'node:process';
import * as fg from 'fast-glob';

async function globArray(glob: string, opts: fg.Options = {}): Promise<string[]> {
  const files: string[] = [];
  for await (const f of fg.stream(glob, { absolute: true, onlyFiles: true, ...opts })) {
    files.push(f.toString('utf8'));
  }
  return files;
}

function hashfile(filename: string) {
  const data = fs.readFileSync(filename);
  const hash = crypto.createHash('sha256').update(data).digest('hex').slice(0, 12);
  return hash;
}

function process(filename: string) {
  const ext = path.extname(filename);
  const name = path.basename(filename, ext);
  const dir = path.dirname(filename);
  const booksDir = dir.includes('books');
  const hash = hashfile(filename);
  const newfilename = path.join(dir, hash + ext);

  if (filename !== newfilename && !fs.existsSync(newfilename)) fs.renameSync(filename, newfilename);

  if (booksDir) {
    filename = `${dir}/${name}.png`;
    if (!fs.existsSync(`${dir}/${hash}.png`)) fs.renameSync(filename, `${dir}/${hash}.png`);
    return [hash, name];
  } else return [`${hash}${ext}`, `${name}${ext}`];
}

async function main() {
  const args = ps.argv.slice(2);
  const rootDir = path.resolve(args.length ? args[0] : '../../../public/lifat/bots');
  const keyNames = new Map<string, string>();
  const files = await globArray('**/*', { cwd: rootDir });
  for (const file of files) {
    if (file.endsWith('-torso.webp') || file.endsWith('not-worthy.webp')) continue;
    if (file.includes('/books/') && file.endsWith('.png')) continue;
    const [_id, name] = process(file);
    keyNames.set(_id, name);
  }
  const manifest = [...keyNames.entries()].map(([id, name]) => ({ _id: id, name }));
  const manifestPath = '../json/local.assets.json';
  fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2), 'utf-8');
  console.log(`Manifest written to ${manifestPath}`);
}

main().catch(console.error);
