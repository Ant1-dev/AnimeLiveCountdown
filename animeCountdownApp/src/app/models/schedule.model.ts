export interface Media {
    id: number;
    title_Romaji: string;
    title_English: string;
    status: string;
    next_Airing_At: Date;
    next_Airing_Episode: number;
    cover_Image_Url: string;
    day: string;
    banner_Image_Url?: string;
    season: string;
}