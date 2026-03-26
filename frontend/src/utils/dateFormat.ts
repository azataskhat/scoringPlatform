/**
 * Jackson may serialize LocalDateTime as an array [year, month, day, hour, minute, second, nano].
 * These helpers handle both array and ISO-string formats.
 */

function toDate(value: unknown): Date | null {
  if (Array.isArray(value)) {
    const [y, m, d, h = 0, min = 0, s = 0] = value;
    return new Date(y, m - 1, d, h, min, s);
  }
  if (!value) return null;
  const date = new Date(value as string);
  return isNaN(date.getTime()) ? null : date;
}

export function formatDate(value: unknown): string {
  const d = toDate(value);
  return d ? d.toLocaleDateString("ru-RU") : "—";
}

export function formatDateTime(value: unknown): string {
  const d = toDate(value);
  return d ? d.toLocaleString("ru-RU") : "";
}

export function toTimestamp(value: unknown): number {
  const d = toDate(value);
  return d ? d.getTime() : 0;
}
