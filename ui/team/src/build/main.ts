import { makeChat } from 'chat';
import type { TeamData } from '../interface';

function main(data: TeamData) {
  window.lishogi.socket = new window.lishogi.StrongSocket(`/team/${data.id}`, data.socketVersion);
  data.chat && makeChat(data.chat);
  $('#team-subscribe').on('change', function (this: HTMLInputElement) {
    $(this)
      .parents('form')
      .each(function (this: HTMLFormElement) {
        window.lishogi.xhr.formToXhr(this);
      });
  });
}

window.lishogi.registerModule(__bundlename__, main);
