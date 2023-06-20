// https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Roles/tab_role

export default function (panels: Cash | HTMLElement, onSelect: (id: string) => void) {
  $(panels)
    .find('.tab-list__tab')
    .on('click', e => {
      const panelId = e.target.getAttribute('aria-controls');

      // Remove all current selected tabs
      $(e.target.parentNode).find('[aria-selected="true"]').attr('aria-selected', 'false');

      // Set this tab as selected
      e.target.setAttribute('aria-selected', true);

      // Hide all tab panels
      $(panels).find('[role="tabpanel"]').addClass('none');

      // Show the selected panel
      $(panels).find(`#${panelId}`).removeClass('none');

      onSelect(panelId);
    });

  const selected = $(panels).find('[role="tab"][aria-selected="true"]').attr('aria-controls');
  if (selected) onSelect(selected);
}
