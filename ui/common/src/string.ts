export function capitalize(str: string): string {
  return str.charAt(0).toLocaleUpperCase() + str.slice(1).toLocaleLowerCase();
}
