export const focusQuery =
  'button, input, select, textarea, [href], [tabindex], [role="tab"], [role="button"], [role="link"]';

export function previousFocusable(current: Element | null = document.activeElement): HTMLElement | null {
  const focii = focusableWithin(document.body);
  const index = focii.indexOf(current as HTMLElement);
  return index > 0 ? focii[index - 1] : null;
}

export function nextFocusable(current: Element | null = document.activeElement): HTMLElement | null {
  const focii = focusableWithin(document.body);
  const index = focii.indexOf(current as HTMLElement);
  return index !== -1 && index < focii.length - 1 ? focii[index + 1] : null;
}

export function focusableWithin(container: HTMLElement): HTMLElement[] {
  return [...container.querySelectorAll<HTMLElement>(focusQuery)].filter(
    el =>
      el.tabIndex !== -1 &&
      el.checkVisibility({ visibilityProperty: true }) &&
      !el.matches(':disabled') &&
      !el.closest('[inert]'),
  );
}
