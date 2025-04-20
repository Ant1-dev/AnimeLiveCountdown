import { User } from "./user.model";

export interface Notification {
    id: number;
    notifDate: Date;
    message: string;
    user: User;
}