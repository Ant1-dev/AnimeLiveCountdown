import { Component, inject, input, signal } from '@angular/core';
import { AuthService } from '../../auth/auth.service';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-profile-dropdown',
  imports: [RouterLink],
  templateUrl: './profile-dropdown.component.html',
  styleUrl: './profile-dropdown.component.css'
})
export class ProfileDropdownComponent {
  isVisible = input<boolean>(false);
  private authService = inject(AuthService);
  protected user = this.authService.user;

  logout(): void {
    this.authService.logout();
  }

}
