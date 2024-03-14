const gulp = require('gulp');
const source = require('vinyl-source-stream');
const buffer = require('vinyl-buffer');
const colors = require('ansi-colors');
const logger = require('fancy-log');
const browserify = require('browserify');
const babelify = require('babelify');
const terser = require('gulp-terser');
const tsify = require('tsify');
const concat = require('gulp-concat');
const rename = require('gulp-rename');
const execSync = require('child_process').execSync;
const fs = require('fs');
const path = require('path');

const browserifyOpts = (entries, debug) => ({
  entries: entries,
  standalone: 'Lishogi',
  debug: debug,
});
const destinationPath = '../../public/compiled/';
const destination = () => gulp.dest(destinationPath);
const fileBaseName = 'lishogi.site';

const abFile = process.env.LILA_AB_FILE;

const jqueryFill = () =>
  gulp
    .src('src/jquery.fill.js')
    .pipe(buffer())
    .pipe(terser({ safari10: true }))
    .pipe(gulp.dest('./dist'));

const ab = () => {
  if (abFile)
    return gulp
      .src(abFile)
      .pipe(buffer())
      .pipe(terser({ safari10: true }))
      .pipe(gulp.dest('./dist'));
  else {
    logger.info(colors.yellow('Building without AB file'));
    return gulp.src('.');
  }
};

const hopscotch = () =>
  gulp
    .src(['dist/js/hopscotch.min.js', 'dist/**/*.min.css', 'dist/img/*'], {
      cwd: path.dirname(require.resolve('hopscotch/package.json')),
      cwdbase: true,
    })
    .pipe(gulp.dest('../../public/vendor/hopscotch/'));

const highcharts = () =>
  gulp
    .src(['highcharts.js', 'highcharts-more.js', 'highstock.js'], {
      cwd: path.dirname(require.resolve('highcharts/package.json')),
      cwdbase: true,
    })
    .pipe(gulp.dest('../../public/vendor/highcharts-4.2.5/'));

const spectrum = () =>
  gulp
    .src(['dist/spectrum.min.js', 'dist/spectrum.min.css'], {
      cwd: path.dirname(require.resolve('spectrum/package.json')),
      cwdbase: true,
    })
    .pipe(gulp.dest('../../public/vendor/spectrum/'));

const fairy = () =>
  gulp
    .src(['stockfish.js', 'stockfish.wasm', 'stockfish.worker.js'], {
      cwd: path.dirname(require.resolve('fairy-stockfish-nnue.wasm/package.json')),
      cwdbase: true,
    })
    .pipe(gulp.dest('../../public/vendor/fairy/'));

const shogiground = () =>
  gulp
    .src(['dist/shogiground.min.js'], {
      cwd: path.dirname(require.resolve('shogiground/package.json')),
      cwdbase: true,
    })
    .pipe(rename({ dirname: '' }))
    .pipe(gulp.dest('../../public/javascripts/vendor'));

const prodSource = () =>
  browserify(browserifyOpts('src/index.ts', false))
    .plugin(tsify)
    .bundle()
    .pipe(source(`${fileBaseName}.source.min.js`))
    .pipe(buffer())
    .pipe(terser({ safari10: true }))
    .pipe(gulp.dest('./dist'));

const devSource = () =>
  browserify(browserifyOpts('src/index.ts', true))
    .plugin(tsify)
    .bundle()
    .pipe(source(`${fileBaseName}.js`))
    .pipe(destination());

function makeDependencies(filename) {
  return function bundleDeps() {
    return gulp
      .src([
        '../../public/javascripts/vendor/jquery.min.js',
        './dist/jquery.fill.js',
        './dep/powertip.min.js',
        './dep/howler.min.js',
        './dep/mousetrap.min.js',
        './dist/consolemsg.js',
        ...(abFile ? ['./dist/ab.js'] : []),
      ])
      .pipe(concat(filename))
      .pipe(destination());
  };
}

function makeBundle(filename) {
  return function bundleItAll() {
    return gulp
      .src([destinationPath + 'lishogi.deps.js', './dist/' + filename])
      .pipe(concat(filename.replace('source.', '')))
      .pipe(destination());
  };
}

const gitSha = cb => {
  let latestCommit;
  try {
    latestCommit = JSON.parse(execSync('curl -s https://api.github.com/repos/WandererXII/lishogi/commits/master'));
  } catch (ex) {
    latestCommit = {};
  }
  const info = JSON.stringify({
    date: new Date(new Date().toUTCString()).toISOString().split('.')[0] + '+00:00',
    commit: (latestCommit.sha || '').trim(),
    message: (latestCommit.commit?.message || '').trim(),
  });
  if (!fs.existsSync('./dist')) fs.mkdirSync('./dist');
  fs.writeFileSync(
    './dist/consolemsg.js',
    `window.lishogi=window.lishogi||{};console.info("Lishogi is open source! https://github.com/WandererXII/lishogi");lishogi.info=${info};`
  );
  cb();
};

const standalonesJs = () =>
  gulp
    .src(
      ['util.js', 'trans.js', 'tv.js', 'puzzle.js', 'user.js', 'coordinate.js', 'embed-analyse.js'].map(
        f => `src/standalones/${f}`
      )
    )
    .pipe(buffer())
    .pipe(terser({ safari10: true }))
    .pipe(destination());

function singlePackageBabel(file, dest) {
  return () =>
    browserify(browserifyOpts(file, false))
      .transform(babelify, { presets: ['@babel/preset-env'], global: true })
      .bundle()
      .pipe(source(dest))
      .pipe(buffer())
      .pipe(terser({ safari10: false }))
      .pipe(destination());
}

function singlePackage(file, dest) {
  return () =>
    browserify(browserifyOpts(file, false))
      .bundle()
      .pipe(source(dest))
      .pipe(buffer())
      .pipe(terser({ safari10: false }))
      .pipe(destination());
}

const userMod = singlePackage('./src/user-mod.js', 'user-mod.js');
const clas = singlePackage('./src/clas.js', 'clas.js');
const captcha = singlePackageBabel('./src/standalones/captcha.js', 'captcha.js');

const deps = makeDependencies('lishogi.deps.js');

const tasks = [
  gitSha,
  jqueryFill,
  ab,
  standalonesJs,
  userMod,
  clas,
  captcha,
  deps,
  hopscotch,
  highcharts,
  spectrum,
  fairy,
  shogiground,
];

const dev = gulp.series(tasks.concat([devSource]));

gulp.task('prod', gulp.series(tasks, prodSource, makeBundle(`${fileBaseName}.source.min.js`)));
gulp.task('dev', dev);
gulp.task(
  'default',
  gulp.series(tasks, dev, () => gulp.watch('src/**/*.js', dev))
);
