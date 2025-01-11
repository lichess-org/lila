import * as fs from 'node:fs';
import { getAllPackages } from './package.js';
import type { Context, PackageInfo } from './types.js';
import { debounce, errorMsg, withColor } from './util.js';

const watchers: fs.FSWatcher[] = [];

function packageWatch(
  ctx: Context,
  pkg: PackageInfo,
  fullPath: string,
  opts: fs.WatchOptions = {},
): void {
  try {
    if (fs.existsSync(fullPath)) {
      const fn = debounce(async (event: fs.WatchEventType, filename: string | null) => {
        console.log(
          `\n🧭 Change detected in package: ${withColor(pkg.name)} (${`${fullPath}/${filename}`})`,
        );
        try {
          console.time('🔨 Rebuild completed in');
          await ctx.run(pkg, event, filename ? `${fullPath}/${filename}` : null);
        } catch (error) {
          console.error(errorMsg(`Build failed for ${pkg.name}:`), error);
        } finally {
          console.timeEnd('🔨 Rebuild completed in');
        }
      }, 20);
      const watcher = fs.watch(fullPath, opts, (event, filename) => fn(event, filename));
      watchers.push(watcher);
    }
  } catch (error) {
    console.error(errorMsg(`Failed to watch ${fullPath}:`), error);
  }
}

function globalWatch(ctx: Context, fullPath: string, opts: fs.WatchOptions = {}): void {
  try {
    if (fs.existsSync(fullPath)) {
      const fn = debounce(async (event: fs.WatchEventType, filename: string | null) => {
        console.log(`\n🧭 Change detected in: ${`${fullPath}/${filename}`}`);
        try {
          console.time('🔨 Global run completed in');
          if (ctx.global) await ctx.global(event, filename ? `${fullPath}/${filename}` : null);
        } catch (error) {
          console.error(errorMsg(`Global run failed for ${fullPath}:`), error);
        } finally {
          console.timeEnd('🔨 Global run completed in');
        }
      }, 20);
      const watcher = fs.watch(fullPath, opts, (event, filename) => fn(event, filename));
      watchers.push(watcher);
    }
  } catch (error) {
    console.error(errorMsg(`Failed to watch ${fullPath}:`), error);
  }
}

let alreadyStopping = false;
function setupSignalHandlers(ctx: Context): void {
  const signals: NodeJS.Signals[] = ['SIGINT', 'SIGTERM', 'SIGQUIT', 'SIGHUP'];
  signals.forEach(signal => {
    process.on(signal, async () => {
      if (alreadyStopping) return;
      console.log('Handling', signal);

      alreadyStopping = true;
      await stop(ctx);
      process.exit(0);
    });
  });

  process.on('uncaughtException', async (error: Error) => {
    console.error(errorMsg('Uncaught Exception:'), error);
    stop(ctx);
    process.exit(1);
  });
}

async function stop(ctx: Context): Promise<void> {
  console.log('\n👋 See you next time.\n');
  await ctx.stop();
  watchers.forEach(watcher => watcher.close());
  watchers.length = 0;
}

function setupWatchers(ctx: Context, packages: PackageInfo[], root: PackageInfo): void {
  packages.forEach(pkg => {
    ctx.packageWatch.forEach(({ path, options }) => {
      const fullPath = path.startsWith('/') ? path : `${pkg.path}/${path}`;
      packageWatch(ctx, pkg, fullPath, options);
    });
  });
  if (ctx.globalWatch && ctx.global) {
    ctx.globalWatch.forEach(({ path, options }) => {
      const fullPath = path.startsWith('/') ? path : `${root.path}/${path}`;
      globalWatch(ctx, fullPath, options);
    });
  }
}

export async function wrapper(context: Context): Promise<void> {
  try {
    const flags = process.argv.slice(2);
    const isProd = flags.includes('--prod');
    console.log(`# ${withColor(context.name)} [${isProd ? 'PROD' : 'DEV'}]\n`);

    setupSignalHandlers(context);

    const pWrap = await getAllPackages(flags);

    console.time('🧰 Initiation completed in');
    await context.init(pWrap, flags);
    console.timeEnd('🧰 Initiation completed in');

    console.time('\n🚧 Full build completed in');
    console.log('');
    await context.runAll();
    console.timeEnd('\n🚧 Full build completed in');

    if (flags.includes('--watch')) {
      setupWatchers(context, pWrap.packages, pWrap.root);
      console.log('\n🔭 Started watching!\n');
    } else {
      console.log('\nRun in watchmode with "--watch" flag.');
      await stop(context);
      process.exit(0);
    }
  } catch (error) {
    console.error(errorMsg('ERROR - build stopped:'), error);
    stop(context);
    process.exit(1);
  }
}
