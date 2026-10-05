import { api } from 'lib/api';
import { licon } from 'lib/licon';
import { profileUrl } from 'lib/view/userLink';

type TitleName = string;

interface Friend {
  id: string;
  name: string;
  title?: string;
  playing: boolean;
  patronColor?: PatronColor;
}

export default class OnlineFriends {
  titleEl: HTMLElement;
  countEl: HTMLElement;
  loaded = false;
  receivedOnlineFriends = false;
  users: Map<string, Friend>;

  constructor(readonly el: HTMLElement) {
    this.titleEl = this.el.querySelector('.friend_box_button') as HTMLElement;
    this.countEl = this.el.querySelector('.friend_box_count') as HTMLElement;
    this.countEl.innerHTML = i18n.site.nbFriendsOnline(0, '-');
    this.updateTitle(0, 0);
    this.titleEl.addEventListener('click', this.load);
    this.titleEl.addEventListener('mouseover', this.load);
    this.users = new Map();
    api.onlineFriends.events.on('onlines', this.receive);
    api.onlineFriends.events.on('enters', this.enters);
    api.onlineFriends.events.on('leaves', this.leaves);
    api.onlineFriends.events.on('playing', this.playing);
    api.onlineFriends.events.on('stopped_playing', this.stoppedPlaying);
  }

  load = () => {
    if (!this.loaded) {
      this.loaded = true;
      api.onlineFriends.request();
    }
  };

  receive = (friends: TitleName[], msg: { playing: string[]; patronColors: PatronColor[] }) => {
    this.users.clear();
    this.receivedOnlineFriends = true;
    friends.forEach((f, i) => {
      const friend = this.insert(f);
      friend.patronColor = msg.patronColors[i];
      friend.playing = msg.playing.includes(friend.id!);
    });
    this.repaint();
  };

  repaint = () => {
    if (this.receivedOnlineFriends)
      requestAnimationFrame(() => {
        const ids = Array.from(this.users.keys()).sort();
        const onTv = Array.from(this.users.values()).filter(friend => friend.playing).length;
        this.updateTitle(ids.length, onTv);
        this.countEl.innerHTML = i18n.site.nbFriendsOnline(ids.length, `<strong>${ids.length}</strong>`);
        this.el.querySelector('.nobody')?.classList.toggle('none', !!ids[0]);
        this.el.querySelector('.list')!.innerHTML = ids
          .map(id => this.renderFriend(this.users.get(id)!))
          .join('');
      });
  };

  updateTitle = (online: number, onTv: number) => {
    const title = i18n.site.friendsOnlineAndOnTv(online, onTv);
    this.titleEl.title = title;
    this.titleEl.setAttribute('aria-label', title);
  };

  renderFriend = (friend: Friend) => {
    const patronCls = friend.patronColor ? ` patron paco${friend.patronColor}` : '';
    const icon = `<icon class="line${patronCls}"></icon>`;
    const titleTag = friend.title
      ? `<span class="utitle"${friend.title === 'BOT' ? ' data-bot' : ''}>${friend.title}</span>&nbsp;`
      : '';
    const url = profileUrl(friend.name);
    const tvButton = friend.playing
      ? `<a data-icon="${licon.AnalogTv}" class="tv ulpt" data-pt-pos="nw" href="${url}/tv" data-href="${url}"></a>`
      : '';
    return `<div><a class="online user-link ulpt" data-pt-pos="nw" href="${url}">${icon}${titleTag}${friend.name}</a>${tvButton}</div>`;
  };

  enters = (titleName: TitleName, msg: { playing: boolean; patronColor?: PatronColor }) => {
    const friend = this.insert(titleName);
    friend.playing = msg.playing;
    friend.patronColor = msg.patronColor;
    this.repaint();
  };

  leaves = (titleName: TitleName) => {
    this.users.delete(this.getId(titleName));
    this.repaint();
  };

  playing = (titleName: TitleName) => {
    this.insert(titleName).playing = true;
    this.repaint();
  };

  stoppedPlaying = (titleName: TitleName) => {
    this.insert(titleName).playing = false;
    this.repaint();
  };

  insert = (titleName: TitleName): Partial<Friend> => {
    const id = this.getId(titleName);
    const found = this.users.get(id);
    if (found) return found;
    const newFriend = this.toFriend(titleName);
    this.users.set(id, newFriend);
    return newFriend;
  };

  getId = (titleName: TitleName) => titleName.toLowerCase().replace(/^\w+\s/, '');

  toFriend = (titleName: TitleName): Friend => {
    const split = titleName.split(' ');
    return {
      id: split[split.length - 1].toLowerCase(),
      name: split[split.length - 1],
      title: split.length > 1 ? split[0] : undefined,
      playing: false,
    };
  };
}
