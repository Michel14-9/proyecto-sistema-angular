import {
  Component,
  ElementRef,
  ViewChild,
  AfterViewChecked,
  OnInit
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ChatbotService } from '../../../core/services/chatbot.service';

interface Message{
  text:string;
  isUser:boolean;
  timestamp:Date;
}

@Component({
  selector:'app-chatbot',
  standalone:true,
  imports:[
    CommonModule,
    FormsModule
  ],
  templateUrl:'./chatbot.component.html',
  styleUrls:['./chatbot.component.css']
})

export class ChatbotComponent implements OnInit, AfterViewChecked{

  @ViewChild('chatContainer')
  private chatContainer!:ElementRef;

  isOpen=false;
  isLoading=false;
  isMinimized=false;
  newMessage='';
  messages:Message[]=[];

  // Fecha actual
  todayDate: string = '';

  constructor(
    private chatbotService:ChatbotService
  ){}

  ngOnInit():void{
    // Fecha actual formateada
    this.todayDate = new Date().toLocaleDateString('es-PE', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      timeZone: 'America/Lima'
    });

    this.addBotMessage(
      '🍗 ¡Bienvenido a Chicken Luren! Soy Chicken Luren Bot. Estoy listo para ayudarte con nuestro menú, promociones y pedidos.'
    );
  }

  ngAfterViewChecked():void{
    this.scrollToBottom();
  }

  toggleChat(){
    this.isOpen=!this.isOpen;
    if(this.isOpen){
      this.isMinimized=false;
    }
  }

  minimizeChat(){
    this.isMinimized=!this.isMinimized;
  }

  sendMessage(){
    const message=this.newMessage.trim();
    if(!message) return;

    this.addUserMessage(message);
    this.newMessage='';
    this.isLoading=true;

    this.chatbotService.sendMessage(message).subscribe({
      next:(response)=>{
        this.addBotMessage(response.response);
        this.isLoading=false;
      },
      error:(error)=>{
        console.error(error);
        this.addBotMessage(
          'Lo sentimos. Ocurrió un problema al comunicarnos con la cocina. Inténtalo nuevamente.'
        );
        this.isLoading=false;
      }
    });
  }

  private addUserMessage(text:string){
    this.messages.push({
      text,
      isUser:true,
      timestamp:new Date()
    });
  }

  private addBotMessage(text:string){
    this.messages.push({
      text,
      isUser:false,
      timestamp:new Date()
    });
  }

  private scrollToBottom(){
    try{
      if(this.chatContainer){
        this.chatContainer.nativeElement.scrollTop=
          this.chatContainer.nativeElement.scrollHeight;
      }
    }catch(error){
      console.error(error);
    }
  }

  formatTime(date:Date):string{
    return date.toLocaleTimeString('es-PE',{
      hour:'2-digit',
      minute:'2-digit'
    });
  }
}
