const source = require('vinyl-source-stream');
const gulp = require('gulp');
const gutil = require('gulp-util');
const browserify = require('browserify');
const uglify = require('gulp-uglify');
const streamify = require('gulp-streamify');
const concat = require('gulp-concat');
const request = require('request');
const download = require('gulp-download-stream');
const exec = require('child_process').exec;
const fs = require('fs');

const destination = '../../public/compiled/';
const standalone = 'Lidraughts';

const abFile = process.env.LILA_AB_FILE;

gulp.task('jquery-fill', function() {
  return gulp.src('src/jquery.fill.js')
    .pipe(streamify(uglify()))
    .pipe(gulp.dest('./dist'));
});

gulp.task('ab', function() {
  if (abFile) {
    return gulp.src(abFile)
      .pipe(streamify(uglify()))
      .pipe(gulp.dest('./dist'));
  } else {
    gutil.log(gutil.colors.yellow('Building without AB file'));
    return gulp.src('.').pipe(gutil.noop());
  }
});

gulp.task('prod-source', function() {
  return browserify('./src/index.js', {
    standalone: standalone
  }).bundle()
    .pipe(source('lidraughts.site.source.min.js'))
    .pipe(streamify(uglify()))
    .pipe(gulp.dest('./dist'));
});

gulp.task('dev-source', function() {
  return browserify('./src/index.js', {
    standalone: standalone
  }).bundle()
    .pipe(source('lidraughts.site.source.js'))
    .pipe(gulp.dest('./dist'));
});

function makeBundle(filename) {
  return function() {
    return gulp.src([
      '../../public/javascripts/vendor/jquery.min.js',
      './dist/jquery.fill.js',
      './dep/powertip.min.js',
      './dep/howler.min.js',
      './dep/mousetrap.min.js',
      './dep/hoverintent.min.js',
      './dist/' + filename,
      './dist/ab.js',
      './dist/consolemsg.js'
    ])
      .pipe(concat(filename.replace('source.', '')))
      .pipe(gulp.dest(destination));
  };
}

gulp.task('standalones', function() {
  return gulp.src([
    './src/util.js',
    './src/trans.js'
  ])
    .pipe(streamify(uglify()))
    .pipe(gulp.dest(destination));
});

gulp.task('git-sha', function(cb) {
  exec("git rev-parse -q --short HEAD", function (err, stdout) {
    if (err) throw err;
    if (!fs.existsSync('./dist')) fs.mkdirSync('./dist');
    var date = new Date().toISOString().split('.')[0];
    fs.writeFileSync('./dist/consolemsg.js',
      'console.info("Lidraughts is open source, a fork of Lichess! See https://github.com/roepstoep/lidraughts");' +
      `lidraughts.info = "Assets built ${date} from sha ${stdout.trim()}";`);
    cb();
  });
});

gulp.task('user-mod', function() {
  return browserify([
    './src/user-mod.js'
  ], {
    standalone: standalone
  }).bundle()
    .pipe(source('user-mod.js'))
    .pipe(streamify(uglify()))
    .pipe(gulp.dest(destination));
});

const tasks = ['jquery-fill', 'ab', 'standalones'];

gulp.task('dev', tasks.concat(['dev-source']), makeBundle('lidraughts.site.source.js'));
gulp.task('prod', tasks.concat(['prod-source']), makeBundle('lidraughts.site.source.min.js'));

gulp.task('watch', ['git-sha', 'jquery-fill', 'ab', 'standalones', 'user-mod', 'dev-source'],
  makeBundle('lidraughts.site.source.js'));

gulp.task('default', ['watch'], function() {
  return gulp.watch('src/*.js', ['watch']);
});
