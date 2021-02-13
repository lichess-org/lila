import zxcvbn from 'zxcvbn';

export function addPasswordChangeListener(id: string): void {
  const passwordInput = document.getElementById(id) as HTMLInputElement;
  passwordInput.addEventListener('input', () => {
    updatePasswordComplexityMeter(passwordInput.value);
  });
  // Update the meter if script loaded after user has already typed something
  if (passwordInput.value) {
    updatePasswordComplexityMeter(passwordInput.value);
  }
}

function updatePasswordComplexityMeter(password: string): void {
  const analysis = zxcvbn(password);
  updateMeter(analysis.score);
}

function updateMeter(score: number): void {
  const color = score > 3 ? 'green' : score > 2 ? '#ffc800' : score > 1 ? '#ff8000' : 'red';
  const meter = document.querySelector('.password-complexity-meter');
  const children = meter?.children || [];

  for (var i = 0; i < children.length; i++) {
    (children[i] as HTMLElement).style.backgroundColor = i < score ? color : '';
  }
}
