package com.orangomango.blockworld.console;

import java.io.*;
import java.util.function.Consumer;

public class Console{
	private Consumer<String> consumer;
	private String lastCommand;

	public Console(Consumer<String> c){
		this.consumer = c;
	}

	public void start(){
		Thread main = new Thread(() -> {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
				System.out.print("> ");
				while (true){
					String command = reader.readLine();
					this.lastCommand = command;
				}
			} catch (IOException ex){
				ex.printStackTrace();
			}
		});
		main.setDaemon(true);
		main.start();
	}

	public void runLastCommand(){
		if (this.lastCommand != null){
			this.consumer.accept(this.lastCommand);
			System.out.print("> ");
			this.lastCommand = null;
		}
	}
}