import { isTouchDevice } from 'common/device';
import { escapeHtml } from 'common';

export const registerMultipleSelect = () => {
  $.fn.multipleSelectHover = function (fnOver, fnOut) {
    return this.on('mouseenter', fnOver).on('mouseleave', fnOut || fnOver);
  };

  function isVisible(this: EleLoose) {
    const display = window.getComputedStyle(this).display;
    return !!display && display != 'none';
  }

  class MultipleSelectState {
    options: MultiSelectOpts;

    $el: Cash;
    $label: Cash | null;
    $parent: Cash;
    $choice: Cash;
    $drop: Cash;

    $searchInput: Cash;
    $selectAll: Cash;
    $selectGroups: Cash;
    $selectItems: Cash;
    $disableItems: Cash;
    $noResults: Cash;

    selectAllName: string;
    selectGroupName: string;
    selectItemName: string;

    constructor($el: Cash, options: MultiSelectOpts) {
      const that = this,
        name = $el.attr('name') || options.name || '';
      this.options = options;
      this.$el = $el.hide();
      this.$label =
        this.$el.closest('label') ||
        (this.$el.attr('id') && $(`label[for="${this.$el.attr('id')?.replace(/:/g, '\\:')}"]`));
      this.$parent = $(`<div class="ms-parent ${$el.attr('class') || ''}"/>`);
      this.$choice = $(
        [
          '<button type="button" class="ms-choice">',
          `<span class="placeholder">${this.options.placeholder}</span>`,
          '<div></div>',
          '</button>',
        ].join(''),
      );
      this.$drop = $(`<div class="ms-drop ${this.options.position}"/>`);
      this.$el.after(this.$parent);
      this.$parent.append(this.$choice);
      this.$parent.append(this.$drop);
      if (this.$el.prop('disabled')) {
        this.$choice.addClass('disabled');
      }
      this.$parent.css('width', this.options.width || this.$el.css('width') || this.$el.outerWidth() + 20);
      this.selectAllName = `data-name="selectAll${name}"`;
      this.selectGroupName = `data-name="selectGroup${name}"`;
      this.selectItemName = `data-name="selectItem${name}"`;
      if (!this.options.keepOpen) {
        $(document).on('click', function (e) {
          if (
            $(e.target)[0] === that.$choice[0] ||
            $(e.target).parents('.ms-choice')[0] === that.$choice[0]
          ) {
            return;
          }
          if (
            ($(e.target)[0] === that.$drop[0] ||
              ($(e.target).parents('.ms-drop')[0] !== that.$drop[0] && e.target !== $el[0])) &&
            that.options.isOpen
          ) {
            that.close();
          }
        });
      }
    }

    init() {
      const that = this,
        $ul = $('<ul></ul>');
      this.$drop.html('');
      if (this.options.filter) {
        this.$drop.append(
          [
            '<div class="ms-search">',
            '<input type="text" autocomplete="off" autocorrect="off" autocapitalize="off" spellcheck="false">',
            '</div>',
          ].join(''),
        );
      }
      if (this.options.selectAll && !this.options.single) {
        $ul.append(
          [
            '<li class="ms-select-all">',
            '<label>',
            `<input type="checkbox" ${this.selectAllName} /> `,
            this.options.selectAllDelimiter?.[0] || '',
            this.options.selectAllText,
            this.options.selectAllDelimiter?.[1] || '',
            '</label>',
            '</li>',
          ].join(''),
        );
      }
      $.each(this.$el.children(), function (i, elm) {
        $ul.append(that.optionToHtml(i, elm!));
      });
      $ul.append(`<li class="ms-no-results">${this.options.noMatchesFound}</li>`);
      this.$drop.append($ul);
      this.$drop.find('ul').css('max-height', this.options.maxHeight + 'px');
      this.$drop.find('.multiple').css('width', this.options.multipleWidth + 'px');
      this.$searchInput = this.$drop.find('.ms-search input');
      this.$selectAll = this.$drop.find('input[' + this.selectAllName + ']');
      this.$selectGroups = this.$drop.find('input[' + this.selectGroupName + ']');
      this.$selectItems = this.$drop.find('input[' + this.selectItemName + ']:enabled');
      this.$disableItems = this.$drop.find('input[' + this.selectItemName + ']:disabled');
      this.$noResults = this.$drop.find('.ms-no-results');
      this.events();
      this.updateSelectAll(true);
      this.update(true);
      if (this.options.isOpen) {
        this.open();
      }
    }

    optionToHtml(i: number, elm: EleLoose, group?: string, groupDisabled?: boolean) {
      const that = this,
        $elm = $(elm),
        classes = $elm.attr('class') || '',
        multiple = this.options.multiple ? 'multiple' : '',
        type = this.options.single ? 'radio' : 'checkbox';
      let disabled: boolean;
      if ($elm.is('option')) {
        const value = $elm.val() as string,
          text = that.options.textTemplate?.($elm) || '',
          selected = $elm.prop('selected'),
          optionalStyle = this.options.styler?.(value),
          style = optionalStyle ? `style="${optionalStyle}"` : '';
        disabled = groupDisabled || $elm.prop('disabled');
        const $el = $(
          [
            `<li class="${multiple} ${classes}" ${style}>`,
            `<label class="${disabled ? 'disabled' : ''}">`,
            `<input type="${type}" value="${escapeHtml(value)}" ${this.selectItemName} ${
              selected ? 'checked' : ''
            } ${disabled ? 'disabled' : ''} ${group ? `data-group="${group}"` : ''}>`,
            text,
            '</label>',
            '</li>',
          ].join(''),
        );
        return $el;
      }
      if ($elm.is('optgroup')) {
        const label = that.options.labelTemplate?.($elm),
          $group = $('<div/>');
        group = 'group_' + i;
        disabled = $elm.prop('disabled');
        $group.append(
          [
            '<li class="group">',
            `<label class="optgroup ${disabled ? 'disabled' : ''}" data-group="${group}">`,
            this.options.hideOptgroupCheckboxes || this.options.single
              ? ''
              : `<input type="checkbox" ${this.selectGroupName} ${disabled ? 'disabled' : ''}>`,
            label,
            '</label>',
            '</li>',
          ].join(''),
        );
        $.each($elm.children(), function (i, elm) {
          $group.append(that.optionToHtml(i, elm!, group, disabled));
        });
        return $group.html();
      }
      return;
    }

    events() {
      const that = this,
        toggleOpen: EventCallback = function (e) {
          e.preventDefault();
          that[that.options.isOpen ? 'close' : 'open']();
        };
      if (this.$label) {
        this.$label.off('click').on('click', function (this: HTMLElement, e) {
          if (e.target.nodeName.toLowerCase() !== 'label' || e.target !== this) {
            return;
          }
          toggleOpen(e);
          if (!that.options.filter || !that.options.isOpen) {
            that.focus();
          }
          e.stopPropagation();
        });
      }
      this.$choice
        .off('click')
        .on('click', toggleOpen)
        .off('focus')
        .on('focus', this.options.onFocus!)
        .off('blur')
        .on('blur', this.options.onBlur!);
      if (!isTouchDevice())
        this.$choice
          .parent()
          .off('mouseover')
          .off('mouseout')
          .multipleSelectHover(that.open.bind(that), that.close.bind(that));
      this.$parent.off('keydown').on('keydown', function (e) {
        switch (e.which) {
          case 27:
            that.close();
            that.$choice[0]?.focus();
            break;
        }
      });
      this.$searchInput
        .off('keydown')
        .on('keydown', function (e) {
          if (e.keyCode === 9 && e.shiftKey) {
            that.close();
          }
        })
        .off('keyup')
        .on('keyup', function (e) {
          if (
            that.options.filterAcceptOnEnter &&
            (e.which === 13 || e.which == 32) &&
            that.$searchInput.val()
          ) {
            that.$selectAll[0]?.click();
            that.close();
            that.focus();
            return;
          }
          that.filter();
        });
      this.$selectAll.off('click').on('click', function (this: HTMLElement) {
        const checked = $(this).prop('checked'),
          $items = that.$selectItems.filter(isVisible);
        if ($items.length === that.$selectItems.length) {
          that[checked ? 'checkAll' : 'uncheckAll']();
        } else {
          that.$selectGroups.prop('checked', checked);
          $items.prop('checked', checked);
          that.options[checked ? 'onCheckAll' : 'onUncheckAll']?.();
          that.update();
        }
      });
      this.$selectGroups.off('click').on('click', function (this: HTMLElement) {
        const group = $(this).parent().attr('data-group'),
          $items = that.$selectItems.filter(isVisible),
          $children = $items.filter(`[data-group="${group}"]`),
          checked = $children.length !== $children.filter(':checked').length;
        $children.prop('checked', checked);
        that.updateSelectAll();
        that.update();
        that.options.onOptgroupClick?.({
          label: $(this).parent().text(),
          checked: checked,
          children: $children.get(),
          instance: that,
        });
      });
      this.$selectItems.off('click').on('click', function (this: HTMLElement) {
        that.updateSelectAll();
        that.update();
        that.updateOptGroupSelect();
        that.options.onClick?.({
          label: $(this).parent().text(),
          value: $(this).val() as string,
          checked: $(this).prop('checked'),
          instance: that,
        });
        if (that.options.single && that.options.isOpen && !that.options.keepOpen) {
          that.close();
        }
        if (that.options.single) {
          const clickedVal = $(this).val();
          that.$selectItems
            .filter(function () {
              return $(this).val() !== clickedVal;
            })
            .each(function () {
              $(this).prop('checked', false);
            });
          that.update();
        }
      });
    }

    open() {
      if (this.$choice.hasClass('disabled')) {
        return;
      }
      this.options.isOpen = true;
      this.$choice.find('div').addClass('open');
      this.$drop.show();
      this.$selectAll.parent().show();
      this.$noResults.hide();
      if (!this.$el.children().length) {
        this.$selectAll.parent().hide();
        this.$noResults.show();
      }
      if (this.options.filter) {
        this.$searchInput.val('')[0]?.focus();
        this.filter();
      }
      this.options.onOpen?.();
    }

    close() {
      this.options.isOpen = false;
      this.$choice.find('div').removeClass('open');
      this.$drop.hide();
      this.options.onClose?.();
    }

    animateMethod(method: 'show' | 'hide') {
      const methods = {
        show: { fade: 'fadeIn', slide: 'slideDown' },
        hide: { fade: 'fadeOut', slide: 'slideUp' },
      };
      return methods[method][this.options.animate as 'fade' | 'slide'] || method;
    }

    update(isInit?: boolean) {
      const selects = this.options.displayValues ? this.getSelects() : this.getSelects('text'),
        $span = this.$choice.find('span'),
        sl = selects.length;
      this.$choice.toggleClass('selected', sl > 0);
      if (sl === 0) {
        $span.addClass('placeholder').html(this.options.placeholder!);
      } else if (this.options.allSelected && sl === this.$selectItems.length + this.$disableItems.length) {
        $span.removeClass('placeholder').html(this.options.allSelected);
      } else if (this.options.ellipsis && sl > this.options.minimumCountSelected!) {
        $span
          .removeClass('placeholder')
          .text(selects.slice(0, this.options.minimumCountSelected).join(this.options.delimiter) + '...');
      } else if (this.options.countSelected && sl > this.options.minimumCountSelected!) {
        $span
          .removeClass('placeholder')
          .html(
            this.options.countSelected
              .replace('#', selects.length + '')
              .replace('%', this.$selectItems.length + this.$disableItems.length + ''),
          );
      } else {
        $span.removeClass('placeholder').text(selects.join(this.options.delimiter));
      }
      if (this.options.addTitle) {
        $span.prop('title', this.getSelects('text'));
      }
      this.$el.val(this.getSelects()).trigger('change');
      this.$drop.find('li').removeClass('selected');
      this.$drop.find(`input[${this.selectItemName}]:checked`).each(function () {
        $(this).parents('li').first().addClass('selected');
      });
      if (!isInit) {
        this.$el.trigger('change');
      }
    }

    updateSelectAll(isInit?: boolean) {
      let $items = this.$selectItems;
      if (!isInit) {
        $items = $items.filter(isVisible);
      }
      this.$selectAll.prop('checked', $items.length && $items.length === $items.filter(':checked').length);
      if (!isInit && this.$selectAll.prop('checked')) {
        this.options.onCheckAll?.();
      }
    }

    updateOptGroupSelect() {
      const $items = this.$selectItems.filter(isVisible);
      $.each(this.$selectGroups, function (_i, val) {
        const group = $(val).parent().attr('data-group'),
          $children = $items.filter(`[data-group="${group}"]`);
        $(val).prop('checked', $children.length && $children.length === $children.filter(':checked').length);
      });
    }

    getSelects(type?: 'text') {
      const that = this,
        values: string[] = [];
      let texts: string[] = [];
      this.$drop.find(`input[${this.selectItemName}]:checked`).each(function () {
        texts.push($(this).parents('li').first().text());
        values.push($(this).val() as string);
      });
      if (type === 'text' && this.$selectGroups.length) {
        texts = [];
        this.$selectGroups.each(function () {
          const html = [],
            text = $(this).parent().text().trim(),
            group = $(this).parent().data('group'),
            $children = that.$drop.find(`[${that.selectItemName}][data-group="${group}"]`),
            $selected = $children.filter(':checked');
          if (!$selected.length) {
            return;
          }
          html.push('[');
          html.push(text);
          if ($children.length > $selected.length) {
            const list: string[] = [];
            $selected.each(function () {
              list.push($(this).parent().text());
            });
            html.push(': ' + list.join(', '));
          }
          html.push(']');
          texts.push(html.join(''));
        });
      }
      return type === 'text' ? texts : values;
    }

    setSelects(values: string[]) {
      const that = this;
      this.$selectItems.prop('checked', false);
      $.each(values, function (_i, value) {
        that.$selectItems.filter(`[value="${value}"]`).prop('checked', true);
      });
      this.$selectAll.prop(
        'checked',
        this.$selectItems.length === this.$selectItems.filter(':checked').length,
      );
      $.each(that.$selectGroups, function (_i, val) {
        const group = $(val).parent().attr('data-group'),
          $children = that.$selectItems.filter(`[data-group="${group}"]`);
        $(val).prop('checked', $children.length && $children.length === $children.filter(':checked').length);
      });
      this.update();
    }

    enable() {
      this.$choice.removeClass('disabled');
    }

    disable() {
      this.$choice.addClass('disabled');
    }

    checkAll() {
      this.$selectItems.prop('checked', true);
      this.$selectGroups.prop('checked', true);
      this.$selectAll.prop('checked', true);
      this.update();
      this.options.onCheckAll?.();
    }

    uncheckAll() {
      this.$selectItems.prop('checked', false);
      this.$selectGroups.prop('checked', false);
      this.$selectAll.prop('checked', false);
      this.update();
      this.options.onUncheckAll?.();
    }

    focus() {
      this.$choice[0]?.focus();
      this.options.onFocus?.();
    }

    blur() {
      this.$choice.trigger('blur');
      this.options.onBlur?.();
    }

    refresh() {
      this.init();
    }

    filter() {
      const that = this,
        text = (this.$searchInput.val() as string).trim().toLowerCase();
      if (text.length === 0) {
        this.$selectAll.parent().show();
        this.$selectItems.parent().show();
        this.$disableItems.parent().show();
        this.$selectGroups.parent().show();
        this.$noResults.hide();
      } else {
        this.$selectItems.each(function () {
          const $parent = $(this).parent();
          $parent[$parent.text().toLowerCase().indexOf(text) < 0 ? 'hide' : 'show']();
        });
        this.$disableItems.parent().hide();
        this.$selectGroups.each(function () {
          const $parent = $(this).parent(),
            group = $parent.attr('data-group'),
            $items = that.$selectItems.filter(isVisible);
          $parent[$items.filter(`[data-group="${group}"]`).length ? 'show' : 'hide']();
        });
        if (this.$selectItems.parent().filter(isVisible).length) {
          this.$selectAll.parent().show();
          this.$noResults.hide();
        } else {
          this.$selectAll.parent().hide();
          this.$noResults.show();
        }
      }
      this.updateOptGroupSelect();
      this.updateSelectAll();
      this.options.onFilter?.(text);
    }
  }

  type AllowedMethod =
    | 'getSelects'
    | 'setSelects'
    | 'enable'
    | 'disable'
    | 'open'
    | 'close'
    | 'checkAll'
    | 'uncheckAll'
    | 'focus'
    | 'blur'
    | 'refresh'
    | 'close';
  $.fn.multipleSelect = function (this: Cash) {
    const option = arguments[0],
      args = arguments;
    let value;
    interface EleHack extends EleLoose {
      multipleSelect: MultipleSelectState;
    }
    this.each(function (this: EleHack) {
      let data = this['multipleSelect'];
      const $this = $(this),
        options = {
          ...$.fn.multipleSelectDefaults,
          ...(typeof option === 'object' ? option : {}),
        };
      if (!data) {
        data = new MultipleSelectState($this, options);
        this['multipleSelect'] = data;
      }
      if (typeof option === 'string') {
        value = data[option as AllowedMethod](args[1]);
      } else {
        data.init();
        if (typeof args[1] === 'string') {
          value = (data[args[1] as AllowedMethod] as any).apply(data, [].slice.call(args, 2));
        }
      }
    });
    return typeof value !== 'undefined' ? value : this;
  } as any; // cast to deal with overloads

  $.fn.multipleSelectDefaults = {
    name: '',
    isOpen: false,
    placeholder: '',
    selectAll: true,
    selectAllDelimiter: ['[', ']'],
    minimumCountSelected: 3,
    ellipsis: false,
    multiple: false,
    multipleWidth: 80,
    single: false,
    filter: false,
    width: undefined,
    dropWidth: undefined,
    maxHeight: undefined,
    position: 'bottom',
    keepOpen: false,
    animate: 'none',
    displayValues: false,
    delimiter: ', ',
    addTitle: false,
    filterAcceptOnEnter: false,
    hideOptgroupCheckboxes: false,
    selectAllText: 'Select all',
    allSelected: 'All selected',
    countSelected: '# of % selected',
    noMatchesFound: 'No matches found',
    styler: function () {
      return null;
    },
    textTemplate: function ($elm) {
      return $elm.text();
    },
    labelTemplate: function ($elm) {
      return $elm.attr('label');
    },
    onOpen: function () {
      return false;
    },
    onClose: function () {
      return false;
    },
    onCheckAll: function () {
      return false;
    },
    onUncheckAll: function () {
      return false;
    },
    onFocus: function () {
      return false;
    },
    onBlur: function () {
      return false;
    },
    onOptgroupClick: function () {
      return false;
    },
    onClick: function () {
      return false;
    },
    onFilter: function () {
      return false;
    },
  };
};
