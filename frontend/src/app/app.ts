import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from "./shared/components/navbar/navbar.component";
import { FooterComponent } from "./shared/components/footer/footer.component";
import { WebSocketService } from './core/services/websocket.service';
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NavbarComponent, FooterComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly title = signal('frontend');
  private readonly webSocketService = inject(WebSocketService);

  ngOnInit(): void {
    // Initiate the WebSocket connection as soon as the app loads.
    this.webSocketService.connect();
  }
}
