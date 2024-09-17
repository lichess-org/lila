site.load.then(() => {
  document.querySelectorAll('.slist td').forEach((td: HTMLElement) => {
    if (td.textContent === 'YES') td.style.color = 'green';
    else if (td.textContent === 'NO') td.style.color = 'red';
  });
});