var source = require('vinyl-source-stream');
var gulp = require('gulp');
var gutil = require('gulp-util');
var browserify = require('browserify');
var uglify = require('gulp-uglify');
var streamify = require('gulp-streamify');
var concat = require('gulp-concat');

var destination = '../../public/compiled/';
var onError = function(error) {
  gutil.log(gutil.colors.red(error.message));
};
var standalone = 'Lichess';

var abFile = process.env.LILA_AB_FILE;
if (!process.env.LILA_AB_FILE) gutil.log('Building without AB file');

gulp.task('jquery-fill', function() {
  return gulp.src('src/jquery.fill.js')
    .pipe(streamify(uglify()))
    .pipe(gulp.dest('./dist'));
});
gulp.task('ab', function() {
  return gulp.src(process.env.LILA_AB_FILE)
    .pipe(streamify(uglify()))
    .pipe(gulp.dest('./dist'));
});

gulp.task('prod-source', function() {
  return browserify('./src/index.js', {
    standalone: standalone
  }).bundle()
    .on('error', onError)
    .pipe(source('lichess.site.source.min.js'))
    .pipe(streamify(uglify()))
    .pipe(gulp.dest('./dist'));
});

gulp.task('dev-source', function() {
  return browserify('./src/index.js', {
    standalone: standalone
  }).bundle()
    .on('error', onError)
    .pipe(source('lichess.site.source.js'))
    .pipe(gulp.dest('./dist'));
});

function makeBundle(filename) {
  return function() {
    return gulp.src([
      '../../public/javascripts/vendor/jquery.min.js',
      './dist/jquery.fill.js',
      '../../public/vendor/moment/min/moment.min.js',
      './dep/powertip.min.js',
      './dep/howler.min.js',
      './dep/mousetrap.min.js',
      './dep/hoverintent.min.js',
      './dist/' + filename,
      './dist/ab.js',
      '../../public/javascripts/ga.js'
    ])
      .pipe(concat(filename.replace('source.', '')))
      .pipe(gulp.dest(destination));
  };
}

gulp.task('standalones', function() {
  return gulp.src([
    './src/util.js',
    './src/themepicker.js'
  ])
    .pipe(streamify(uglify()))
    .pipe(gulp.dest(destination));
});

gulp.task('dev', ['jquery-fill', 'ab', 'standalones', 'dev-source'], makeBundle('lichess.site.source.js'));
gulp.task('prod', ['jquery-fill', 'ab', 'standalones', 'prod-source'], makeBundle('lichess.site.source.min.js'));

gulp.task('default', ['dev'], function() {
  return gulp.watch('src/*.js', ['dev']);
});
