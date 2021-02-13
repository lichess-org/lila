export default function (select: HTMLSelectElement) {
  const max = 5,
    $select = $(select),
    $rate = $('<rate-stars>').addClass('rate-active').insertAfter(select),
    getValue = () => parseInt($select.val() as string),
    $stars = Array(max)
      .fill(0)
      .map((_, i: number) =>
        $('<star>')
          .data('v', i + 1)
          .appendTo($rate)
      ),
    setClasses = () =>
      requestAnimationFrame(() => {
        const v = hovered || getValue();
        $stars.map(($star, i) => $star.toggleClass('rate-selected', i <= v - 1));
      });

  let hovered = 0;

  setClasses();

  $rate
    .on('mouseenter', 'star', function (this: HTMLElement) {
      hovered = parseInt(this.getAttribute('data-v')!);
      setClasses();
    })
    .on('mouseleave', 'star', function (this: HTMLElement) {
      hovered = 0;
      setClasses();
    })
    .on('click', 'star', function (this: HTMLElement) {
      $select.val(this.getAttribute('data-v')!);
      setClasses();
    });
}
