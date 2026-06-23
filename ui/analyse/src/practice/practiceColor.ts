export const practicePovColor = (variant: VariantKey, playerColor: Color, boardColor: Color): Color =>
  variant === 'racingKings' ? playerColor : boardColor;
