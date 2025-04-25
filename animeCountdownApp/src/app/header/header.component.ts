import { Component, HostListener, inject, signal } from '@angular/core';
import { SearchbarComponent } from './searchbar/searchbar.component';
import { RouterLink } from '@angular/router';
import { SelectButtonModule } from 'primeng/selectbutton';
import { ReactiveFormsModule } from '@angular/forms';
import { MatIcon } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { SignInComponent } from './sign-in/sign-in.component';
import { AuthService } from '../auth/auth.service';
import { ProfileDropdownComponent } from "./profile-dropdown/profile-dropdown.component";

@Component({
  selector: 'app-header',
  imports: [
    SearchbarComponent,
    RouterLink,
    SelectButtonModule,
    ReactiveFormsModule,
    MatIcon,
    ProfileDropdownComponent
],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css',
})
export class HeaderComponent {
  dropdownVisible = signal<boolean>(false);
  private dialog = inject(MatDialog);
  protected user = inject(AuthService).user;

  openSignIn(): void {
    const dialogRef = this.dialog.open(SignInComponent, {
      panelClass: 'sign-in-panel',
    });
    console.log('dialog was opened');

    dialogRef.afterClosed().subscribe(() => {
      console.log('Action has been finished');
    });
  }

  @HostListener('document:click', ['$event'])
  closeDropdownOnOutsideClick(event: MouseEvent): void {
    if (!this.dropdownVisible()) return;
    
    const dropdown = document.querySelector('.profile-dropdown');
    const profileButton = document.querySelector('.profile button');
    
    if (dropdown && profileButton && 
        !dropdown.contains(event.target as Node) && 
        !profileButton.contains(event.target as Node)) {
      this.dropdownVisible.set(false);
    }
  }

  setVisible(): void {
    this.dropdownVisible.set(!this.dropdownVisible());
  }
}
