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


const deps = makeDependencies('lichess.deps.js');

const tasks = [
  ab, 
  deps
];

const dev = gulp.series(tasks);

gulp.task('prod', gulp.series(tasks, makeBundle(`${fileBaseName}.source.min.js`)));
gulp.task('dev', gulp.series(tasks, dev));
gulp.task('default', gulp.series(tasks, dev, () => gulp.watch('src/**/*.js', dev)));
