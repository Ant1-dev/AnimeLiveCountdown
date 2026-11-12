import { Media } from './schedule.model';

export type ReactionType = 'FIRE' | 'LOVE' | 'LOL' | 'SHOCK' | 'SAD' | 'MEH';

export interface ReactionSummary {
  mediaId: number;
  /** null when no episode has aired yet */
  episode: number | null;
  counts: Record<ReactionType, number>;
  total: number;
  mine: ReactionType | null;
  emojis: Record<ReactionType, string>;
}

export interface HypeEntry {
  media: Media;
  score: number;
  recentReactions: number;
  newFollowers: number;
  followers: number;
}
