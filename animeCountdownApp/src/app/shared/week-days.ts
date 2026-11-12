/** Weekday URL segments for /schedule/:day, Monday first. */
export const WEEK_DAYS = ['monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday'] as const;
export type WeekDay = (typeof WEEK_DAYS)[number];

export function dayLabel(day: string): string {
  return day.charAt(0).toUpperCase() + day.slice(1);
}
