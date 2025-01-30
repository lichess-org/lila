import { writeFile } from 'node:fs/promises';
import path from 'node:path';
import { prefix, signature, themes } from './constants.js';
import { type ThemeRecord, defaultTheme } from './types.js';

export async function generateCssVariables(
  themeVars: ThemeRecord,
  extracted: Set<string>,
  outDir: string,
): Promise<void> {
  themes.forEach(async theme => {
    let output = `${signature}
@use 'sass:color';
@use '../util' as *;
@use '../${defaultTheme}' as *;
${theme !== defaultTheme ? `@use '../${theme}' as *` : ''};
`;

    const sel = theme === defaultTheme ? 'html' : `html.${theme}`;
    output += `\n${sel} {\n`;

    const vars = themeVars[theme];
    const varKeys = Object.keys(vars).sort();

    varKeys.forEach(name => {
      const value = vars[name];
      if (
        theme !== defaultTheme &&
        !value.includes('c-') &&
        themeVars[defaultTheme][name] === value
      )
        console.warn('Redundant variable repetition:', `$${name}: ${value}`, 'in', theme);
      output += cssVariable(name, value);
    });

    Array.from(extracted).forEach(name => {
      const v = colorFunction(name, varKeys);
      if (v) output += cssVariable(name, v);
    });
    output += '}\n\n';

    await writeFile(path.join(outDir, `_${theme}-vars.scss`), output);
  });
}

function cssVariable(name: string, value: string): string {
  if (value.startsWith('"') || value.startsWith("'")) return `  --${name}: ${value};\n`;
  return `  --${name}: #{${value}};\n`;
}

function colorFunction(variable: string, themeKeys: string[]): string | undefined {
  function themifyColor(color: string): string {
    if (['black', 'white'].includes(color)) return color;
    else return `$c-${color}`;
  }

  const name = variable.substring(prefix.length);
  const parts = name.split('_');
  const length = parts.length;
  const percentagePart = parts[length - 1];
  const percentage = Number.parseInt(`${percentagePart.replace('neg', '-')}`);
  const func = parts[length - 2];
  const colors = parts.slice(0, -2).map(p => themifyColor(p));

  if (!colors.some(c => themeKeys.includes(c.split('$')[1]))) {
    // console.log('Skipping', variable, 'for', theme, 'theme');
    return;
  }

  const mixDups = new Set<string>();
  if (func === 'mix') {
    const normalized = normalizeMixVariable(colors, percentage);
    if (mixDups.has(normalized)) console.warn('Found duplicates:', normalized);
    else mixDups.add(normalized);

    return `color.mix(${[colors[0], colors[1], `${percentage}%`].join(',')})`;
  } else {
    const value = func.endsWith('alpha') ? (percentage / 100).toString() : `${percentage}%`;
    return `color.${func.startsWith('c-') ? 'change' : 'adjust'}(${colors[0]},$${func.replace('c-', '')}:${value})`;
  }
}

// $m-accent_bg-box_mix_10
// $m-bg-box_accent_mix_90
function normalizeMixVariable(colors: string[], percentage: number): string {
  const sortedCols = colors.slice().sort();
  const sortedPerc = sortedCols[0] === colors[0] ? percentage : 100 - percentage;
  return `${prefix}${[...sortedCols, sortedPerc, 'mix'].join('_')}`;
}
