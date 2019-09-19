const gulp = require('gulp');
const source = require('vinyl-source-stream');
const buffer = require('vinyl-buffer');
const colors = require('ansi-colors');
const logger = require('fancy-log');
const watchify = require('watchify');
const browserify = require('browserify');
const terser = require('gulp-terser');
const size = require('gulp-size');
const tsify = require('tsify');

module.exports = (standalone, fileBaseName, dir) => {

  const browserifyOpts = (debug) => ({
    entries: [`${dir}/src/main.ts`],
    standalone: standalone,
    debug: debug
  });
  const destination = () => gulp.dest('../../public/compiled/');

  const prod = () => browserify(browserifyOpts(false))
    .plugin(tsify)
    .bundle()
    .pipe(source(`${fileBaseName}.min.js`))
    .pipe(buffer())
    .pipe(terser({safari10: true}))
    .pipe(size())
    .pipe(destination());

  const dev = () => browserify(browserifyOpts(true))
    .plugin(tsify)
    .bundle()
    .pipe(source(`${fileBaseName}.js`))
    .pipe(destination());

  const watch = () => {

    const bundle = () => bundler
      .bundle()
      .on('error', error => logger.error(colors.red(error.message)))
      .pipe(source(`${fileBaseName}.js`))
      .pipe(destination());

    const bundler = watchify(
      browserify(Object.assign({}, watchify.args, browserifyOpts(true)))
      .plugin(tsify)
    ).on('update', bundle).on('log', logger.info);

    return bundle();
  };

  gulp.task('prod', prod);
  gulp.task('dev', dev);
  gulp.task('default', watch);
};
