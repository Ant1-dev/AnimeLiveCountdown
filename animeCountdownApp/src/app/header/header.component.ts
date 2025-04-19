import { Component, inject } from '@angular/core';
import { SearchbarComponent } from './searchbar/searchbar.component';
import { RouterLink } from '@angular/router';
import { SelectButtonModule } from 'primeng/selectbutton';
import { ReactiveFormsModule } from '@angular/forms';
import { MatIcon } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { SignInComponent } from './sign-in/sign-in.component';

@Component({
  selector: 'app-header',
  imports: [
    SearchbarComponent,
    RouterLink,
    SelectButtonModule,
    ReactiveFormsModule,
    MatIcon,
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css',
})
export class HeaderComponent {
  dialog = inject(MatDialog);

  openSignIn(): void {
    const dialogRef = this.dialog.open(SignInComponent, {
      panelClass: 'sign-in-panel',
    });
    console.log('dialog was opened');

    dialogRef.afterClosed().subscribe(() => {
      console.log('Action has been finished');
    });
  }
}
