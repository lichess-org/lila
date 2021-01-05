export default function changeColorHandle(): void {
  const dict: { [theme: string]: [string] } = {
    'solid-natural': ['#3e3423'],
    'kaya1': ['#503a1c'],
    'kaya2': ['#3a2b11'],
    'oak': ['#352b1b'],
    'solid-brown1': ['#150e09'],
    'solid-wood1': ['#6f5f2e'],
    'dark-blue': ['#000'],
    'Painting1': ['#332f27'],
    'Painting2': ['#35312a'],
    'Kinkaku': ['#141716'],
    'space1': ['#707070'],
    'space2': ['#b3b3b3'],
    'whiteBoard': ['#3e3e3e'],
    'darkBoard': ['#808080'],
    'doubutsu': ['#c97f69']
  };

  for (const theme of $('body').attr('class').split(' ')) {
    if (theme in dict) {
      document.documentElement.style.setProperty('--cg-coord-color', dict[theme][0]);
      document.documentElement.style.setProperty('--cg-coord-shadow', 'none');
    }
  }
}
