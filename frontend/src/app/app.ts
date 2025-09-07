import { Component, effect, inject, OnInit, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from "./shared/components/navbar/navbar.component";
import { FooterComponent } from "./shared/components/footer/footer.component";
import { WebSocketService } from './core/services/websocket.service';
import { AuthService } from './core/services/auth.service'; // <-- Import AuthService
import { CommonModule } from '@angular/common';
import { ChatWidgetComponent } from './shared/components/chat-widget.component/chat-widget.component';

@Component({
  selector: 'app-root',
  standalone: true, // <-- Make sure component is standalone
  imports: [CommonModule, RouterOutlet, NavbarComponent, FooterComponent, ChatWidgetComponent], // <-- Add CommonModule
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly title = signal('frontend');
  private readonly webSocketService = inject(WebSocketService);
  public readonly authService = inject(AuthService); // <-- Inject AuthService and make it public

  constructor() {
    effect(() => {
      if (this.authService.isUserLoggedIn()) {
        this.webSocketService.connect();
      } else {
        this.webSocketService.disconnect(); // Assumes you have a disconnect method
      }
    });
  }
}