const source = require('vinyl-source-stream');
const gulp = require('gulp');
const gutil = require('gulp-util');
const browserify = require('browserify');
const tsify = require('tsify');
const uglify = require('gulp-uglify');
const streamify = require('gulp-streamify');
const concat = require('gulp-concat');
const request = require('request');
const download = require('gulp-download-stream');
const exec = require('child_process').exec;
const fs = require('fs');

const destination = '../../public/compiled/';
const standalone = 'Lichess';

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

function downloadGithubRelease(repo, dest, cb) {
  const headers = {'User-Agent': 'lila/gulpfile.js'};
  if (process.env.GITHUB_API_TOKEN) {
    headers['Authorization'] = 'token ' + process.env.GITHUB_API_TOKEN;
  }

  request({
    url: 'https://api.github.com/repos/' + repo + '/releases/latest',
    headers: headers
  }, function(err, res, body) {
    if (err) throw err;
    const release = JSON.parse(body);

    download(release.assets.filter(function(asset) {
      const path = dest + asset.name;
      if (!fs.existsSync(path)) return true;
      const stat = fs.statSync(path);
      return stat.mtime < new Date(asset.updated_at) || stat.size != asset.size;
    }).map(function (asset) {
      return asset.browser_download_url;
    })).pipe(gulp.dest(dest)).on('end', cb);
  });
}

gulp.task('stockfish.pexe', function(cb) {
  downloadGithubRelease('niklasf/stockfish.pexe', '../../public/vendor/stockfish/', cb);
});

gulp.task('stockfish.js', function(cb) {
  downloadGithubRelease('niklasf/stockfish.js', '../../public/vendor/stockfish/', cb);
});

gulp.task('prod-source', function() {
  return browserify('src/index.ts', {
    standalone: standalone
  }).plugin(tsify)
    .bundle()
    .pipe(source('lichess.site.source.min.js'))
    .pipe(streamify(uglify()))
    .pipe(gulp.dest('./dist'));
});

gulp.task('dev-source', function() {
  return browserify('src/index.ts', {
    standalone: standalone
  }).plugin(tsify)
    .bundle()
    .pipe(source('lichess.site.source.js'))
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

gulp.task('git-sha', function(cb) {
  exec("git rev-parse -q --short HEAD", function (err, stdout) {
    if (err) throw err;
    if (!fs.existsSync('./dist')) fs.mkdirSync('./dist');
    var date = new Date().toISOString().split('.')[0];
    fs.writeFileSync('./dist/consolemsg.js',
      'console.info("Lichess is open source! https://github.com/ornicar/lila");' +
      `lichess.info = "Assets built ${date} from sha ${stdout.trim()}";`);
    cb();
  });
});

gulp.task('standalones', function() {
  return gulp.src([
    './src/util.js',
    './src/trans.js'
  ])
    .pipe(streamify(uglify()))
    .pipe(gulp.dest(destination));
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

const tasks = ['git-sha', 'jquery-fill', 'ab', 'standalones'];
if (!process.env.TRAVIS || process.env.GITHUB_API_TOKEN) {
  if (!process.env.NO_SF) { // to skip SF download
    tasks.push('stockfish.pexe');
    tasks.push('stockfish.js');
  }
}

gulp.task('dev', tasks.concat(['dev-source']), makeBundle('lichess.site.source.js'));
gulp.task('prod', tasks.concat(['prod-source']), makeBundle('lichess.site.source.min.js'));

gulp.task('watch', ['git-sha', 'jquery-fill', 'ab', 'standalones', 'user-mod', 'dev-source'],
  makeBundle('lichess.site.source.js'));

gulp.task('default', ['watch'], function() {
  return gulp.watch('src/*.js', ['watch']);
});
