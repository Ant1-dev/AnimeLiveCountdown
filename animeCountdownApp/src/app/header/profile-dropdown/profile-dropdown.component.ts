import { Component, inject, input, output, ChangeDetectionStrategy } from '@angular/core';
import { AuthService } from '../../auth/auth.service';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-profile-dropdown',
  imports: [RouterLink],
  templateUrl: './profile-dropdown.component.html',
  styleUrl: './profile-dropdown.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileDropdownComponent {
  isVisible = input<boolean>(false);
  close = output<void>();

  private authService = inject(AuthService);
  private router = inject(Router);
  protected user = this.authService.user;

  navigateToFollowing(): void {
    this.router.navigate(['/following']);
    this.close.emit();
  }

  navigateToNotifications(): void {
    this.router.navigate(['/notifications']);
    this.close.emit();
  }

  logout(): void {
    this.authService.logout();
  }

}
