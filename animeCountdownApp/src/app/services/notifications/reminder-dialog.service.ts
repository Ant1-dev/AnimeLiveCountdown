import { inject, Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { AuthService } from '../../auth/auth.service';
import { SignInComponent } from '../../header/sign-in/sign-in.component';
import { ReminderDialogComponent, ReminderDialogData } from '../../shared-components/reminder-dialog/reminder-dialog.component';

/** Opens the reminder dialog for a show, or the sign-in dialog for signed-out visitors. */
@Injectable({ providedIn: 'root' })
export class ReminderDialogService {
  private dialog = inject(MatDialog);
  private auth = inject(AuthService);

  open(data: ReminderDialogData): void {
    if (!this.auth.user()) {
      this.dialog.open(SignInComponent, { panelClass: 'sign-in-panel' });
      return;
    }
    this.dialog.open(ReminderDialogComponent, { data, width: 'min(440px, calc(100vw - 32px))', autoFocus: 'dialog' });
  }
}
