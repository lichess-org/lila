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

module.exports = (standalone, fileBaseName, dir, standaloneFiles) => {

  const browserifyOpts = (debug) => ({
    entries: [`${dir}/src/main.ts`],
    standalone: standalone,
    debug: debug
  });
  const destination = () => gulp.dest(`../../public/compiled/`);

  const prod = () => browserify(browserifyOpts(false))
    .plugin(tsify)
    .bundle()
    .pipe(source(`${fileBaseName}.min.js`))
    .pipe(buffer())
    .pipe(uglify())
    .pipe(size())
    .pipe(destination());

  const dev = () => browserify(browserifyOpts(true))
    .plugin(tsify)
    .bundle()
    .pipe(source(`${fileBaseName}.js`))
    .pipe(destination());

  const standalones = gulp.series(standaloneFiles.map(file => {
    return function standalone() {
      return browserify({
        entries: [file],
        debug: true
      })
        .plugin(tsify)
        .bundle()
        .pipe(source(file.replace(/\.ts/, '.min.js')))
        .pipe(buffer())
        .pipe(uglify())
        .pipe(size())
        .pipe(destination());
    }
  }));

  const nonEmptyStandalones = standaloneFiles ? standalones : gulp.noop();

  const watch = () => {

    const bundle = gulp.series([nonEmptyStandalones, () => bundler
      .bundle()
      .on('error', error => logger.error(colors.red(error.message)))
      .pipe(source(`${fileBaseName}.js`))
      .pipe(destination())
    ]);

    const bundler = watchify(
      browserify(Object.assign({}, watchify.args, browserifyOpts(true)))
      .plugin(tsify)
    ).on('update', bundle).on('log', logger.info);

    return bundle();
  };

    gulp.task('prod', prod);
    gulp.task('dev', gulp.series([standalones, dev]));
    gulp.task('default', watch);
  };
