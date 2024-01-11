import { ROLES, Role } from 'shogiops/types';
import { speeds, variants } from './types';

export function toPercentage(n: number, total: number): number {
  return total ? +((n / total) * 100).toFixed(2) : 0;
}

export function fixed(n: number | undefined, digits: number = 2): number {
  return +(n || 0).toFixed(digits);
}

export function roleToIndex(role: Role): number {
  return ROLES.indexOf(role);
}

export function idFromVariant(variant: VariantKey): number {
  return variants.indexOf(variant) + 1;
}

export function idFromSpeed(speed: Speed): number {
  return speeds.indexOf(speed);
}
