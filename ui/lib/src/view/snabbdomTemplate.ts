/**
 * Snabbdom HTML template support based on Snabby: https://github.com/mreinstein/snabby
 */

import hyperx, { type HtmlTemplate } from 'hyperx';
import { type Classes, h } from 'snabbdom';

type ClassValue = string | Classes | Array<string | Classes>;

// Note: data.class and data.attrs.class are not allowed together, one will overwrite the other,
// hence we inject class strings to object, if object notation was used in the array
function collectClasses(clsValue: ClassValue): [string[], Classes | undefined] {
  const classNames: string[] = [];
  let classObject: Classes | undefined;

  function addClassName(className: string): void {
    for (const key of className.split(/\s+/)) {
      if (!key) continue;
      if (classObject) classObject[key] = true;
      else classNames.push(key);
    }
  }

  function visit(value: ClassValue): void {
    if (!value) return;

    if (typeof value === 'string') {
      addClassName(value);
    } else if (Array.isArray(value)) {
      for (const nested of value) visit(nested);
    } else {
      if (!classObject) {
        classObject = {};
        for (const key of classNames) classObject[key] = true;
        classNames.length = 0;
      }
      for (const key in value) classObject[key] = value[key];
    }
  }

  visit(clsValue);

  return [classNames, classObject];
}

function createVNode(tag: string, properties: Record<string, any>, content: any) {
  if (tag === '!--') return h('!', properties.comment);

  if (Array.isArray(content) && content.length) {
    content = content.length === 1 ? content[0] : content.flat();
  }

  const property = Object.keys(properties);
  if (!property?.length) return h(tag, content);

  const data = {} as Record<string, any>;
  let attrs;

  for (let i = 0; i < property.length; i++) {
    const name = property[i];
    let value = properties[name];

    if (name.startsWith('@')) {
      const parts = name.slice(1).split(':');

      if ((parts[0] !== 'attrs' || BOOLEAN_ATTRIBUTES.has(parts[1])) && value === 'false') {
        value = false;
      }

      let obj = data;
      for (let p = 0; p < parts.length - 1; p++) {
        const part = parts[p];
        obj = obj[part] || (obj[part] = {});
      }
      obj[parts[parts.length - 1]] = value;
      continue;
    }

    if (!attrs) attrs ||= data.attrs ||= {};

    if (name === 'class') {
      const [classNames, classObject] = collectClasses(value);
      if (classObject) {
        data.class = classObject;
      } else if (classNames?.length) {
        attrs.class = classNames.join(' ');
      }
      continue;
    }

    if (BOOLEAN_ATTRIBUTES.has(name) && value === 'false') value = false;

    attrs[name] = value;
  }

  return h(tag, data, content);
}

export const html: HtmlTemplate = hyperx(createVNode, {
  comments: true,
  attrToProp: false,
});

const BOOLEAN_ATTRIBUTES = new Set([
  'allowfullscreen',
  'async',
  'autofocus',
  'checked',
  'compact',
  'declare',
  'default',
  'defer',
  'disabled',
  'formnovalidate',
  'hidden',
  'inert',
  'ismap',
  'itemscope',
  'multiple',
  'multiple',
  'muted',
  'nohref',
  'noresize',
  'noshade',
  'novalidate',
  'nowrap',
  'open',
  'readonly',
  'required',
  'reversed',
  'seamless',
  'selected',
  'sortable',
  'truespeed',
  'typemustmatch',
  'contenteditable',
  'spellcheck',
]);
