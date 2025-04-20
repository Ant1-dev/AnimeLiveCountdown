import { Component } from '@angular/core';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-sign-in',
  imports: [],
  templateUrl: './sign-in.component.html',
  styleUrl: './sign-in.component.css'
})
export class SignInComponent {
  googleSignIn() {
    window.location.href = environment.apiUrl + "login/oauth2/authorization/google";
  }
  
  discordSignIn() {
    window.location.href = environment.apiUrl + "login/oauth2/authorization/discord";
  }
  
  appleSignIn() {
    window.location.href = environment.apiUrl + "login/oauth2/authorization/github";
  }
}