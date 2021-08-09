import { Rules } from 'chessops/types';

export function variantToRules(variant?: string | null): Rules {
  switch (variant) {
    case 'threeCheck':
      return '3check';
    case 'kingOfTheHill':
      return 'kingofthehill';
    case 'racingKings':
      return 'racingkings';
    case 'antichess':
    case 'atomic':
    case 'horde':
    case 'crazyhouse':
      return variant;
    default:
      return 'chess';
  }
}

export function rulesToVariant(rules: Rules): VariantKey {
  switch (rules) {
    case 'chess':
      return 'standard';
    case '3check':
      return 'threeCheck';
    case 'kingofthehill':
      return 'kingOfTheHill';
    case 'racingkings':
      return 'racingKings';
    case 'antichess':
    case 'atomic':
    case 'horde':
    case 'crazyhouse':
      return rules;
  }
}
