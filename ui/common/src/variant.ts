export function variantToId(variant: VariantKey): number {
  switch (variant) {
    case 'minishogi':
      return 2;
    case 'chushogi':
      return 3;
    case 'annanshogi':
      return 4;
    case 'kyotoshogi':
      return 5;
    case 'checkshogi':
      return 6;
    default:
      return 1;
  }
}

export function idToVariant(id: number | string): VariantKey {
  switch (typeof id === 'string' ? parseInt(id) : id) {
    case 2:
      return 'minishogi';
    case 3:
      return 'chushogi';
    case 4:
      return 'annanshogi';
    case 5:
      return 'kyotoshogi';
    case 6:
      return 'checkshogi';
    default:
      return 'standard';
  }
}
