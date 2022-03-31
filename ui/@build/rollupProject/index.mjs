import resolve from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';
import typescript from '@rollup/plugin-typescript';
import { terser } from 'rollup-plugin-terser';

export const multi = targets => args =>
  targets.map(target => ({
    input: target.input,
    output: [
      {
        format: 'iife',
        name: target.name,
        globals: target.globals,
        generatedCode: {
          preset: 'es2015',
          constBindings: false, // for window[target.name]
        },
        ...(args['config-prod']
          ? {
              file: `../../public/compiled/${target.output}.min.js`,
              plugins: [
                terser({
                  safari10: true,
                  output: {
                    comments: false,
                  },
                  numWorkers: 1,
                }),
              ],
            }
          : {
              file: `../../public/compiled/${target.output}.js`,
            }),
      },
    ],
    plugins: [
      ...(target.plugins || []),
      resolve(),
      ...(target.js ? [] : [typescript()]),
      typescript(),
      commonjs({ extensions: ['.js'] }),
    ],
  }));

export default targets => args => multi([targets[args['config-plugin'] || 'main']])(args);
