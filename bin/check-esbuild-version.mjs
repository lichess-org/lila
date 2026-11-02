import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const repoRoot = resolve(import.meta.dirname, '..');
const flake = readFileSync(resolve(repoRoot, 'flake.nix'), 'utf8');
const override = flake.match(/esbuild\s*=\s*prev\.esbuild\.overrideAttrs\s*\([\s\S]*?\n\s*\}\);/);
const flakeVersion = override?.[0].match(/\bversion\s*=\s*"([^"]+)"/)?.[1];

if (!flakeVersion) {
  throw new Error('Could not find the esbuild version in flake.nix');
}

const packageJson = JSON.parse(readFileSync(resolve(repoRoot, 'ui/.build/package.json'), 'utf8'));
const packageVersion = packageJson.dependencies?.esbuild;

if (flakeVersion !== packageVersion) {
  throw new Error(
    `esbuild version mismatch: flake.nix uses ${flakeVersion}, ui/.build/package.json uses ${packageVersion}`,
  );
}

console.log(`esbuild versions match: ${flakeVersion}`);
