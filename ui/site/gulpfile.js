const gulp = require('gulp');
const source = require('vinyl-source-stream');
const buffer = require('vinyl-buffer');
const colors = require('ansi-colors');
const logger = require('fancy-log');
const watchify = require('watchify');
const browserify = require('browserify');
const terser = require('gulp-terser');
const size = require('gulp-size');
const tsify = require('tsify');
const concat = require('gulp-concat');
const exec = require('child_process').exec;
const fs = require('fs');

require('../gulp/cssProject.js')(__dirname);

const browserifyOpts = (entries, debug) => ({
  entries: entries,
  standalone: 'Lichess',
  debug: debug
});
const destinationPath = '../../public/compiled/';
const destination = () => gulp.dest(destinationPath);
const fileBaseName = 'lichess.site';

const abFile = process.env.LILA_AB_FILE;

const jqueryFill = () => gulp.src('src/jquery.fill.js')
  .pipe(buffer())
  .pipe(terser({safari10: true}))
  .pipe(gulp.dest('./dist'));

const ab = () => {
  if (abFile) return gulp.src(abFile)
    .pipe(buffer())
    .pipe(terser({safari10: true}))
    .pipe(gulp.dest('./dist'));
  else {
    logger.info(colors.yellow('Building without AB file'));
    return gulp.src('.');
  }
};

const stockfishPexe = () => gulp.src([
  require.resolve('stockfish.pexe/stockfish.nmf'),
  require.resolve('stockfish.pexe/stockfish.pexe'),
  require.resolve('stockfish.pexe/stockfish.bc')
]).pipe(gulp.dest('../../public/vendor/stockfish.pexe'));

const stockfishJs = () => gulp.src([
  require.resolve('stockfish.js/stockfish.wasm.js'),
  require.resolve('stockfish.js/stockfish.wasm'),
  require.resolve('stockfish.js/stockfish.js')
]).pipe(gulp.dest('../../public/vendor/stockfish.js'));

const stockfishWasm = () => gulp.src([
  require.resolve('stockfish.wasm/stockfish.js'),
  require.resolve('stockfish.wasm/stockfish.js.mem'),
  require.resolve('stockfish.wasm/stockfish.wasm'),
  require.resolve('stockfish.wasm/stockfish.worker.js')
]).pipe(gulp.dest('../../public/vendor/stockfish.wasm/'));

const stockfishMvWasm = () => gulp.src([
  require.resolve('stockfish-mv.wasm/stockfish.js'),
  require.resolve('stockfish-mv.wasm/stockfish.js.mem'),
  require.resolve('stockfish-mv.wasm/stockfish.wasm'),
  require.resolve('stockfish-mv.wasm/pthread-main.js')
]).pipe(gulp.dest('../../public/vendor/stockfish-mv.wasm/'));

const prodSource = () => browserify(browserifyOpts('src/index.ts', false))
  .plugin(tsify)
  .bundle()
  .pipe(source(`${fileBaseName}.source.min.js`))
  .pipe(buffer())
  .pipe(terser({safari10: true}))
  .pipe(gulp.dest('./dist'));

const devSource = () => browserify(browserifyOpts('src/index.ts', true))
  .plugin(tsify)
  .bundle()
  .pipe(source(`${fileBaseName}.js`))
  .pipe(destination());

function makeDependencies(filename) {
  return function bundleDeps() {
    return gulp.src([
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
    return gulp.src([
      destinationPath + 'lichess.deps.js',
      './dist/' + filename,
    ])
      .pipe(concat(filename.replace('source.', '')))
      .pipe(destination());
  };
}

const gitSha = (cb) => exec("git rev-parse -q --short HEAD", function (err, stdout) {
  if (err) throw err;
  if (!fs.existsSync('./dist')) fs.mkdirSync('./dist');
  var date = new Date().toISOString().split('.')[0];
  fs.writeFileSync('./dist/consolemsg.js',
    'window.lichess=window.lichess||{};console.info("Lichess is open source! https://github.com/ornicar/lila");' +
    `lichess.info = "Assets built ${date} from sha ${stdout.trim()}";`);
  cb();
});

const standalonesJs = () => gulp.src([
  'util.js', 'trans.js', 'tv.js', 'puzzle.js', 'user.js', 'coordinate.js', 'captcha.js', 'embed-analyse.js'
].map(f => `src/standalones/${f}`))
  .pipe(buffer())
  .pipe(terser({safari10: true}))
  .pipe(destination());

const userMod = () => browserify(browserifyOpts('./src/user-mod.js', false))
  .bundle()
  .pipe(source('user-mod.js'))
  .pipe(buffer())
  .pipe(terser({safari10: true}))
  .pipe(destination());

const deps = makeDependencies('lichess.deps.js');

const tasks = [gitSha, jqueryFill, ab, standalonesJs, userMod, stockfishWasm, stockfishMvWasm, stockfishPexe, stockfishJs, deps];

const dev = gulp.series(tasks.concat([devSource]));

gulp.task('prod', gulp.series(tasks, prodSource, makeBundle(`${fileBaseName}.source.min.js`)));
gulp.task('dev', gulp.series(tasks, dev));
gulp.task('default', gulp.series(tasks, dev, () => gulp.watch('src/**/*.js', dev)));
