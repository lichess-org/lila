const gulp = require('gulp');
const source = require('vinyl-source-stream');
const buffer = require('vinyl-buffer');
const colors = require('ansi-colors');
const logger = require('fancy-log');
const watchify = require('watchify');
const browserify = require('browserify');
const uglify = require('gulp-uglify');
const size = require('gulp-size');
const tsify = require('tsify');
const concat = require('gulp-concat');
const request = require('request');
const download = require('gulp-download-stream');
const exec = require('child_process').exec;
const fs = require('fs');

const browserifyOpts = (entries, debug) => ({
  entries: entries,
  standalone: 'Lichess',
  debug: debug
});
const destination = () => gulp.dest('../../public/compiled/');
const fileBaseName = 'lichess.site';

const abFile = process.env.LILA_AB_FILE;

const jqueryFill = () => gulp.src('src/jquery.fill.js')
  .pipe(buffer())
  .pipe(uglify())
  .pipe(gulp.dest('./dist'));

const ab = () => {
  if (abFile) return gulp.src(abFile)
    .pipe(buffer())
    .pipe(uglify())
    .pipe(gulp.dest('./dist'));
    else {
      logger.info(colors.yellow('Building without AB file'));
      return gulp.src('.');
    }
};

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

const stockfishPexe = (cb) =>
  downloadGithubRelease('niklasf/stockfish.pexe', '../../public/vendor/stockfish/', cb);

const stockfishJs = (cb) =>
  downloadGithubRelease('niklasf/stockfish.js', '../../public/vendor/stockfish/', cb);

const prodSource = () => browserify(browserifyOpts('src/index.ts', false))
  .plugin(tsify)
  .bundle()
  .pipe(source(`${fileBaseName}.source.min.js`))
  .pipe(buffer())
  .pipe(uglify())
  .pipe(gulp.dest('./dist'));

const devSource = () => browserify(browserifyOpts('src/index.ts', true))
  .plugin(tsify)
  .bundle()
  .pipe(source(`${fileBaseName}.source.js`))
  .pipe(gulp.dest('./dist'));

function makeBundle(filename) {
  return function bundleItAll() {
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
      .pipe(destination());
  };
}

const gitSha = (cb) => exec("git rev-parse -q --short HEAD", function (err, stdout) {
  if (err) throw err;
  if (!fs.existsSync('./dist')) fs.mkdirSync('./dist');
  var date = new Date().toISOString().split('.')[0];
  fs.writeFileSync('./dist/consolemsg.js',
    'console.info("Lichess is open source! https://github.com/ornicar/lila");' +
    `lichess.info = "Assets built ${date} from sha ${stdout.trim()}";`);
  cb();
});

const standaloneFiles = [
  'src/standalones/util.js',
  'src/standalones/trans.js',
  'src/standalones/tv.js',
  'src/standalones/puzzle.js',
  'src/standalones/user.js',
  'src/standalones/coordinate.js'
];

const standalones = () => gulp.src(standaloneFiles)
  .pipe(buffer())
  .pipe(uglify())
  .pipe(destination());

const userMod = () => browserify(browserifyOpts('./src/user-mod.js', false))
  .bundle()
  .pipe(source('user-mod.js'))
  .pipe(buffer())
  .pipe(uglify())
  .pipe(destination());

const tasks = [gitSha, jqueryFill, ab, standalones, userMod];
if ((!process.env.TRAVIS || process.env.GITHUB_API_TOKEN) && !process.env.NO_SF) {
  tasks.push(stockfishPexe);
  tasks.push(stockfishJs);
}

const dev = gulp.series(tasks.concat([devSource, makeBundle(`${fileBaseName}.source.js`)]));

gulp.task('prod', gulp.series(tasks.concat([prodSource, makeBundle(`${fileBaseName}.source.min.js`)])));
gulp.task('dev', dev);
gulp.task('default', gulp.series(dev, () => gulp.watch('src/*.js', dev)));
