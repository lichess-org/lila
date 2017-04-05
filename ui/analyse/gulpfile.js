const gulp = require('gulp');
const gutil = require('gulp-util');
const watchify = require('watchify');
const browserify = require('browserify');
const uglify = require('gulp-uglify');
const source = require('vinyl-source-stream');
const buffer = require('vinyl-buffer');
const tsify = require('tsify');

const destination = '../../public/compiled/';

function onError(error) {
  gutil.log(gutil.colors.red(error.message));
}

function build() {
  return browserify('src/main.ts', {
      standalone: 'LichessAnalyse',
      debug: true
    })
    .plugin(tsify);
}

const watchedBrowserify = watchify(build());

function bundle(stream) {
  return stream
    .bundle()
    .on('error', onError)
    .pipe(source('lichess.analyse.js'))
    .pipe(buffer())
    .pipe(gulp.dest(destination));
}

gulp.task('default', () => bundle(watchedBrowserify));
watchedBrowserify.on('update', () => bundle(watchedBrowserify));
watchedBrowserify.on('log', gutil.log);

gulp.task('dev', function() {
  return bundle(build());
});

gulp.task('prod', function() {
  return build()
    .bundle()
    .on('error', onError)
    .pipe(source('lichess.analyse.min.js'))
    .pipe(buffer())
    .pipe(uglify())
    .pipe(gulp.dest(destination));
});
