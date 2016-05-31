var source = require('vinyl-source-stream');
var gulp = require('gulp');
var gutil = require('gulp-util');
var watchify = require('watchify');
var browserify = require('browserify');
var uglify = require('gulp-uglify');
var streamify = require('gulp-streamify');

var sources = ['./src/main.js'];
var destination = '../../public/compiled/';
var onError = function(error) {
  gutil.log(gutil.colors.red(error.message));
};
var standalone = 'LichessNotification';

gulp.task('prod', function() {
  return browserify('./src/main.js', {
    standalone: standalone
  }).bundle()
    .on('error', onError)
    .pipe(source('lichess.notification.min.js'))
    .pipe(streamify(uglify()))
    .pipe(gulp.dest(destination));
});

gulp.task('dev', function() {
  return browserify('./src/main.js', {
    standalone: standalone
  }).bundle()
    .on('error', onError)
    .pipe(source('lichess.notification.js'))
    .pipe(gulp.dest(destination));
});

gulp.task('watch', function() {
  var opts = watchify.args;
  opts.debug = true;
  opts.standalone = standalone;

  var bundleStream = watchify(browserify(sources, opts))
    .on('update', rebundle)
    .on('log', gutil.log);

  function rebundle() {
    return bundleStream.bundle()
      .on('error', onError)
      .pipe(source('lichess.notification.js'))
      .pipe(gulp.dest(destination));
  }

  return rebundle();
});

gulp.task('default', ['watch']);
