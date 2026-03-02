export default function menuKeyboardInteractions(): void {
  if ('ontouchstart' in window) return;

  const $nav = $('#topnav');

  const handleKeyDown = (ev: KeyboardEvent) => {
    const $target = $(ev.target as HTMLElement | null);
    const $section = $target.parent().is('section') ? $target.parent() : $target.parent().parent();

    if (ev.code === 'Tab') {
      if (ev.shiftKey ? $target.is(':first-child') : $target.is(':last-child')) {
        $section.removeClass('active');
        return;
      } else if ($section.hasClass('active')) {
        return;
      }
    } else if (ev.code === 'Space') {
      $section.toggleClass('active');
      ev.preventDefault();
      ev.stopPropagation();
    } else if (!ev.shiftKey) {
      $section.removeClass('active');
    }
  };

  const handleFocusOut = (ev: FocusEvent) => {
    const focusTarget = ev.relatedTarget as HTMLElement | null;
    const hasFocus = focusTarget && ($nav[0] === focusTarget || $nav[0]?.contains(focusTarget));

    if (!hasFocus) {
      $nav.find('section.active').removeClass('active');
    }
  };

  const handleSwitchToMouse = () => {
    $nav.find('section.active').removeClass('active');
    (document.activeElement as HTMLElement | null)?.blur();
  };

  $nav.on('keydown', handleKeyDown).on('focusout', handleFocusOut).on('mouseover', handleSwitchToMouse);
}
