import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterOutlet } from '@angular/router';
import { HeaderComponent } from "./header/header.component";
import { FooterComponent } from "./footer/footer.component";
import { Router } from '@angular/router'


@Component({
    selector: 'app-root',
    imports: [HeaderComponent, FooterComponent, RouterOutlet],
    templateUrl: './app.component.html',
    styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
    private route = inject(ActivatedRoute);
    private router = inject(Router)

    ngOnInit(): void {
        this.route.queryParams.subscribe(params => {
            const token = params['token'];

            if (token) {
                localStorage.setItem('token', token);
                this.router.navigate([], {
                    queryParams: {},
                    replaceUrl: true,
                });
            }
        });
    }
    
}
