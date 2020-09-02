const gulp = require('gulp');
const buffer = require('vinyl-buffer');
const colors = require('ansi-colors');
const logger = require('fancy-log');
const terser = require('gulp-terser');
const concat = require('gulp-concat');
const path = require('path');

const destinationPath = '../../public/compiled/';
const destination = () => gulp.dest(destinationPath);
const fileBaseName = 'lichess.site';

const abFile = process.env.LILA_AB_FILE;

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

const hopscotch = () => gulp.src([
  'dist/js/hopscotch.min.js',
  'dist/**/*.min.css',
  'dist/img/*'
], {
  cwd: path.dirname(require.resolve('hopscotch/package.json')),
  cwdbase: true
}).pipe(gulp.dest('../../public/vendor/hopscotch/'));

const jqueryBarRating = () => gulp.src([
  'dist/jquery.barrating.min.js'
], {
  cwd: path.dirname(require.resolve('jquery-bar-rating/package.json')),
  cwdbase: true
}).pipe(gulp.dest('../../public/vendor/bar-rating/'));

const highcharts = () => gulp.src([
  'highcharts.js',
  'highcharts-more.js',
  'highstock.js'
], {
  cwd: path.dirname(require.resolve('highcharts/package.json')),
  cwdbase: true
}).pipe(gulp.dest('../../public/vendor/highcharts-4.2.5/'));

function makeDependencies(filename) {
  return function bundleDeps() {
    return gulp.src([
  '../../public/javascripts/vendor/jquery.min.js',
  './dep/powertip.min.js',
  './dep/howler.min.js',
  './dep/mousetrap.min.js',
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

const standalonesJs = () => gulp.src([
  'captcha.js', 'embed-analyse.js'
].map(f => `src/standalones/${f}`))
  .pipe(buffer())
  .pipe(terser({safari10: true}))
  .pipe(destination());

const deps = makeDependencies('lichess.deps.js');

const tasks = [
  ab, standalonesJs,
  deps,
  hopscotch, jqueryBarRating, highcharts
];

const dev = gulp.series(tasks);

gulp.task('prod', gulp.series(tasks, makeBundle(`${fileBaseName}.source.min.js`)));
gulp.task('dev', gulp.series(tasks, dev));
gulp.task('default', gulp.series(tasks, dev, () => gulp.watch('src/**/*.js', dev)));
