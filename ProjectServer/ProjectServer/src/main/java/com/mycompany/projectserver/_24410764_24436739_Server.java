/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.tcpechoserver;
import java.io.*;
import java.net.*;


public class _24410764_24436739_Server {
  private static ServerSocket servSock;
  private static final int PORT = 5555;
  private static int clientConnections = 0;

  public static void main(String[] args) {
    System.out.println("Opening port...\n");
    try 
    {
        servSock = new ServerSocket(PORT);
    }
    catch(IOException e) 
    {
         System.out.println("Unable to attach to port!");
         System.exit(1);
    }
    
    do 
    {
         run();
    }while (true);

  }
  
  private static void run()
  {
    Socket link = null;
    try 
    {
      link = servSock.accept();
      clientConnections++;
      BufferedReader in = new BufferedReader( new InputStreamReader(link.getInputStream())); //Step 3.
      PrintWriter out = new PrintWriter(link.getOutputStream(),true);
      
      String message = in.readLine();
      System.out.println("Message received from client: " + clientConnections + "  "+ message);
      out.println("Echo Message: " + message);
     }
    catch(IOException e)
    {
        e.printStackTrace();
    }
    finally 
    {
       try {
	    System.out.println("\n* Closing connection... *");
            link.close();
	}
       catch(IOException e)
       {
            System.out.println("Unable to disconnect!");
	    System.exit(1);
       }
    }
  }
}
