export interface YoutubeMatch {
  videoType: VideoType;
  videoId: string;
  startTime: number;
}
type VideoType = 'watch' | 'embed' | 'shorts' | 'live' | 'playlist';
type DomainType = 'youtube.com' | 'youtu.be';

const supportedVideoTypes: string[] = ['watch', 'embed', 'shorts', 'live', 'playlist'] as const;
const videoIdValidLength = 11;
const videoIdRegex: RegExp = /^[a-zA-Z0-9_-]{11}$/;

export function parseYoutubeUrl(url: string): YoutubeMatch | undefined {
  const youtubeUrl = toURL(url);
  if (!youtubeUrl) {
    return;
  }

  switch (getDomainType(youtubeUrl.hostname)) {
    case 'youtu.be':
      return handleYoutuBe(youtubeUrl);
    case 'youtube.com':
      return handleYoutubeCom(youtubeUrl);
  }
}

export function embedYoutubeUrl(match: YoutubeMatch): string {
  const params = new URLSearchParams({
    modestbranding: '1',
    rel: '0',
    controls: '2',
    iv_load_policy: '3',
  });

  if (match.videoType === 'playlist') {
    params.append('list', match.videoId);
    return `https://www.youtube-nocookie.com/embed/videoseries?${params.toString()}`;
  }

  if (match.startTime > 0) {
    params.append('start', match.startTime.toString());
  }
  return `https://www.youtube-nocookie.com/embed/${match.videoId}?${params.toString()}`;
}

function getDomainType(hostname: string): DomainType | undefined {
  if (['www.youtube.com', 'm.youtube.com', 'youtube.com'].includes(hostname)) {
    return 'youtube.com';
  }

  if ('youtu.be' === hostname) {
    return 'youtu.be';
  }
}

function handleYoutubeCom(url: URL): YoutubeMatch | undefined {
  const { pathname, searchParams } = url;

  const parsedResult = parseVideoPath(pathname);
  const { videoType } = parsedResult;
  let { videoId } = parsedResult;

  if (!videoType) {
    return;
  }

  let startTimeParamName = 't';
  switch (videoType) {
    case 'watch':
      videoId = searchParams.get('v');
      break;
    case 'shorts':
    case 'live':
      // no-op
      // videoId already handled in parseVideoPath
      break;
    case 'embed':
      // videoId already handled in parseVideoPath
      startTimeParamName = 'start';
      break;
    case 'playlist':
      const playlistId = searchParams.get('list');
      if (!playlistId || !isPlaylistIdValid(playlistId)) {
        return;
      }

      return {
        videoType,
        videoId: playlistId,
        startTime: 0,
      };
  }

  if (!isVideoIdValid(videoId) || !videoId) {
    return;
  }
  const startTime = extractStartTime(searchParams.get(startTimeParamName) ?? '');

  return {
    videoType,
    videoId,
    startTime,
  };
}

function handleYoutuBe(url: URL): YoutubeMatch | undefined {
  const { pathname, searchParams } = url;

  const [videoId] = getPathSegments(pathname);
  if (!isVideoIdValid(videoId) || !videoId) {
    return;
  }

  const startTimeParamName = 't';
  const startTime = extractStartTime(searchParams.get(startTimeParamName) ?? '');

  return {
    videoType: 'watch', // youtube fall-backs to 'watch' even for live youtu.be
    videoId,
    startTime,
  };
}

function getPathSegments(path: string): string[] {
  return path
    .replace(/\/+$/, '') // remove trailing slashes
    .split('/')
    .filter(Boolean);
}

function parseVideoPath(path: string): {
  videoType?: VideoType;
  videoId?: string | null;
} {
  const [type, id] = getPathSegments(path);

  const videoType = supportedVideoTypes.includes(type as VideoType) ? (type as VideoType) : undefined;

  const videoId = isVideoIdValid(id) ? id : undefined;

  return { videoType, videoId };
}

const isVideoIdValid = (id?: string | null) =>
  !!id && id.length === videoIdValidLength && videoIdRegex.test(id);

const isPlaylistIdValid = (id?: string | null) => !!id && id.length >= 18 && id.length <= 34;

function extractStartTime(value: string): number {
  let start = 0;

  if (!value) {
    return 0;
  }

  if (/^\d+$/.test(value)) {
    start = parseInt(value, 10);
  } else {
    // Parse time format like 1h2m3s
    const timeMatch = value.match(/(?:(\d+)h)?(?:(\d+)m)?(?:(\d+)s)?/);
    if (timeMatch) {
      const hours = parseInt(timeMatch[1] || '0', 10);
      const minutes = parseInt(timeMatch[2] || '0', 10);
      const seconds = parseInt(timeMatch[3] || '0', 10);
      start = hours * 3600 + minutes * 60 + seconds;
    }
  }
  return start;
}

function toURL(url: string): URL | undefined {
  try {
    return new URL(url);
  } catch {
    return;
  }
}
