import { execSync } from 'child_process';
import * as path from 'path';
import { fileURLToPath } from 'url';
import * as fs from 'fs/promises';

const baseDistFolder = path.join(
  path.dirname(execSync('pnpm root -w').toString()),
  'public/vendors',
);

function stripScope(packageName: string): string {
  if (packageName.startsWith('@')) return packageName.split('/')[1];
  else return packageName;
}

export async function copyLocalPackage(packageName: string): Promise<void> {
  try {
    const __dirname = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..'),
      packagePath = path.join(__dirname, 'src/local', packageName),
      nameNoScope = stripScope(packageName),
      destinationPath = path.join(baseDistFolder, nameNoScope);

    await fs.mkdir(destinationPath, { recursive: true });

    await fs.cp(packagePath, destinationPath, { recursive: true });
    console.log(`✓ local/${nameNoScope}`);
  } catch (error) {
    console.error(`Failed to copy directory: ${error.message}`);
  }
}

export async function copyVendorPackage(packageName: string, fileNames: string[]): Promise<void> {
  try {
    const __dirname = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..'),
      nameNoScope = stripScope(packageName),
      packagePath = path.join(__dirname, 'node_modules', packageName);

    await fs.mkdir(path.join(baseDistFolder, nameNoScope), { recursive: true });

    for (const fileName of fileNames) {
      const sourceFilePath = path.join(packagePath, fileName),
        destinationFilePath = path.join(
          path.join(baseDistFolder, nameNoScope),
          path.basename(fileName),
        );

      await fs.copyFile(sourceFilePath, destinationFilePath);
      console.log(`✓ ${packageName}`);
    }
  } catch (error) {
    console.error(`Failed to copy files: ${error.message}`);
  }
}
