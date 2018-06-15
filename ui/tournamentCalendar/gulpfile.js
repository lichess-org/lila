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
      standalone: 'LichessTournamentCalendar',
      debug: true
    })
    .plugin(tsify);
}

const watchedBrowserify = watchify(build());

function bundle() {
  return watchedBrowserify
    .bundle()
    .on('error', onError)
    .pipe(source('lichess.tournament-calendar.js'))
    .pipe(buffer())
    .pipe(gulp.dest(destination));
}

gulp.task('default', bundle);
watchedBrowserify.on('update', bundle);
watchedBrowserify.on('log', gutil.log);

gulp.task('dev', function() {
  return build()
    .bundle()
    .pipe(source('lichess.tournament-calendar.js'))
    .pipe(gulp.dest(destination));
});

gulp.task('prod', function() {
  return build()
    .bundle()
    .pipe(source('lichess.tournament-calendar.min.js'))
    .pipe(buffer())
    .pipe(uglify())
    .pipe(gulp.dest(destination));
});
