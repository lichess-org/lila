var source = require('vinyl-source-stream');
var gulp = require('gulp');
var gutil = require('gulp-util');
var browserify = require('browserify');
var uglify = require('gulp-uglify');
var streamify = require('gulp-streamify');
var concat = require('gulp-concat');
var request = require('request');
var download = require('gulp-download-stream');

var destination = '../../public/compiled/';
var onError = function(error) {
  gutil.log(gutil.colors.red(error.message));
};
var standalone = 'Lichess';

var abFile = process.env.LILA_AB_FILE;

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

function latestGithubRelease(repo, cb) {
  request({
    url: 'https://api.github.com/repos/' + repo + '/releases/latest',
    headers: {
      'User-Agent': 'lila/gulpfile.js'
    }
  }, function(err, res, body) {
    var release = JSON.parse(body);
    cb(release.assets.map(function (asset) {
      return asset.browser_download_url;
    }));
  });
}

gulp.task('stockfish.pexe', function() {
  latestGithubRelease('niklasf/stockfish.pexe', function(urls) {
    download(urls)
      .pipe(gulp.dest('../../public/vendor/stockfish/'));
  });
});

gulp.task('stockfish.js', function() {
  latestGithubRelease('niklasf/stockfish.js', function(urls) {
    download(urls)
      .pipe(gulp.dest('../../public/vendor/stockfish/'));
  });
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

var tasks = ['jquery-fill', 'ab', 'standalones', 'stockfish.pexe', 'stockfish.js'];

gulp.task('dev', tasks.concat(['dev-source']), makeBundle('lichess.site.source.js'));
gulp.task('prod', tasks.concat(['prod-source']), makeBundle('lichess.site.source.min.js'));

gulp.task('default', ['dev'], function() {
  return gulp.watch('src/*.js', ['dev']);
});
