import { Component } from '@angular/core'
import { RouterOutlet } from '@angular/router'
import { ConfirmDialogModule } from 'primeng/confirmdialog'
import { AppToastComponent } from './shared/components/app-toast/app-toast.component'

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, AppToastComponent, ConfirmDialogModule],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {}
