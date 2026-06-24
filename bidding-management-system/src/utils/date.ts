const VIETNAM_TIME_FORMATTER = new Intl.DateTimeFormat('vi-VN', {
  timeZone: 'Asia/Ho_Chi_Minh',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit'
});

const VIETNAM_DATE_FORMATTER = new Intl.DateTimeFormat('vi-VN', {
  timeZone: 'Asia/Ho_Chi_Minh',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit'
});

const VIETNAM_HOUR_MINUTE_FORMATTER = new Intl.DateTimeFormat('vi-VN', {
  timeZone: 'Asia/Ho_Chi_Minh',
  hour: '2-digit',
  minute: '2-digit'
});

export function formatVietnamDateTime(value: string | null | undefined): string {
  if (!value) {
    return 'Chưa có';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return VIETNAM_TIME_FORMATTER.format(date);
}

export function getTimeMs(value: string | null | undefined): number | null {
  if (!value) {
    return null;
  }

  const timeMs = new Date(value).getTime();
  return Number.isNaN(timeMs) ? null : timeMs;
}

export function formatDate(value: string | null | undefined, placeholder = '--'): string {
  const timeMs = getTimeMs(value);
  if (timeMs == null) {
    return placeholder;
  }

  return VIETNAM_DATE_FORMATTER.format(new Date(timeMs));
}

export function formatDateTime(value: string | null | undefined, placeholder = '--'): string {
  const timeMs = getTimeMs(value);
  if (timeMs == null) {
    return placeholder;
  }

  const date = new Date(timeMs);
  return `${VIETNAM_DATE_FORMATTER.format(date)} ${VIETNAM_HOUR_MINUTE_FORMATTER.format(date)}`;
}
