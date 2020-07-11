package com.clibank.clibank.service;

import org.springframework.stereotype.Service;

import java.io.PrintStream;

@Service
public class ConsoleService {

    private final PrintStream out = System.out;
    private final static  String ANSI_RESET = "\u001B[0m";
    private final static  String ANSI_YELLOW = "\u001B[33m";
    public void write(String msg , String ... args){
        this.out.print(">");
        this.out.print(ANSI_YELLOW);
        this.out.printf(msg , (Object[]) args);
        this.out.print(ANSI_RESET);
        this.out.println();
    }
}
