import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './header/header.component';
import { FooterComponent } from './footer/footer.component';
import { Router } from '@angular/router';
import { TokenStorageService } from './services/token-storage.service';

@Component({
  selector: 'app-root',
  imports: [HeaderComponent, FooterComponent, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent implements OnInit {
  private router = inject(Router);
  private tokenStorage = inject(TokenStorageService);

  ngOnInit(): void {
    const url = new URL(window.location.href);
    const token = url.searchParams.get('token');

    if (token) {
      this.tokenStorage.saveToken(token);
      this.router.navigate([], { replaceUrl: true });
    }
  }
}
