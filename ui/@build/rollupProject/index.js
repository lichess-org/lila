const resolve = require('@rollup/plugin-node-resolve').default;
const commonjs = require('@rollup/plugin-commonjs');
const typescript = require('@rollup/plugin-typescript');
const terser = require('rollup-plugin-terser').terser;

exports.rollupProject = (targets) => {
  return (args) => {
    const prod = args['config-prod'];
    const target = targets[args['config-plugin'] || 'main'];
    return {
      input: target.input,
      output: [
        prod ? {
          file: `../../public/compiled/${target.output}.min.js`,
          format: 'iife',
          name: target.name,
          plugins: [
            terser({
              output: {
                comments: false,
              },
            }),
          ],
        } : {
          file: `../../public/compiled/${target.output}.js`,
          format: 'iife',
          name: target.name,
        }
      ],
      plugins: [
        ...(target.plugins || []),
        resolve(),
        ...(target.js ? [] : [typescript()]),
        commonjs({
          extensions: ['.js', '.ts'],
        }),
      ],
    };
  };
};
