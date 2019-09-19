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

module.exports = (plugins) => {

  const destination = () => gulp.dest(`../../public/compiled/`);

  const mainProd = gulp.task('prod');

  const pluginsProd = plugins.map(opts => {
    return () => browserify(opts)
      .plugin(tsify)
      .bundle()
      .pipe(source(opts.target))
      .pipe(buffer())
      .pipe(terser({safari10: true}))
      .pipe(size())
      .pipe(destination());
  });

  gulp.task('prod', gulp.series(pluginsProd.concat(mainProd)));

  const pluginWatch = (opts) => () => {

    const bundle = () => bundler
      .bundle()
      .on('error', error => logger.error(colors.red(error.message)))
      .pipe(source(opts.target))
      .pipe(destination());

    const bundler = watchify(
      browserify(Object.assign({}, watchify.args, {...opts, debug: true}))
      .plugin(tsify)
    ).on('update', bundle).on('log', logger.info);

    return bundle();
  };

  plugins.map(plugin => {
    gulp.task(plugin.standalone, pluginWatch(plugin));
  });
}
